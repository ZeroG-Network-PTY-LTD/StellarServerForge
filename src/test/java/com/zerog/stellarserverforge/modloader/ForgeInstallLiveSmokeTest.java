package com.zerog.stellarserverforge.modloader;

import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.mojang.MojangManifestService;
import com.zerog.stellarserverforge.mojang.VanillaInstallService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A real network smoke test of the Forge install path: fetches live promotions_slim.json + maven
 * metadata, resolves the newest Forge version for a real Minecraft version, downloads and runs
 * the real Forge installer (--installServer) against live Forge/Mojang servers, confirms
 * install-detection recognizes the result, and confirms the args-file launch line can actually be
 * built (proving win_args.txt/unix_args.txt was really produced by the installer). Excluded from
 * the default {@code test} task; run explicitly with {@code ./gradlew testLive}.
 */
@Tag("live")
class ForgeInstallLiveSmokeTest {

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void installsRealForgeServerEndToEnd(@TempDir Path tempDir) throws Exception {
        Path cacheDir = tempDir.resolve("cache");
        Path serverDir = tempDir.resolve("server");
        Files.createDirectories(serverDir);

        McVersion mc = McVersion.parse("1.20.1");

        ModLoaderMetadataService metadataService = new ModLoaderMetadataService(cacheDir);
        Path metadataFile = metadataService.ensureMetadataFile(ModLoader.FORGE, mc);
        Path promotionsFile = metadataService.ensurePromotionsFile();

        ModLoaderVersionResolver resolver = new ModLoaderVersionResolver();
        Optional<String> newest = resolver.resolveNewest(ModLoader.FORGE, mc, metadataFile, promotionsFile);
        assertTrue(newest.isPresent(), "should resolve a newest Forge version from live promotions_slim.json");

        String javaCommand = System.getProperty("java.home") + java.io.File.separator + "bin"
                + java.io.File.separator + (System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java");

        MojangManifestService mojangManifestService = new MojangManifestService(cacheDir);
        VanillaInstallService vanillaInstallService = new VanillaInstallService(cacheDir, serverDir);

        ForgeNeoForgeInstaller installer = new ForgeNeoForgeInstaller(cacheDir, serverDir);
        assertTrue(!installer.isInstalled(ModLoader.FORGE, mc, newest.get()), "should not be pre-installed");

        StringBuilder log = new StringBuilder();
        installer.install(ModLoader.FORGE, mc, newest.get(), javaCommand, mojangManifestService, vanillaInstallService, log::append);

        assertTrue(installer.isInstalled(ModLoader.FORGE, mc, newest.get()),
                "Forge should be detected as installed after a successful install. Installer log:\n" + log);

        List<String> tailArgs = ModLoaderLaunchLine.buildTailArgs(ModLoader.FORGE, mc, newest.get(), serverDir);
        assertTrue(tailArgs.get(0).startsWith("@"), "MC 1.20.1 Forge should launch via an @args-file reference");
    }
}
