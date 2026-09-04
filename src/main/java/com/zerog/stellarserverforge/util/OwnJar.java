package com.zerog.stellarserverforge.util;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Locates the jar StellarServerForge itself is currently running from, when packaged as one. Used
 * to make sure a "wipe the server jar(s)" operation (mod loader reinstall, purge) never tries to
 * delete the app's own running jar — a real scenario when the app jar is placed directly in the
 * server folder it manages (see {@code MainFrame#resolveServerDir}), where a locked/in-use file
 * fails the delete with an {@code IOException} on Windows.
 */
public final class OwnJar {

    private OwnJar() {
    }

    /** Returns the absolute, normalized path to the running jar, or {@code null} when not running
     * from a jar (e.g. via an IDE launcher or {@code gradlew run}). */
    public static Path path() {
        try {
            Path location = Path.of(OwnJar.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(location)) {
                return location.toAbsolutePath().normalize();
            }
        } catch (URISyntaxException | RuntimeException ignored) {
            // Not running from a jar, or code source unavailable.
        }
        return null;
    }
}
