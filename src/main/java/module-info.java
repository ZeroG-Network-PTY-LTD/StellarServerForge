module com.zerog.stellarserverforge {
    // Core Java modules
    requires java.desktop;
    requires java.sql;
    requires java.prefs;
    requires java.management;
    
    // UI Framework
    requires com.formdev.flatlaf;
    
    // HTTP Client
    requires okhttp3;
    
    // JSON Processing
    requires com.google.gson;
    
    // Apache Commons
    requires org.apache.commons.lang3;
    requires org.apache.commons.io;
    
    // Logging
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    
    // Crypto (for KeyVault)
    requires java.base;

    // Export packages
    exports com.zerog.network.stellarforge;
    exports com.zerog.network.stellarforge.gui;
    exports com.zerog.network.stellarforge.model;
    exports com.zerog.network.stellarforge.config;
    exports com.zerog.network.stellarforge.security;
    exports com.zerog.network.stellarforge.api;
    exports com.zerog.network.stellarforge.utils;
}