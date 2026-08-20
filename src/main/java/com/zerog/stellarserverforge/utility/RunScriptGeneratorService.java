package com.zerog.stellarserverforge.utility;

import com.zerog.stellarserverforge.launch.LaunchArgsBuilder;
import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;
import com.zerog.stellarserverforge.model.ServerSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Generates standalone {@code run.sh}/{@code run.bat} scripts for Forge/NeoForge (spec §13.3),
 * so the server can be started without StellarServerForge once installed.
 */
public class RunScriptGeneratorService {

    public void generate(ServerSettings settings, McVersion mc, Path serverDir) throws IOException {
        ModLoader loader = settings.getModLoader();
        if (loader != ModLoader.FORGE && loader != ModLoader.NEOFORGE) {
            throw new IOException("Run script generation is only supported for Forge/NeoForge.");
        }

        List<String> jvmArgs = LaunchArgsBuilder.buildJvmArgs(settings, settings.getJavaVersion());
        String jvmArgsLine = String.join(" ", jvmArgs);

        if (mc.major() <= 16) {
            String jarName = findLegacyJar(serverDir, mc, settings.getModLoaderVersion())
                    .orElseThrow(() -> new IOException("Could not find the installed Forge server jar — launch the server at least once first."));
            Files.deleteIfExists(serverDir.resolve("user_jvm_args.txt"));
            writeScript(serverDir.resolve("run.sh"),
                    "#!/usr/bin/env sh\n" + "java " + jvmArgsLine + " -jar " + jarName + " nogui \"$@\"\n");
            writeScript(serverDir.resolve("run.bat"),
                    "@echo off\r\n" + "java " + jvmArgsLine + " -jar " + jarName + " nogui %*\r\nPAUSE\r\n");
        } else {
            String argsDirRelative = argsDirRelative(loader, mc, settings.getModLoaderVersion());
            Path unixArgs = serverDir.resolve(argsDirRelative).resolve("unix_args.txt");
            Path winArgs = serverDir.resolve(argsDirRelative).resolve("win_args.txt");
            if (!Files.isRegularFile(unixArgs) && !Files.isRegularFile(winArgs)) {
                throw new IOException("The modloader launch args file was not found — launch the server at least once first.");
            }
            Files.writeString(serverDir.resolve("user_jvm_args.txt"), jvmArgsLine + System.lineSeparator(), StandardCharsets.UTF_8);
            writeScript(serverDir.resolve("run.sh"),
                    "#!/usr/bin/env sh\n" + "java @user_jvm_args.txt @" + argsDirRelative + "/unix_args.txt nogui \"$@\"\n");
            writeScript(serverDir.resolve("run.bat"),
                    "@echo off\r\n" + "java @user_jvm_args.txt @" + argsDirRelative + "/win_args.txt nogui %*\r\nPAUSE\r\n");
        }
    }

    private String argsDirRelative(ModLoader loader, McVersion mc, String version) {
        if (loader == ModLoader.NEOFORGE) {
            return "1.20.1".equals(mc.raw())
                    ? "libraries/net/neoforged/forge/" + mc.raw() + "-" + version
                    : "libraries/net/neoforged/neoforge/" + version;
        }
        return "libraries/net/minecraftforge/forge/" + mc.raw() + "-" + version;
    }

    private java.util.Optional<String> findLegacyJar(Path serverDir, McVersion mc, String version) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(serverDir, "*.jar")) {
            for (Path p : stream) {
                String name = p.getFileName().toString();
                if (name.contains(mc.raw()) && name.contains(version)) {
                    return java.util.Optional.of(name);
                }
            }
        }
        return java.util.Optional.empty();
    }

    private void writeScript(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
        path.toFile().setExecutable(true);
    }
}
