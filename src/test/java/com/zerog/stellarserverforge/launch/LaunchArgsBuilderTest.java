package com.zerog.stellarserverforge.launch;

import com.zerog.stellarserverforge.model.ServerSettings;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchArgsBuilderTest {

    @Test
    void java17WithDefaultArgsSuppressesG1gcTuning() {
        ServerSettings settings = new ServerSettings();
        settings.setMaxRamGigs(4);
        // ARGS left at ServerSettings.DEFAULT_ARGS by default.

        List<String> jvmArgs = LaunchArgsBuilder.buildJvmArgs(settings, 17);

        assertEquals("-Xmx4G", jvmArgs.get(0));
        assertFalse(jvmArgs.contains("-XX:+UseG1GC"));
        assertTrue(jvmArgs.contains("-Dlog4j2.formatMsgNoLookups=true")); // OTHERARGS always applied
    }

    @Test
    void java8WithDefaultArgsKeepsG1gcTuning() {
        ServerSettings settings = new ServerSettings();
        settings.setMaxRamGigs(6);

        List<String> jvmArgs = LaunchArgsBuilder.buildJvmArgs(settings, 8);

        assertTrue(jvmArgs.contains("-XX:+UseG1GC"));
    }

    @Test
    void customArgsAreKeptRegardlessOfJavaVersion() {
        ServerSettings settings = new ServerSettings();
        settings.setMaxRamGigs(4);
        settings.setArgs("-XX:+UseZGC");

        List<String> jvmArgs = LaunchArgsBuilder.buildJvmArgs(settings, 21);

        assertTrue(jvmArgs.contains("-XX:+UseZGC"));
        assertFalse(jvmArgs.contains("-XX:+UseG1GC"));
    }

    @Test
    void otherArgsAlwaysAppendedEvenWithCustomArgs() {
        ServerSettings settings = new ServerSettings();
        settings.setMaxRamGigs(4);
        settings.setArgs("-XX:+UseZGC");

        List<String> jvmArgs = LaunchArgsBuilder.buildJvmArgs(settings, 8);

        assertTrue(jvmArgs.contains("-XX:+IgnoreUnrecognizedVMOptions"));
        assertTrue(jvmArgs.contains("-Dlog4j2.formatMsgNoLookups=true"));
    }
}
