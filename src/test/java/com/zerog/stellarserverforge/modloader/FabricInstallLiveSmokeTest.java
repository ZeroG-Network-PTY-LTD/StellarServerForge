package com.zerog.stellarserverforge.modloader;

import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A real network smoke test of the Fabric install path: fetches live Fabric maven metadata,
 * resolves the newest loader version for a real Minecraft version, downloads and runs the real
 * Fabric installer against the real Fabric/Mojang servers, and verifies the resulting server jar
 * is detected as installed afterward. Excluded from the default {@code test} task; run explicitly
 * with {@code ./gradlew testLive}.
 */
@Tag("live")
class FabricInstallLiveSmokeTest {

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void installsRealFabricServerEndToEnd(@TempDir Path tempDir) throws Exception {
        Path cacheDir = tempDir.resolve("cache");
        Path serverDir = tempDir.resolve("server");
        java.nio.file.Files.createDirectories(serverDir);

        McVersion mc = McVersion.parse("1.20.4");

        ModLoaderMetadataService metadataService = new ModLoaderMetadataService(cacheDir);
        Path metadataFile = metadataService.ensureMetadataFile(ModLoader.FABRIC, mc);

        ModLoaderVersionResolver resolver = new ModLoaderVersionResolver();
        Optional<String> newest = resolver.resolveNewest(ModLoader.FABRIC, mc, metadataFile, null);
        assertTrue(newest.isPresent(), "should resolve a newest Fabric loader version from live metadata");

        FabricQuiltInstaller installer = new FabricQuiltInstaller(cacheDir, serverDir);
        assertTrue(!installer.isInstalled(ModLoader.FABRIC, mc, newest.get()), "should not be pre-installed");

        String javaCommand = System.getProperty("java.home") + java.io.File.separator + "bin"
                + java.io.File.separator + (System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java");

        StringBuilder log = new StringBuilder();
        installer.install(ModLoader.FABRIC, mc, newest.get(), javaCommand, log::append);

        assertTrue(installer.isInstalled(ModLoader.FABRIC, mc, newest.get()),
                "Fabric should be detected as installed after a successful install. Installer log:\n" + log);
    }
}
