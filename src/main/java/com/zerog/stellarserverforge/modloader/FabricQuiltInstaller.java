package com.zerog.stellarserverforge.modloader;

import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.net.ChecksumUtil;
import com.zerog.stellarserverforge.net.HttpFetcher;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

/**
 * Detects and installs Fabric/Quilt server files via the loader-agnostic installer jar
 * (spec §6.5).
 */
public class FabricQuiltInstaller {

    private final HttpFetcher http = new HttpFetcher();
    private final Path cacheDir;
    private final Path serverDir;

    public FabricQuiltInstaller(Path cacheDir, Path serverDir) {
        this.cacheDir = cacheDir;
        this.serverDir = serverDir;
    }

    public String serverJarFileName(ModLoader loader, McVersion mc, String version) {
        String prefix = loader == ModLoader.QUILT ? "quilt" : "fabric";
        return prefix + "-server-launch-" + mc.raw() + "-" + version + ".jar";
    }

    public boolean isInstalled(ModLoader loader, McVersion mc, String version) {
        Path jar = serverDir.resolve(serverJarFileName(loader, mc, version));
        Path librariesMarker = loader == ModLoader.QUILT
                ? serverDir.resolve("libraries/org/quiltmc")
                : serverDir.resolve("libraries/net/fabricmc");
        return Files.isRegularFile(jar) && Files.isDirectory(librariesMarker);
    }

    public void install(ModLoader loader, McVersion mc, String version, String javaCommand, Consumer<String> log)
            throws IOException, InterruptedException {
        if (loader == ModLoader.QUILT && serverDir.toString().contains(" ")) {
            throw new IOException("Quilt's installer cannot run from a folder path containing spaces: \""
                    + serverDir + "\" — move the server to a path with no spaces and try again.");
        }

        cleanExisting();

        String installerVersion = fetchInstallerVersion(loader);
        String installerFileName = (loader == ModLoader.QUILT ? "quilt-installer-" : "fabric-installer-")
                + installerVersion + ".jar";
        String installerUrl = installerBaseUrl(loader) + installerVersion + "/" + installerFileName;

        Path cachedInstaller = cacheDir.resolve("installers").resolve(installerFileName);
        Files.createDirectories(cachedInstaller.getParent());
        if (!Files.exists(cachedInstaller)) {
            log.accept("Downloading " + loader + " installer " + installerVersion + "...");
            downloadBestEffortVerified(installerUrl, cachedInstaller);
        }

        Path workingInstaller = serverDir.resolve(installerFileName);
        Files.copy(cachedInstaller, workingInstaller, StandardCopyOption.REPLACE_EXISTING);

        log.accept("Running the " + loader + " installer...");
        Process installerProcess = buildInstallerProcess(loader, mc, version, javaCommand, installerFileName).start();
        try (var reader = installerProcess.inputReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.accept(line);
            }
        }
        int exit = installerProcess.waitFor();
        Files.deleteIfExists(workingInstaller);

        if (exit != 0) {
            throw new IOException(loader + " installer exited with code " + exit + ".");
        }

        renameServerLaunchJar(loader, mc, version);

        if (!isInstalled(loader, mc, version)) {
            throw new IOException(loader + " " + version + " installation did not produce the expected server jar.");
        }
    }

    private ProcessBuilder buildInstallerProcess(ModLoader loader, McVersion mc, String version, String javaCommand,
                                                  String installerFileName) {
        java.util.List<String> command;
        if (loader == ModLoader.QUILT) {
            command = java.util.List.of(javaCommand, "-XX:+UseG1GC", "-jar", installerFileName,
                    "install", "server", mc.raw(), version, "--download-server",
                    "--install-dir=" + serverDir.toAbsolutePath());
        } else {
            command = java.util.List.of(javaCommand, "-XX:+UseG1GC", "-jar", installerFileName,
                    "server", "-loader", version, "-mcversion", mc.raw(), "-downloadMinecraft");
        }
        return new ProcessBuilder(command).directory(serverDir.toFile()).redirectErrorStream(true);
    }

    private void renameServerLaunchJar(ModLoader loader, McVersion mc, String version) throws IOException {
        String prefix = loader == ModLoader.QUILT ? "quilt" : "fabric";
        Path generated = serverDir.resolve(prefix + "-server-launch.jar");
        Path target = serverDir.resolve(serverJarFileName(loader, mc, version));
        if (Files.exists(generated)) {
            Files.move(generated, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String fetchInstallerVersion(ModLoader loader) throws IOException, InterruptedException {
        String metadataUrl = loader == ModLoader.QUILT
                ? "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/maven-metadata.xml"
                : "https://maven.fabricmc.net/net/fabricmc/fabric-installer/maven-metadata.xml";
        Path tmp = Files.createTempFile("installer-metadata", ".xml");
        try {
            Files.writeString(tmp, http.getString(metadataUrl));
            String release = MavenMetadata.parse(tmp).release();
            if (release == null) {
                throw new IOException("Could not determine the latest " + loader + " installer version.");
            }
            return release;
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private String installerBaseUrl(ModLoader loader) {
        return loader == ModLoader.QUILT
                ? "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/"
                : "https://maven.fabricmc.net/net/fabricmc/fabric-installer/";
    }

    private void downloadBestEffortVerified(String url, Path destination) throws IOException, InterruptedException {
        http.downloadToFile(url, destination);
        String sha1;
        try {
            sha1 = http.getString(url + ".sha1").trim();
        } catch (com.zerog.stellarserverforge.net.HttpStatusException e) {
            if (e.isNotFound()) {
                // No .sha1 sidecar published — proceed without verification.
                return;
            }
            Files.deleteIfExists(destination);
            throw e;
        } catch (IOException e) {
            // Any other network failure (timeout, connection reset, etc.) fetching the checksum —
            // don't silently run an unverified installer jar over a transient condition.
            Files.deleteIfExists(destination);
            throw e;
        }
        if (!ChecksumUtil.matches(destination, sha1, "SHA-1")) {
            Files.deleteIfExists(destination);
            throw new IOException("Installer checksum verification failed for " + url);
        }
    }

    private void cleanExisting() throws IOException {
        deleteRecursively(serverDir.resolve(".fabric"));
        deleteRecursively(serverDir.resolve("libraries"));
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(serverDir, "*.jar")) {
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
