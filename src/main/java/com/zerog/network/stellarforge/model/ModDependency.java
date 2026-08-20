package com.zerog.network.stellarforge.model;

/**
 * Represents a dependency relationship between mods.
 */
public class ModDependency {

    public enum DependencyType {
        REQUIRED("Required"),
        OPTIONAL("Optional"),
        INCOMPATIBLE("Incompatible"),
        EMBEDDED("Embedded");

        private final String displayName;
        DependencyType(String n) { this.displayName = n; }
        public String getDisplayName() { return displayName; }
    }

    private final String sourceProjectId;   // The mod that has this dependency
    private final String targetProjectId;   // The dependency itself
    private final String targetName;        // Human-readable name
    private final DependencyType type;
    private final String requiredVersion;   // null = any version

    public ModDependency(String sourceProjectId, String targetProjectId,
                         String targetName, DependencyType type, String requiredVersion) {
        this.sourceProjectId  = sourceProjectId;
        this.targetProjectId  = targetProjectId;
        this.targetName       = targetName;
        this.type             = type;
        this.requiredVersion  = requiredVersion;
    }

    // ── Factory helpers ────────────────────────────────────────────────────────

    public static ModDependency required(String source, String target, String name) {
        return new ModDependency(source, target, name, DependencyType.REQUIRED, null);
    }

    public static ModDependency optional(String source, String target, String name) {
        return new ModDependency(source, target, name, DependencyType.OPTIONAL, null);
    }

    public static ModDependency incompatible(String source, String target, String name) {
        return new ModDependency(source, target, name, DependencyType.INCOMPATIBLE, null);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String getSourceProjectId()  { return sourceProjectId; }
    public String getTargetProjectId()  { return targetProjectId; }
    public String getTargetName()       { return targetName; }
    public DependencyType getType()     { return type; }
    public String getRequiredVersion()  { return requiredVersion; }

    public boolean isRequired()      { return type == DependencyType.REQUIRED; }
    public boolean isOptional()      { return type == DependencyType.OPTIONAL; }
    public boolean isIncompatible()  { return type == DependencyType.INCOMPATIBLE; }

    @Override
    public String toString() {
        return type.getDisplayName() + ": " + targetName
                + (requiredVersion != null ? " @" + requiredVersion : "");
    }
}

