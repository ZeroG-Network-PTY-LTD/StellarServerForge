package com.zerog.stellarserverforge.javamanaged;

import com.zerog.stellarserverforge.model.McVersion;

import java.util.List;

/**
 * The Minecraft-version -> required-Java-major-version rule table (spec §7.1).
 */
public final class JavaVersionRules {

    private JavaVersionRules() {
    }

    /** {@code options} has one element when there's no real choice; otherwise the first is the default. */
    public record JavaOptions(int defaultVersion, List<Integer> options) {
        public boolean onlyOneChoice() {
            return options.size() == 1;
        }
    }

    public static JavaOptions resolve(McVersion mc) {
        int major = mc.major();
        int minor = mc.minor();

        if (major <= 15) {
            return only(8);
        }
        if (major <= 16 && minor <= 4) {
            return only(8);
        }
        if (major <= 16) { // minor >= 5, i.e. 1.16.5
            return new JavaOptions(8, List.of(8, 11));
        }
        if (major == 17) {
            return only(16);
        }
        if (major >= 18 && major <= 19) {
            return new JavaOptions(17, List.of(17, 21, 25));
        }
        if (major == 20 && minor <= 5) {
            return new JavaOptions(17, List.of(17, 21, 25));
        }
        if (major == 20) { // minor >= 6
            return new JavaOptions(21, List.of(21, 25));
        }
        if (major == 21) {
            return new JavaOptions(21, List.of(21, 25));
        }
        // major > 21
        return only(25);
    }

    private static JavaOptions only(int version) {
        return new JavaOptions(version, List.of(version));
    }
}
