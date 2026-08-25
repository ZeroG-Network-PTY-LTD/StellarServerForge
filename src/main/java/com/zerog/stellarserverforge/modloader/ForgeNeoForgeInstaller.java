package com.zerog.stellarserverforge.modloader;

import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.mojang.MojangManifestService;
import com.zerog.stellarserverforge.mojang.VanillaInstallService;
import com.zerog.stellarserverforge.net.ChecksumUtil;
import com.zerog.stellarserverforge.net.HttpFetcher;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Detects and installs Forge/NeoForge server files (spec §6.4). Installer jars are cached under
 * {@code installers/} for reuse across launches.
 */
public class ForgeNeoForgeInstaller {

    private final HttpFetcher http = new HttpFetcher();
    private final Path cacheDir;
    private final Path serverDir;

    public ForgeNeoForgeInstaller(Path cacheDir, Path serverDir) {
        this.cacheDir = cacheDir;
        this.serverDir = serverDir;
    }

    public boolean isInstalled(ModLoader loader, McVersion mc, String version) {
        if (loader == ModLoader.NEOFORGE) {
            Path dir = "1.20.1".equals(mc.raw())
                    ? serverDir.resolve("libraries/net/neoforged/forge/" + mc.raw() + "-" + version)
                    : serverDir.resolve("libraries/net/neoforged/neoforge/" + version);
            return Files.isDirectory(dir);
        }

        // FORGE
        if (mc.major() >= 17) {
            return Files.isDirectory(serverDir.resolve("libraries/net/minecraftforge/forge/" + mc.raw() + "-" + version));
        }
        if (mc.major() >= 13) {
            Path jar = serverDir.resolve("forge-" + mc.raw() + "-" + version + ".jar");
            Path libDir = serverDir.resolve("libraries/net/minecraftforge/forge/" + mc.raw() + "-" + version);
            return Files.isRegularFile(jar) && Files.isDirectory(libDir);
        }
        // MC <= 12: wildcard match any *<mc>-<ver>*.jar in root.
        return matchesWildcardJar(mc.raw(), version);
    }

    private boolean matchesWildcardJar(String mc, String version) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(serverDir, "*.jar")) {
            for (Path p : stream) {
                String name = p.getFileName().toString();
                if (name.contains(mc) && name.contains(version)) {
                    return true;
                }
            }
        } catch (IOException ignored) {
            // No jars present.
        }
        return false;
    }

    public void install(ModLoader loader, McVersion mc, String version, String javaCommand,
                         MojangManifestService mojangManifestService, VanillaInstallService vanillaInstallService,
                         Consumer<String> log) throws IOException, InterruptedException {
        cleanExisting();

        if (mc.major() <= 16) {
            log.accept("Ensuring vanilla server jar is present first (older Forge installers can reference it)...");
            MojangManifestService.VersionEntry entry = mojangManifestService.findVersion(mc.raw());
            if (entry != null) {
                vanillaInstallService.ensureInstalled(entry);
            }
        }

        String installerUrl = installerUrl(loader, mc, version);
        String installerFileName = installerFileName(loader, mc, version);

        Path cachedInstaller = cacheDir.resolve("installers").resolve(installerFileName);
        Files.createDirectories(cachedInstaller.getParent());

        if (!Files.exists(cachedInstaller)) {
            log.accept("Downloading " + loader + " installer " + version + "...");
            downloadAndVerify(installerUrl, cachedInstaller);
        }

        Path workingInstaller = serverDir.resolve(installerFileName);
        Files.copy(cachedInstaller, workingInstaller, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        log.accept("Running the " + loader + " installer...");
        Process installerProcess = new ProcessBuilder(
                javaCommand, "-Djava.net.preferIPv4Stack=true", "-XX:+UseG1GC",
                "-jar", installerFileName, "--installServer")
                .directory(serverDir.toFile())
                .redirectErrorStream(true)
                .start();
        try (var reader = installerProcess.inputReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.accept(line);
            }
        }
        int exit = installerProcess.waitFor();

        cleanupAfterInstall(workingInstaller);

        if (exit != 0 || !isInstalled(loader, mc, version)) {
            Files.deleteIfExists(cachedInstaller);
            throw new IOException(loader + " " + version + " installation did not complete successfully (exit code " + exit + ").");
        }
    }

    private void downloadAndVerify(String installerUrl, Path destination) throws IOException, InterruptedException {
        http.downloadToFile(installerUrl, destination);
        String sha256;
        try {
            sha256 = http.getString(installerUrl + ".sha256").trim();
        } catch (com.zerog.stellarserverforge.net.HttpStatusException e) {
            if (e.isNotFound()) {
                // No .sha256 sidecar published for this artifact — proceed without verification.
                return;
            }
            // A real (non-404) HTTP failure fetching the checksum — don't silently run an
            // unverified installer jar over a transient server/network condition.
            Files.deleteIfExists(destination);
            throw e;
        } catch (IOException e) {
            // Any other network failure (timeout, connection reset, etc.) — same reasoning.
            Files.deleteIfExists(destination);
            throw e;
        }
        if (!ChecksumUtil.matches(destination, sha256, "SHA-256")) {
            Files.deleteIfExists(destination);
            throw new IOException("Installer checksum verification failed for " + installerUrl);
        }
    }

    private String installerFileName(ModLoader loader, McVersion mc, String version) {
        if (loader == ModLoader.NEOFORGE) {
            return "1.20.1".equals(mc.raw())
                    ? "forge-" + mc.raw() + "-" + version + "-installer.jar"
                    : "neoforge-" + version + "-installer.jar";
        }
        return "forge-" + mc.raw() + "-" + version + "-installer.jar";
    }

    private String installerUrl(ModLoader loader, McVersion mc, String version) {
        String fileName = installerFileName(loader, mc, version);
        if (loader == ModLoader.NEOFORGE) {
            return "1.20.1".equals(mc.raw())
                    ? "https://maven.neoforged.net/releases/net/neoforged/forge/" + mc.raw() + "-" + version + "/" + fileName
                    : "https://maven.neoforged.net/releases/net/neoforged/neoforge/" + version + "/" + fileName;
        }
        return "https://maven.minecraftforge.net/net/minecraftforge/forge/" + mc.raw() + "-" + version + "/" + fileName;
    }

    private void cleanExisting() throws IOException {
        deleteMatching(serverDir, "*.jar");
        deleteRecursively(serverDir.resolve("libraries"));
        deleteRecursively(serverDir.resolve(".fabric"));
    }

    private void cleanupAfterInstall(Path installerJar) throws IOException {
        Files.deleteIfExists(installerJar);
        deleteMatching(serverDir, "*installer.log*");
        deleteMatching(serverDir, "run.sh");
        deleteMatching(serverDir, "run.bat");
        Files.deleteIfExists(serverDir.resolve("user_jvm_args.txt"));
    }

    private void deleteMatching(Path dir, String glob) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, glob)) {
            for (Path p : stream) {
                Files.deleteIfExists(p);
            }
        }
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
