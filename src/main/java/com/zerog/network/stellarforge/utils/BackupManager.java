package com.zerog.network.stellarforge.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Manages automatic and manual backups of server data.
 * Backups are stored as ZIP archives under: backups/<profileName>/
 */
public class BackupManager {
    private static final Logger logger = LoggerFactory.getLogger(BackupManager.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final Path BACKUPS_ROOT = Paths.get("backups");

    public enum BackupType { WORLD_ONLY, FULL }

    /** Lightweight progress callback */
    public interface BackupProgressCallback {
        void onProgress(int percent, String message);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static Path createBackup(Path serverPath, String profileName,
                                    BackupType type,
                                    BackupProgressCallback callback) {
        try {
            Path profileBackupsDir = BACKUPS_ROOT.resolve(sanitise(profileName));
            Files.createDirectories(profileBackupsDir);

            String timestamp = LocalDateTime.now().format(TS);
            String label     = type == BackupType.WORLD_ONLY ? "world" : "full";
            String zipName   = label + "_" + timestamp + ".zip";
            Path   zipPath   = profileBackupsDir.resolve(zipName);

            if (callback != null) callback.onProgress(0, "Starting " + label + " backup…");

            if (type == BackupType.WORLD_ONLY) {
                Path worldDir = serverPath.resolve("world");
                if (!Files.exists(worldDir)) {
                    if (callback != null) callback.onProgress(0, "World folder not found – nothing to backup.");
                    return null;
                }
                zipDirectory(worldDir, zipPath, callback);
            } else {
                zipDirectory(serverPath, zipPath, callback);
            }

            if (callback != null) callback.onProgress(100, "Backup complete: " + zipName);
            logger.info("Backup created: {}", zipPath);

            applyRetentionPolicy(profileBackupsDir, 10);
            return zipPath;

        } catch (IOException e) {
            logger.error("Backup failed", e);
            if (callback != null) callback.onProgress(-1, "Backup failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * List all backups for a profile, newest first.
     */
    public static List<BackupEntry> listBackups(String profileName) {
        List<BackupEntry> entries = new ArrayList<>();
        Path dir = BACKUPS_ROOT.resolve(sanitise(profileName));
        if (!Files.exists(dir)) return entries;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.zip")) {
            for (Path p : stream) {
                BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
                entries.add(new BackupEntry(p, attr.size(), attr.creationTime().toInstant().toString()));
            }
        } catch (IOException e) {
            logger.error("Error listing backups", e);
        }

        entries.sort(Comparator.comparing(b -> b.path.getFileName().toString(), Comparator.reverseOrder()));
        return entries;
    }

    /**
     * Restore a backup ZIP to the target directory, overwriting existing files.
     */
    public static boolean restoreBackup(Path zipPath, Path targetPath,
                                         BackupProgressCallback callback) {
        try {
            if (callback != null) callback.onProgress(0, "Restoring from " + zipPath.getFileName());
            Files.createDirectories(targetPath);

            try (java.util.zip.ZipInputStream zis =
                         new java.util.zip.ZipInputStream(new FileInputStream(zipPath.toFile()))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path dest = targetPath.resolve(entry.getName()).normalize();
                    if (!dest.startsWith(targetPath)) continue; // zip-slip guard
                    if (entry.isDirectory()) {
                        Files.createDirectories(dest);
                    } else {
                        Files.createDirectories(dest.getParent());
                        Files.copy(zis, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }

            if (callback != null) callback.onProgress(100, "Restore complete!");
            logger.info("Restored backup {} → {}", zipPath, targetPath);
            return true;

        } catch (IOException e) {
            logger.error("Restore failed", e);
            if (callback != null) callback.onProgress(-1, "Restore failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete a backup file.
     */
    public static boolean deleteBackup(Path zipPath) {
        try {
            Files.deleteIfExists(zipPath);
            logger.info("Deleted backup: {}", zipPath);
            return true;
        } catch (IOException e) {
            logger.error("Error deleting backup", e);
            return false;
        }
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private static void zipDirectory(Path sourceDir, Path zipFile,
                                     BackupProgressCallback callback) throws IOException {
        // Count total files first for progress
        long[] total = {0};
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path f, BasicFileAttributes a) {
                total[0]++;
                return FileVisitResult.CONTINUE;
            }
        });

        long[] done = {0};
        Path parent = sourceDir.getParent();

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(
                new FileOutputStream(zipFile.toFile())))) {

            Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String entryName = parent == null
                            ? file.toString()
                            : parent.relativize(file).toString().replace('\\', '/');

                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zos);
                    zos.closeEntry();

                    done[0]++;
                    if (callback != null && total[0] > 0) {
                        int pct = (int) ((done[0] * 100) / total[0]);
                        callback.onProgress(pct, "Compressing " + file.getFileName());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) {
                    logger.warn("Skipping unreadable file: {}", file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /** Keep only the N most-recent backups, delete older ones. */
    private static void applyRetentionPolicy(Path dir, int maxKeep) {
        List<BackupEntry> all = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.zip")) {
            for (Path p : stream) {
                BasicFileAttributes a = Files.readAttributes(p, BasicFileAttributes.class);
                all.add(new BackupEntry(p, a.size(), a.creationTime().toInstant().toString()));
            }
        } catch (IOException e) {
            return;
        }

        if (all.size() <= maxKeep) return;
        all.sort(Comparator.comparing(b -> b.path.getFileName().toString()));
        int toDelete = all.size() - maxKeep;
        for (int i = 0; i < toDelete; i++) {
            deleteBackup(all.get(i).path);
            logger.info("Retention policy: removed old backup {}", all.get(i).path.getFileName());
        }
    }

    private static String sanitise(String name) {
        return name.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    // ── Value object ──────────────────────────────────────────────────────────

    public static class BackupEntry {
        public final Path   path;
        public final long   sizeBytes;
        public final String created;

        public BackupEntry(Path path, long sizeBytes, String created) {
            this.path      = path;
            this.sizeBytes = sizeBytes;
            this.created   = created;
        }

        public String getDisplayName() { return path.getFileName().toString(); }

        public String getFormattedSize() {
            if (sizeBytes < 1024)             return sizeBytes + " B";
            if (sizeBytes < 1024 * 1024)      return String.format("%.1f KB", sizeBytes / 1024.0);
            if (sizeBytes < 1024 * 1024 * 1024) return String.format("%.1f MB", sizeBytes / (1024.0 * 1024));
            return String.format("%.1f GB", sizeBytes / (1024.0 * 1024 * 1024));
        }
    }
}


