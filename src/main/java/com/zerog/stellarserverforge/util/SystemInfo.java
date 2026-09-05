package com.zerog.stellarserverforge.util;

import java.lang.management.ManagementFactory;

/** Small host-machine facts used to sanity-check user input (e.g. RAM allocation) against what's
 * actually available, rather than letting a spinner/slider accept any number. */
public final class SystemInfo {

    private SystemInfo() {
    }

    /** Best-effort total physical RAM, in GB, via the JDK's HotSpot-specific MXBean extension.
     * Returns -1 if unavailable (a non-HotSpot JVM) so callers can skip a RAM-relative check
     * rather than guess. */
    public static long totalRamGigs() {
        try {
            var os = ManagementFactory.getOperatingSystemMXBean();
            if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
                return sunOs.getTotalMemorySize() / (1024L * 1024 * 1024);
            }
        } catch (Throwable ignored) {
            // Not available on this JVM — skip the check rather than guess.
        }
        return -1;
    }
}
