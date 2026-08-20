package com.zerog.network.stellarforge.utils;

import com.zerog.network.stellarforge.model.ModInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects potential conflicts between mods before installation.
 *
 * Checks:
 *  - Duplicate mod JARs (same project ID detected twice)
 *  - Known incompatible pairs
 *  - Mods already present in the mods folder
 */
public class ConflictDetector {

    private static final Logger logger = LoggerFactory.getLogger(ConflictDetector.class);

    // Known incompatible pairs (simplified; extend as needed)
    private static final String[][] KNOWN_CONFLICTS = {
        {"optifine", "sodium"},
        {"optifine", "iris"},
        {"phosphor", "starlight"},
    };

    private final File modsDirectory;

    public ConflictDetector(File modsDirectory) {
        this.modsDirectory = modsDirectory;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Analyse a list of mods-to-install and return any conflicts found.
     */
    public List<Conflict> detect(List<ModInfo> toInstall) {
        List<Conflict> conflicts = new ArrayList<>();

        // 1. Duplicates within the install list
        for (int i = 0; i < toInstall.size(); i++) {
            for (int j = i + 1; j < toInstall.size(); j++) {
                ModInfo a = toInstall.get(i);
                ModInfo b = toInstall.get(j);
                if (a.getProjectId() != null && a.getProjectId().equals(b.getProjectId())) {
                    conflicts.add(new Conflict(ConflictType.DUPLICATE, a, b,
                            "Both entries refer to the same mod project."));
                }
            }
        }

        // 2. Already installed
        if (modsDirectory != null && modsDirectory.exists()) {
            File[] installed = modsDirectory.listFiles((dir, name) -> name.endsWith(".jar"));
            if (installed != null) {
                for (ModInfo mod : toInstall) {
                    String fn = mod.getFileName();
                    if (fn != null) {
                        for (File f : installed) {
                            if (f.getName().equalsIgnoreCase(fn)) {
                                conflicts.add(new Conflict(ConflictType.ALREADY_INSTALLED, mod, null,
                                        fn + " is already in the mods folder."));
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 3. Known incompatible pairs
        for (String[] pair : KNOWN_CONFLICTS) {
            ModInfo modA = findByKeyword(toInstall, pair[0]);
            ModInfo modB = findByKeyword(toInstall, pair[1]);
            if (modA != null && modB != null) {
                conflicts.add(new Conflict(ConflictType.INCOMPATIBLE, modA, modB,
                        modA.getName() + " and " + modB.getName() + " are known to be incompatible."));
            }
        }

        if (!conflicts.isEmpty()) {
            logger.warn("ConflictDetector found {} conflict(s)", conflicts.size());
        }
        return conflicts;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ModInfo findByKeyword(List<ModInfo> mods, String keyword) {
        for (ModInfo m : mods) {
            if (m.getName() != null && m.getName().toLowerCase().contains(keyword)) return m;
            if (m.getSlug() != null && m.getSlug().toLowerCase().contains(keyword)) return m;
        }
        return null;
    }

    // ── Result types ──────────────────────────────────────────────────────────

    public enum ConflictType {
        DUPLICATE("Duplicate"),
        ALREADY_INSTALLED("Already Installed"),
        INCOMPATIBLE("Incompatible");

        private final String display;
        ConflictType(String d) { this.display = d; }
        public String getDisplay() { return display; }
    }

    public static class Conflict {
        public final ConflictType type;
        public final ModInfo modA;
        public final ModInfo modB;    // may be null for single-mod conflicts
        public final String reason;

        public Conflict(ConflictType type, ModInfo modA, ModInfo modB, String reason) {
            this.type   = type;
            this.modA   = modA;
            this.modB   = modB;
            this.reason = reason;
        }

        public boolean isBlocking() {
            return type == ConflictType.INCOMPATIBLE;
        }

        @Override
        public String toString() {
            return "[" + type.getDisplay() + "] " + reason;
        }
    }
}

