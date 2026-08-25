package com.zerog.stellarserverforge.zerogmods;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One entry in the externally-supplied catalog of mods the ZeroG Network org has made. The
 * catalog itself is just a JSON array of these, hosted wherever the user maintains it (e.g. a
 * raw GitHub URL into their org's repo) — this app never scans GitHub itself to discover mods.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZeroGModEntry {

    /** Where the actual installable file lives — ZeroG Network only authors the mod, it doesn't host files. */
    public enum Source { MODRINTH, CURSEFORGE }

    private String name;
    private String description;
    private Source source;
    /** Modrinth project slug/ID, or CurseForge numeric mod ID, depending on {@link #source}. */
    private String projectId;
    /** Optional direct link to the mod's project page, used for "Open page" and as a browse link. */
    private String pageUrl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    @Override
    public String toString() {
        return name == null ? "(unnamed mod)" : name;
    }
}
