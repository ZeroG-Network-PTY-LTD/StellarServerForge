package com.zerog.network.stellarforge.utils;

import com.zerog.network.stellarforge.api.CurseForgeClient;
import com.zerog.network.stellarforge.api.ModrinthClient;
import com.zerog.network.stellarforge.model.ModInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

/**
 * Checks whether installed mods have newer versions available.
 *
 * How it works:
 *  1. Scans installed jar files in the mods directory
 *  2. For each jar that matches a known ModInfo (with projectId + version), queries the API
 *  3. Compares version strings and reports outdated mods
 */
public class ModUpdateChecker {

    private static final Logger logger = LoggerFactory.getLogger(ModUpdateChecker.class);

    private final File modsDirectory;
    private final String minecraftVersion;
    private final String modLoader;

    private final ModrinthClient modrinthClient;
    private final CurseForgeClient curseForgeClient;

    public ModUpdateChecker(File modsDirectory, String minecraftVersion, String modLoader) {
        this.modsDirectory    = modsDirectory;
        this.minecraftVersion = minecraftVersion;
        this.modLoader        = modLoader;
        this.modrinthClient   = new ModrinthClient();

        CurseForgeClient cf = null;
        try {
            cf = new CurseForgeClient();
        } catch (Exception e) {
            logger.warn("CurseForge client unavailable for update checks: {}", e.getMessage());
        }
        this.curseForgeClient = cf;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Synchronously check a list of installed mods for updates.
     * Called from a background thread (SwingWorker).
     *
     * @param installed  List of ModInfo representing currently-installed mods
     * @param progress   Optional progress callback (mod name being checked)
     * @return List of UpdateInfo for mods that have updates available
     */
    public List<UpdateInfo> checkForUpdates(List<ModInfo> installed, Consumer<String> progress) {
        List<UpdateInfo> updates = new ArrayList<>();

        for (ModInfo mod : installed) {
            if (progress != null) progress.accept(mod.getName());
            try {
                UpdateInfo info = checkMod(mod);
                if (info != null) updates.add(info);
            } catch (Exception e) {
                logger.warn("Update check failed for {}: {}", mod.getName(), e.getMessage());
            }
        }

        logger.info("Update check complete: {} update(s) found out of {} mods",
                updates.size(), installed.size());
        return updates;
    }

    /**
     * Build a list of ModInfo from jar files in the mods directory.
     * Filename is used as a proxy for identification.
     */
    public List<ModInfo> scanInstalledMods() {
        List<ModInfo> result = new ArrayList<>();
        if (modsDirectory == null || !modsDirectory.exists()) return result;

        File[] files = modsDirectory.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null) return result;

        for (File f : files) {
            ModInfo m = new ModInfo();
            m.setFileName(f.getName());
            m.setName(f.getName().replace(".jar", ""));
            m.setSource(ModInfo.ModSource.LOCAL);
            m.setInstalled(true);
            result.add(m);
        }

        logger.info("Found {} installed mods in {}", result.size(), modsDirectory);
        return result;
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private UpdateInfo checkMod(ModInfo mod) {
        if (mod.getProjectId() == null || mod.getProjectId().isEmpty()) return null;
        if (mod.getVersion()   == null) return null;

        ModInfo.ModSource source = mod.getSource();

        if (source == ModInfo.ModSource.MODRINTH) {
            return checkModrinthUpdate(mod);
        } else if (source == ModInfo.ModSource.CURSEFORGE && curseForgeClient != null) {
            return checkCurseForgeUpdate(mod);
        }

        // Unknown source: try Modrinth first, then CurseForge
        UpdateInfo info = checkModrinthUpdate(mod);
        if (info == null && curseForgeClient != null) {
            info = checkCurseForgeUpdate(mod);
        }
        return info;
    }

    private UpdateInfo checkModrinthUpdate(ModInfo mod) {
        ModrinthClient.ProjectVersion latest =
                modrinthClient.getLatestVersion(mod.getProjectId(), minecraftVersion, modLoader);
        if (latest == null || latest.versionNumber == null) return null;

        if (!mod.getVersion().equals(latest.versionNumber)) {
            logger.info("Update available for {} (Modrinth): {} → {}",
                    mod.getName(), mod.getVersion(), latest.versionNumber);
            return new UpdateInfo(mod, mod.getVersion(), latest.versionNumber, latest.downloadUrl);
        }
        return null;
    }

    private UpdateInfo checkCurseForgeUpdate(ModInfo mod) {
        CurseForgeClient.ProjectVersion latest =
                curseForgeClient.getLatestVersion(mod.getProjectId(), minecraftVersion, modLoader);
        if (latest == null || latest.fileId == null) return null;

        // Compare by file ID (most reliable for CurseForge)
        String installedFileId = mod.getFileId();
        if (installedFileId != null && !installedFileId.equals(latest.fileId)) {
            String latestLabel = latest.displayName != null ? latest.displayName : latest.fileId;
            String currentLabel = mod.getVersion() != null ? mod.getVersion() : installedFileId;
            logger.info("Update available for {} (CurseForge): {} → {}",
                    mod.getName(), currentLabel, latestLabel);
            return new UpdateInfo(mod, currentLabel, latestLabel, latest.downloadUrl);
        }
        return null;
    }

    // ── Result model ──────────────────────────────────────────────────────────

    public static class UpdateInfo {
        public final ModInfo mod;
        public final String currentVersion;
        public final String latestVersion;
        public final String downloadUrl;

        public UpdateInfo(ModInfo mod, String currentVersion, String latestVersion, String downloadUrl) {
            this.mod            = mod;
            this.currentVersion = currentVersion;
            this.latestVersion  = latestVersion;
            this.downloadUrl    = downloadUrl;
        }

        public String getSummary() {
            return mod.getName() + ": " + currentVersion + " → " + latestVersion;
        }
    }
}
