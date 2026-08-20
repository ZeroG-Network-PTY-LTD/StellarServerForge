package com.zerog.stellarserverforge.javamanaged;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerog.stellarserverforge.net.ChecksumUtil;
import com.zerog.stellarserverforge.net.HttpFetcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads and manages Adoptium (Eclipse Temurin) JDK/JRE builds under {@code java/<folder>/}
 * in the app's cache directory (spec §7.2, Adoptium-managed Java fallback / mode F).
 */
public class AdoptiumProvisioner {

    private static final Duration STALENESS_CUTOFF = Duration.ofDays(30 * 6);

    private final HttpFetcher http = new HttpFetcher();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path javaCacheDir;

    public AdoptiumProvisioner(Path cacheDir) {
        this.javaCacheDir = cacheDir.resolve("java");
    }

    /** Ensures a managed Java of the given major version is present and reasonably current, returning its java executable. */
    public Path ensureProvisioned(int majorVersion) throws IOException, InterruptedException {
        Files.createDirectories(javaCacheDir);
        Optional<Path> existing = findExistingFolder(majorVersion);

        if (existing.isPresent()) {
            Path javaExe = javaExecutable(existing.get());
            if (Files.isRegularFile(javaExe)) {
                Instant lastWrite = Files.getLastModifiedTime(javaExe).toInstant();
                boolean stale = Instant.now().isAfter(lastWrite.plus(STALENESS_CUTOFF));
                if (!stale) {
                    return javaExe;
                }
                AdoptiumRelease release = fetchLatestRelease(majorVersion);
                if (release != null && existing.get().getFileName().toString().equals(release.releaseName())) {
                    // Folder name matches the current release name; still functionally current.
                    return javaExe;
                }
                deleteRecursively(existing.get());
            }
        }

        return download(majorVersion);
    }

    private Path download(int majorVersion) throws IOException, InterruptedException {
        AdoptiumRelease release = fetchLatestRelease(majorVersion);
        if (release == null) {
            throw new IOException("No Adoptium release found for Java " + majorVersion);
        }

        Path zipPath = javaCacheDir.resolve(release.releaseName() + ".zip");
        http.downloadToFile(release.downloadUrl(), zipPath);

        if (release.sha256() != null && !ChecksumUtil.matches(zipPath, release.sha256(), "SHA-256")) {
            Files.deleteIfExists(zipPath);
            throw new IOException("Adoptium download for Java " + majorVersion + " failed checksum verification");
        }

        Path extractedRoot = extractZip(zipPath, javaCacheDir);
        Files.deleteIfExists(zipPath);

        Path javaExe = javaExecutable(extractedRoot);
        if (!Files.isRegularFile(javaExe)) {
            throw new IOException("Extracted Adoptium build did not contain a java executable: " + extractedRoot);
        }
        return javaExe;
    }

    private record AdoptiumRelease(String releaseName, String downloadUrl, String sha256) {
    }

    private AdoptiumRelease fetchLatestRelease(int majorVersion) throws IOException, InterruptedException {
        String imageType = majorVersion == 16 ? "jdk" : "jre";
        String os = isWindows() ? "windows" : (isMac() ? "mac" : "linux");
        String url = "https://api.adoptium.net/v3/assets/feature_releases/" + majorVersion
                + "/ga?architecture=x64&image_type=" + imageType + "&os=" + os
                + "&vendor=eclipse&page_size=1&sort_method=DATE&sort_order=DESC";

        String body;
        try {
            body = http.getString(url);
        } catch (IOException e) {
            return null;
        }
        JsonNode array = mapper.readTree(body);
        if (!array.isArray() || array.isEmpty()) {
            return null;
        }
        JsonNode first = array.get(0);
        String releaseName = first.path("release_name").asText();
        JsonNode binaries = first.path("binaries");
        if (!binaries.isArray() || binaries.isEmpty()) {
            return null;
        }
        JsonNode pkg = binaries.get(0).path("package");
        String link = pkg.path("link").asText();
        String checksum = pkg.has("checksum") ? pkg.path("checksum").asText() : null;
        return new AdoptiumRelease(releaseName, link, checksum);
    }

    private Optional<Path> findExistingFolder(int majorVersion) throws IOException {
        String prefix = majorVersion == 8 ? "jdk8u" : "jdk-" + majorVersion;
        if (!Files.isDirectory(javaCacheDir)) {
            return Optional.empty();
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(javaCacheDir)) {
            for (Path dir : stream) {
                if (Files.isDirectory(dir) && dir.getFileName().toString().startsWith(prefix)) {
                    return Optional.of(dir);
                }
            }
        }
        return Optional.empty();
    }

    private Path javaExecutable(Path jdkRoot) {
        String exeName = isWindows() ? "java.exe" : "java";
        return jdkRoot.resolve("bin").resolve(exeName);
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }

    private Path extractZip(Path zipPath, Path destDir) throws IOException {
        Path topLevelDir = null;
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path target = destDir.resolve(entry.getName()).normalize();
                if (!target.startsWith(destDir)) {
                    throw new IOException("Zip entry escapes destination directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                }
                if (topLevelDir == null) {
                    Path relative = destDir.relativize(target);
                    if (relative.getNameCount() > 0) {
                        topLevelDir = destDir.resolve(relative.getName(0));
                    }
                }
            }
        }
        if (topLevelDir == null || !Files.isDirectory(topLevelDir)) {
            throw new IOException("Could not determine extracted JDK root from " + zipPath);
        }
        return topLevelDir;
    }

    private void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                            // Best-effort cleanup.
                        }
                    });
        }
    }
}
