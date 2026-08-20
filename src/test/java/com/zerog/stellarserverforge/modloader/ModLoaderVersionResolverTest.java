package com.zerog.stellarserverforge.modloader;

import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModLoaderVersionResolverTest {

    private static final String NEOFORGE_METADATA = """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata>
              <groupId>net.neoforged</groupId>
              <artifactId>neoforge</artifactId>
              <versioning>
                <release>20.4.190</release>
                <versions>
                  <version>20.4.185</version>
                  <version>20.4.190</version>
                  <version>20.2.88</version>
                  <version>20.4.190-beta</version>
                </versions>
              </versioning>
            </metadata>
            """;

    @Test
    void resolvesNewestNeoForgeVersionForMatchingMajorMinor(@TempDir Path tempDir) throws Exception {
        Path metadataFile = tempDir.resolve("maven-neoforge-metadata.xml");
        Files.writeString(metadataFile, NEOFORGE_METADATA);

        ModLoaderVersionResolver resolver = new ModLoaderVersionResolver();
        McVersion mc = McVersion.parse("1.20.4");

        Optional<String> newest = resolver.resolveNewest(ModLoader.NEOFORGE, mc, metadataFile, null);

        assertTrue(newest.isPresent());
        assertEquals("20.4.190", newest.get());
    }

    @Test
    void neoForge1_20_1UsesHardcodedVersion(@TempDir Path tempDir) throws Exception {
        ModLoaderVersionResolver resolver = new ModLoaderVersionResolver();
        McVersion mc = McVersion.parse("1.20.1");

        // Metadata file/path is irrelevant for the 1.20.1 hardcoded branch.
        Optional<String> newest = resolver.resolveNewest(ModLoader.NEOFORGE, mc, tempDir.resolve("unused.xml"), null);

        assertEquals(Optional.of("47.1.106"), newest);
    }

    @Test
    void throwsWhenNoNeoForgeVersionsMatchMcVersion(@TempDir Path tempDir) throws IOException {
        Path metadataFile = tempDir.resolve("maven-neoforge-metadata.xml");
        Files.writeString(metadataFile, NEOFORGE_METADATA);

        ModLoaderVersionResolver resolver = new ModLoaderVersionResolver();
        McVersion mc = McVersion.parse("1.19.2"); // no matching entries in the sample metadata

        org.junit.jupiter.api.Assertions.assertThrows(
                ModLoaderVersionResolver.NoMatchingVersionException.class,
                () -> resolver.resolveNewest(ModLoader.NEOFORGE, mc, metadataFile, null));
    }

    @Test
    void isValidVersionChecksNeoForgeBareVersionMembership(@TempDir Path tempDir) throws Exception {
        Path metadataFile = tempDir.resolve("maven-neoforge-metadata.xml");
        Files.writeString(metadataFile, NEOFORGE_METADATA);

        ModLoaderVersionResolver resolver = new ModLoaderVersionResolver();
        McVersion mc = McVersion.parse("1.20.4");

        assertTrue(resolver.isValidVersion(ModLoader.NEOFORGE, mc, metadataFile, "20.4.185"));
        assertFalse(resolver.isValidVersion(ModLoader.NEOFORGE, mc, metadataFile, "99.99.99"));
    }

    @Test
    void containsLettersDetectsAlphaCharacters() {
        assertTrue(ModLoaderVersionResolver.containsLetters("20.4.190-beta"));
        assertFalse(ModLoaderVersionResolver.containsLetters("47.1.3"));
    }
}
