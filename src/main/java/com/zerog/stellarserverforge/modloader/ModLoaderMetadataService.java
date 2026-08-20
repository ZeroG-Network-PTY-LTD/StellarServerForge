package com.zerog.stellarserverforge.modloader;

import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.net.HttpFetcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * Fetches/caches each modloader's maven-metadata.xml (and Forge's promotions_slim.json),
 * refreshed every 6 hours, degrading gracefully to a stale cached copy if the fetch fails
 * (spec §6.1).
 */
public class ModLoaderMetadataService {

    private static final Duration REFRESH_INTERVAL = Duration.ofHours(6);

    private final HttpFetcher http = new HttpFetcher();
    private final Path cacheDir;

    public ModLoaderMetadataService(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    public record MetadataSource(String fileName, String url) {
    }

    public static MetadataSource metadataSourceFor(ModLoader loader, McVersion mc) {
        return switch (loader) {
            case FABRIC -> new MetadataSource("maven-fabric-metadata.xml",
                    "https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml");
            case QUILT -> new MetadataSource("maven-quilt-metadata.xml",
                    "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-loader/maven-metadata.xml");
            case FORGE -> new MetadataSource("maven-forge-metadata.xml",
                    "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml");
            case NEOFORGE -> "1.20.1".equals(mc.raw())
                    ? new MetadataSource("maven-neoforge-1.20.1-metadata.xml",
                    "https://maven.neoforged.net/releases/net/neoforged/forge/maven-metadata.xml")
                    : new MetadataSource("maven-neoforge-metadata.xml",
                    "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml");
            case VANILLA -> throw new IllegalArgumentException("Vanilla has no modloader metadata");
        };
    }

    private static final String PROMOTIONS_URL =
            "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json";
    private static final String PROMOTIONS_FILE = "promotions_slim.json";

    public Path ensureMetadataFile(ModLoader loader, McVersion mc) throws IOException, InterruptedException {
        MetadataSource source = metadataSourceFor(loader, mc);
        return ensureFresh(source.fileName(), source.url());
    }

    public Path ensurePromotionsFile() throws IOException, InterruptedException {
        return ensureFresh(PROMOTIONS_FILE, PROMOTIONS_URL);
    }

    private Path ensureFresh(String fileName, String url) throws IOException, InterruptedException {
        Files.createDirectories(cacheDir);
        Path path = cacheDir.resolve(fileName);

        boolean stale = true;
        if (Files.exists(path)) {
            Instant modified = Files.getLastModifiedTime(path).toInstant();
            stale = Instant.now().isAfter(modified.plus(REFRESH_INTERVAL));
        }
        if (!stale) {
            return path;
        }

        try {
            String body = http.getString(url);
            Files.writeString(path, body, StandardCharsets.UTF_8);
        } catch (IOException e) {
            if (!Files.exists(path)) {
                throw e;
            }
            // Degrade gracefully: keep using the stale cached copy if the modloader host is unreachable.
        }
        return path;
    }
}
