package com.zerog.stellarserverforge.launch;

import com.zerog.stellarserverforge.model.ServerSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds the JVM argument list for launching a server, mirroring spec §8: RAM (-Xmx), the
 * G1GC-tuning ARGS (suppressed on Java 17+ if left at their default value, since newer JVMs
 * self-tune GC better), and the always-applied OTHERARGS.
 */
public final class LaunchArgsBuilder {

    private LaunchArgsBuilder() {
    }

    public static List<String> buildJvmArgs(ServerSettings settings, int resolvedJavaMajorVersion) {
        List<String> jvmArgs = new ArrayList<>();
        jvmArgs.add("-Xmx" + settings.getMaxRamGigs() + "G");

        boolean suppressDefaultTuning = resolvedJavaMajorVersion >= 17 && settings.hasDefaultArgs();
        if (!suppressDefaultTuning && settings.getArgs() != null && !settings.getArgs().isBlank()) {
            jvmArgs.addAll(splitArgs(settings.getArgs()));
        }

        jvmArgs.addAll(splitArgs(ServerSettings.OTHER_ARGS));
        return jvmArgs;
    }

    private static List<String> splitArgs(String args) {
        return Arrays.stream(args.trim().split("\\s+"))
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
