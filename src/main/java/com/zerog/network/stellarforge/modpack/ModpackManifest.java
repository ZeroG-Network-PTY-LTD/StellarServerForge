package com.zerog.network.stellarforge.modpack;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Represents a modpack manifest file structure
 * Common format used by CurseForge and other platforms
 */
public class ModpackManifest {
    
    public static class Minecraft {
        public String version;
        public List<ModLoader> modLoaders;
    }
    
    public static class ModLoader {
        public String id;
        public boolean primary;
    }
    
    public static class File {
        public int projectID;
        public int fileID;
        public boolean required;
        @SerializedName("serverSide")
        public String serverSide; // "required", "optional", "unsupported"
        @SerializedName("clientSide")
        public String clientSide; // "required", "optional", "unsupported"
    }
    
    public String name;
    public String version;
    public String author;
    public Minecraft minecraft;
    public String manifestType;
    public int manifestVersion;
    public List<File> files;
    public String overrides;
    
    /**
     * Check if a mod file is server-side compatible
     */
    public boolean isServerSideCompatible(File file) {
        if (file.serverSide == null) {
            return true; // Default to compatible if not specified
        }
        return "required".equals(file.serverSide) || "optional".equals(file.serverSide);
    }
    
    /**
     * Check if a mod file is required for server
     */
    public boolean isServerRequired(File file) {
        return "required".equals(file.serverSide) || 
               (file.serverSide == null && file.required);
    }
}
