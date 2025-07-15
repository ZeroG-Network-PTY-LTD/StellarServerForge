package com.zerog.network.stellarforge.modpack;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.zerog.network.stellarforge.api.CurseForgeClient;
import com.zerog.network.stellarforge.model.ModInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.Collectors;

/**
 * Handles importing modpacks from ZIP files
 * Supports CurseForge manifest format and HTML mod lists
 */
public class ModpackImporter {
    
    private final CurseForgeClient curseForgeClient;
    private final Gson gson;
    
    public ModpackImporter() {
        this.curseForgeClient = new CurseForgeClient();
        this.gson = new Gson();
    }
    
    /**
     * Import a modpack from a ZIP file
     */
    public ModpackImportResult importModpack(String zipFilePath, JFrame parent) {
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            
            // Step 1: Look for manifest.json
            ModpackManifest manifest = findManifest(zipFile);
            
            // Step 2: Look for HTML mod list if no manifest
            List<ModInfo> htmlMods = new ArrayList<>();
            if (manifest == null) {
                htmlMods = findModsFromHTML(zipFile);
            }
            
            // Step 3: Prompt user for configuration
            ModpackConfig config = promptUserConfiguration(manifest, parent);
            if (config == null) {
                return null; // User cancelled
            }
            
            // Step 4: Process mods
            List<ModInfo> modsToInstall = new ArrayList<>();
            
            if (manifest != null) {
                modsToInstall = processManifestMods(manifest, config);
            } else if (!htmlMods.isEmpty()) {
                modsToInstall = processHTMLMods(htmlMods, config);
            }
            
            // Step 5: Filter server-side mods
            List<ModInfo> serverMods = filterServerSideMods(modsToInstall);
            
            return new ModpackImportResult(config, serverMods, manifest != null);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent, 
                "Error importing modpack: " + e.getMessage(), 
                "Import Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
    
    /**
     * Find and parse manifest.json from ZIP
     */
    private ModpackManifest findManifest(ZipFile zipFile) {
        try {
            ZipEntry manifestEntry = zipFile.getEntry("manifest.json");
            if (manifestEntry == null) {
                // Try alternative locations
                manifestEntry = zipFile.getEntry("modpack/manifest.json");
                if (manifestEntry == null) {
                    return null;
                }
            }
            
            try (InputStream is = zipFile.getInputStream(manifestEntry);
                 InputStreamReader reader = new InputStreamReader(is)) {
                
                return gson.fromJson(reader, ModpackManifest.class);
            }
        } catch (Exception e) {
            System.err.println("Error reading manifest: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Find mods from HTML files in ZIP
     */
    private List<ModInfo> findModsFromHTML(ZipFile zipFile) {
        List<ModInfo> mods = new ArrayList<>();
        
        try {
            // Look for HTML files
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().toLowerCase().endsWith(".html")) {
                    mods.addAll(parseHTMLModList(zipFile, entry));
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing HTML mod list: " + e.getMessage());
        }
        
        return mods;
    }
    
    /**
     * Parse HTML file for mod information
     */
    private List<ModInfo> parseHTMLModList(ZipFile zipFile, ZipEntry htmlEntry) {
        List<ModInfo> mods = new ArrayList<>();
        
        try (InputStream is = zipFile.getInputStream(htmlEntry)) {
            Document doc = Jsoup.parse(is, "UTF-8", "");
            
            // Look for CurseForge links
            Elements links = doc.select("a[href*=\"curseforge.com/minecraft/mc-mods\"]");
            for (Element link : links) {
                String href = link.attr("href");
                String modName = link.text();
                
                // Extract mod slug from URL
                String modSlug = extractModSlugFromURL(href);
                if (modSlug != null) {
                    ModInfo mod = new ModInfo();
                    mod.setName(modName);
                    mod.setSlug(modSlug);
                    mod.setPlatform("curseforge");
                    mods.add(mod);
                }
            }
            
            // Look for Modrinth links
            Elements modrinthLinks = doc.select("a[href*=\"modrinth.com/mod\"]");
            for (Element link : modrinthLinks) {
                String href = link.attr("href");
                String modName = link.text();
                
                String modSlug = extractModrinthSlugFromURL(href);
                if (modSlug != null) {
                    ModInfo mod = new ModInfo();
                    mod.setName(modName);
                    mod.setSlug(modSlug);
                    mod.setPlatform("modrinth");
                    mods.add(mod);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing HTML: " + e.getMessage());
        }
        
        return mods;
    }
    
    /**
     * Extract mod slug from CurseForge URL
     */
    private String extractModSlugFromURL(String url) {
        try {
            // URL format: https://www.curseforge.com/minecraft/mc-mods/mod-name
            String[] parts = url.split("/");
            if (parts.length >= 5) {
                return parts[parts.length - 1];
            }
        } catch (Exception e) {
            System.err.println("Error extracting mod slug: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Extract mod slug from Modrinth URL
     */
    private String extractModrinthSlugFromURL(String url) {
        try {
            // URL format: https://modrinth.com/mod/mod-name
            String[] parts = url.split("/");
            if (parts.length >= 4) {
                return parts[parts.length - 1];
            }
        } catch (Exception e) {
            System.err.println("Error extracting Modrinth slug: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Prompt user for modpack configuration
     */
    private ModpackConfig promptUserConfiguration(ModpackManifest manifest, JFrame parent) {
        ModpackConfigDialog dialog = new ModpackConfigDialog(parent, manifest);
        dialog.setVisible(true);
        
        if (dialog.isConfirmed()) {
            return dialog.getConfig();
        }
        return null;
    }
    
    /**
     * Process mods from manifest
     */
    private List<ModInfo> processManifestMods(ModpackManifest manifest, ModpackConfig config) {
        List<ModInfo> mods = new ArrayList<>();
        
        for (ModpackManifest.File file : manifest.files) {
            if (manifest.isServerSideCompatible(file)) {
                try {
                    // Fetch mod info from CurseForge
                    ModInfo mod = curseForgeClient.getModInfo(file.projectID, file.fileID);
                    if (mod != null) {
                        mod.setServerSide(manifest.isServerRequired(file));
                        mods.add(mod);
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching mod info for project " + file.projectID + ": " + e.getMessage());
                }
            }
        }
        
        return mods;
    }
    
    /**
     * Process mods from HTML list
     */
    private List<ModInfo> processHTMLMods(List<ModInfo> htmlMods, ModpackConfig config) {
        List<ModInfo> processedMods = new ArrayList<>();
        
        for (ModInfo mod : htmlMods) {
            try {
                // Fetch detailed mod info
                ModInfo detailedMod = null;
                if ("curseforge".equals(mod.getPlatform())) {
                    detailedMod = curseForgeClient.searchModBySlug(mod.getSlug(), config.getMinecraftVersion());
                }
                
                if (detailedMod != null) {
                    processedMods.add(detailedMod);
                }
            } catch (Exception e) {
                System.err.println("Error processing mod " + mod.getName() + ": " + e.getMessage());
            }
        }
        
        return processedMods;
    }
    
    /**
     * Filter mods to only include server-side compatible ones
     */
    private List<ModInfo> filterServerSideMods(List<ModInfo> mods) {
        return mods.stream()
            .filter(mod -> mod.isServerSide() || mod.isServerCompatible())
            .collect(Collectors.toList());
    }
    
    /**
     * Result of modpack import
     */
    public static class ModpackImportResult {
        private final ModpackConfig config;
        private final List<ModInfo> mods;
        private final boolean hasManifest;
        
        public ModpackImportResult(ModpackConfig config, List<ModInfo> mods, boolean hasManifest) {
            this.config = config;
            this.mods = mods;
            this.hasManifest = hasManifest;
        }
        
        public ModpackConfig getConfig() { return config; }
        public List<ModInfo> getMods() { return mods; }
        public boolean hasManifest() { return hasManifest; }
    }
}
