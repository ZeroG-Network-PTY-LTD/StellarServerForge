package com.zerog.stellarserverforge.model;

public enum JavaOverrideMode {
    /** Detect a matching system JDK, falling back to a Universalator-managed Adoptium download. */
    AUTOMATIC,
    /** Always use whatever "java" resolves to on the system PATH. */
    SYSTEM_PATH,
    /** Always use a Universalator-managed Adoptium download, skipping system detection entirely. */
    FORCE_MANAGED
}
