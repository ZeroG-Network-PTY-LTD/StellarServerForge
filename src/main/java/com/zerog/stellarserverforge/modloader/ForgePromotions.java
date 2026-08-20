package com.zerog.stellarserverforge.modloader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** Reads Forge's {@code promotions_slim.json} "latest per Minecraft version" index (spec §6.3). */
public final class ForgePromotions {

    private ForgePromotions() {
    }

    public static Optional<String> latestFor(Path promotionsJson, String minecraftVersion) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(promotionsJson.toFile());
        JsonNode promos = root.path("promos");
        JsonNode value = promos.get(minecraftVersion + "-latest");
        if (value == null || value.isMissingNode() || value.isNull()) {
            return Optional.empty();
        }
        return Optional.of(value.asText());
    }
}
