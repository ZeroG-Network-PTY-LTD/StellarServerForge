package com.zerog.network.stellarforge.utils;

import com.zerog.network.stellarforge.model.ModDependency;
import com.zerog.network.stellarforge.model.ModInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Analyses a list of mods to be installed and resolves missing dependencies.
 *
 * Usage:
 *   DependencyResolver resolver = new DependencyResolver(modsDirectory);
 *   DependencyResolver.Resolution result = resolver.resolve(modsToInstall);
 *   result.getMissing()  -> required deps not in the install list or mods folder
 *   result.getOptional() -> optional deps not present
 */
public class DependencyResolver {

    private static final Logger logger = LoggerFactory.getLogger(DependencyResolver.class);

    private final File modsDirectory;

    public DependencyResolver(File modsDirectory) {
        this.modsDirectory = modsDirectory;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public Resolution resolve(List<ModInfo> modsToInstall) {
        Set<String> installedIds  = getInstalledModIds();
        Set<String> installIds    = new HashSet<>();
        for (ModInfo m : modsToInstall) {
            if (m.getProjectId() != null) installIds.add(m.getProjectId());
        }

        List<ModDependency> missing  = new ArrayList<>();
        List<ModDependency> optional = new ArrayList<>();
        List<ModDependency> conflicts = new ArrayList<>();

        for (ModInfo mod : modsToInstall) {
            if (mod.getDependencies() == null) continue;
            for (String depId : mod.getDependencies()) {
                if (depId == null || depId.isEmpty()) continue;
                ModDependency dep = ModDependency.required(mod.getProjectId(), depId, depId);
                if (!installedIds.contains(depId) && !installIds.contains(depId)) {
                    missing.add(dep);
                    logger.debug("Missing required dependency: {} -> {}", mod.getName(), depId);
                }
            }
        }

        return new Resolution(missing, optional, conflicts);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns a set of "identifiers" for already-installed mods (filename stems). */
    private Set<String> getInstalledModIds() {
        Set<String> ids = new HashSet<>();
        if (modsDirectory == null || !modsDirectory.exists()) return ids;
        File[] files = modsDirectory.listFiles();
        if (files == null) return ids;
        for (File f : files) {
            if (f.getName().endsWith(".jar")) {
                // Use filename without extension as a rough ID
                ids.add(f.getName().replace(".jar", "").toLowerCase());
            }
        }
        return ids;
    }

    // ── Result model ──────────────────────────────────────────────────────────

    public static class Resolution {
        private final List<ModDependency> missing;
        private final List<ModDependency> optional;
        private final List<ModDependency> conflicts;

        public Resolution(List<ModDependency> missing,
                          List<ModDependency> optional,
                          List<ModDependency> conflicts) {
            this.missing   = missing;
            this.optional  = optional;
            this.conflicts = conflicts;
        }

        public List<ModDependency> getMissing()   { return missing; }
        public List<ModDependency> getOptional()  { return optional; }
        public List<ModDependency> getConflicts() { return conflicts; }

        public boolean isClean() {
            return missing.isEmpty() && conflicts.isEmpty();
        }

        public String getSummary() {
            if (isClean() && optional.isEmpty()) return "All dependencies satisfied.";
            StringBuilder sb = new StringBuilder();
            if (!missing.isEmpty())   sb.append(missing.size()).append(" missing required dep(s). ");
            if (!optional.isEmpty())  sb.append(optional.size()).append(" optional dep(s) not installed. ");
            if (!conflicts.isEmpty()) sb.append(conflicts.size()).append(" conflict(s) detected. ");
            return sb.toString().trim();
        }
    }
}

