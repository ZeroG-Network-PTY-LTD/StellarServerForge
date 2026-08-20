package com.zerog.stellarserverforge.mojang;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A real network smoke test: fetches the live Mojang manifest, downloads the (small) 1.8.8
 * vanilla server jar, and verifies it against Mojang's own published SHA1 — proving the
 * manifest-fetch/version-lookup/download/checksum-verify path actually works end to end, not
 * just that it compiles. Excluded from the default {@code test} task since it needs network
 * access; run explicitly with {@code -Dgroups=live}.
 */
@Tag("live")
class VanillaInstallServiceLiveSmokeTest {

    @Test
    void downloadsAndVerifiesRealVanillaServerJar(@TempDir Path tempDir) throws Exception {
        Path cacheDir = tempDir.resolve("cache");
        Path serverDir = tempDir.resolve("server");
        Files.createDirectories(serverDir);

        MojangManifestService manifestService = new MojangManifestService(cacheDir);
        MojangManifestService.VersionEntry version = manifestService.findVersion("1.8.8");
        assertTrue(version != null, "1.8.8 should be a known release version in the live Mojang manifest");

        VanillaInstallService installService = new VanillaInstallService(cacheDir, serverDir);
        Path jarPath = installService.ensureInstalled(version);

        assertTrue(Files.isRegularFile(jarPath), "server jar should have been downloaded");
        assertTrue(Files.size(jarPath) > 1000, "downloaded jar should not be empty/truncated");
    }
}
