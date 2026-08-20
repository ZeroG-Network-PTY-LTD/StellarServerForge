package com.zerog.stellarserverforge.modloader;

import com.zerog.stellarserverforge.model.McVersion;
import com.zerog.stellarserverforge.model.ModLoader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Builds the tail of the java command line (the launch target and anything after it) for each
 * loader (spec §6.7). {@code win_args.txt}/{@code unix_args.txt} already contain the modloader's
 * own required JVM/classpath args; the JVM tuning args built by {@link
 * com.zerog.stellarserverforge.launch.LaunchArgsBuilder} are injected ahead of the {@code @file}
 * reference by the caller.
 */
public final class ModLoaderLaunchLine {

    private ModLoaderLaunchLine() {
    }

    public static List<String> buildTailArgs(ModLoader loader, McVersion mc, String modLoaderVersion, Path serverDir)
            throws IOException {
        return switch (loader) {
            case VANILLA -> List.of("-jar", "minecraft_server." + mc.raw() + ".jar", "nogui");
            case FABRIC -> List.of("-jar", "fabric-server-launch-" + mc.raw() + "-" + modLoaderVersion + ".jar", "nogui");
            case QUILT -> List.of("-jar", "quilt-server-launch-" + mc.raw() + "-" + modLoaderVersion + ".jar", "nogui");
            case FORGE -> mc.major() <= 16
                    ? legacyForgeJarTail(mc, modLoaderVersion, serverDir)
                    : argsFileTail("libraries/net/minecraftforge/forge/" + mc.raw() + "-" + modLoaderVersion, serverDir);
            case NEOFORGE -> "1.20.1".equals(mc.raw())
                    ? argsFileTail("libraries/net/neoforged/forge/" + mc.raw() + "-" + modLoaderVersion, serverDir)
                    : argsFileTail("libraries/net/neoforged/neoforge/" + modLoaderVersion, serverDir);
        };
    }

    private static List<String> legacyForgeJarTail(McVersion mc, String version, Path serverDir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(serverDir, "*.jar")) {
            for (Path p : stream) {
                String name = p.getFileName().toString();
                if (name.contains(mc.raw()) && name.contains(version)) {
                    return List.of("-jar", name, "nogui");
                }
            }
        }
        throw new IOException("Could not find the installed Forge server jar for " + mc.raw() + "-" + version);
    }

    private static List<String> argsFileTail(String argsDirRelative, Path serverDir) throws IOException {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String argsFileName = windows ? "win_args.txt" : "unix_args.txt";
        Path argsFile = serverDir.resolve(argsDirRelative).resolve(argsFileName);
        if (!Files.isRegularFile(argsFile)) {
            throw new IOException("Expected launch args file not found: " + argsFile);
        }
        return List.of("@" + argsDirRelative + "/" + argsFileName, "nogui");
    }
}
