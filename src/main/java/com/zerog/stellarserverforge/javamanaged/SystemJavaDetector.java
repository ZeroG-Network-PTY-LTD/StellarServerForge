package com.zerog.stellarserverforge.javamanaged;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans common JDK install locations for a JDK matching a target major version, mirroring
 * the folder-name-regex approach from spec §7.2 (system Java detection, mode A).
 */
public final class SystemJavaDetector {

    private static final Pattern[] FOLDER_PATTERNS = {
            Pattern.compile("jdk-?(\\d+).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("temurin-?(\\d+).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("jre-?(\\d+).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("zulu-?(\\d+).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("jdk1\\.(\\d+).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("java-?(\\d+).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("openjdk-?(\\d+).*", Pattern.CASE_INSENSITIVE),
    };

    private SystemJavaDetector() {
    }

    private static List<Path> candidateRoots() {
        List<Path> roots = new ArrayList<>();
        if (isWindows()) {
            String[] bases = {
                    "C:\\Program Files\\Java",
                    "C:\\Program Files\\Eclipse Adoptium",
                    "C:\\Program Files\\Eclipse Foundation",
                    "C:\\Program Files\\Amazon Corretto",
                    "C:\\Program Files\\Zulu",
            };
            for (String base : bases) {
                Path p = Path.of(base);
                if (Files.isDirectory(p)) {
                    roots.add(p);
                }
            }
        } else {
            String[] bases = {"/usr/lib/jvm", "/opt/java", "/Library/Java/JavaVirtualMachines"};
            for (String base : bases) {
                Path p = Path.of(base);
                if (Files.isDirectory(p)) {
                    roots.add(p);
                }
            }
        }
        return roots;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public static Optional<Path> findJavaExecutable(int majorVersion) {
        String exeName = isWindows() ? "java.exe" : "java";
        for (Path root : candidateRoots()) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
                for (Path dir : stream) {
                    if (!Files.isDirectory(dir)) {
                        continue;
                    }
                    String name = dir.getFileName().toString();
                    if (!folderMatchesVersion(name, majorVersion)) {
                        continue;
                    }
                    Path javaBin = isWindows() ? dir.resolve("bin").resolve(exeName) : dir.resolve("bin").resolve(exeName);
                    if (Files.isRegularFile(javaBin)) {
                        return Optional.of(javaBin);
                    }
                }
            } catch (IOException ignored) {
                // Skip unreadable roots.
            }
        }
        return Optional.empty();
    }

    private static boolean folderMatchesVersion(String folderName, int majorVersion) {
        for (Pattern pattern : FOLDER_PATTERNS) {
            Matcher m = pattern.matcher(folderName);
            if (m.matches()) {
                try {
                    int found = Integer.parseInt(m.group(1));
                    if (found == majorVersion) {
                        return true;
                    }
                } catch (NumberFormatException ignored) {
                    // Fall through to next pattern.
                }
            }
        }
        return false;
    }
}
