# NeoForge Version Collection Improvements

## Overview
This document summarizes the improvements made to the mod loader version collection system to properly support NeoForge and other mod loaders.

## Problem Statement
The original system had hardcoded mod loader versions and did not properly collect versions for NeoForge-based servers, as reported by the user.

## Solution Implementation

### 1. Created ModLoaderVersionFetcher Utility
- **Location**: `com.zerog.network.stellarforge.util.ModLoaderVersionFetcher`
- **Purpose**: Dynamically fetch mod loader versions from official APIs
- **Features**:
  - Forge: Uses promotions API for recommended and latest versions
  - Fabric: Uses Fabric Meta API for stable versions
  - Quilt: Uses Quilt Meta API for available versions
  - NeoForge: Uses NeoForge API and Maven endpoints with proper version compatibility checking

### 2. NeoForge-Specific Improvements
- **Version Compatibility**: Implemented `isNeoForgeVersionCompatible()` method to match NeoForge versions with Minecraft versions
- **Dual API Support**: First tries NeoForge API, then falls back to Maven metadata
- **Proper Versioning**: Handles NeoForge's unique versioning scheme (e.g., `20.4.109-beta` for MC 1.20.4)
- **Fallback Versions**: Provides appropriate fallback versions when APIs are unavailable

### 3. Updated User Interfaces
- **ModpackConfigDialog**: Now uses async version fetching with loading indicators
- **MainWindow**: Converted from text field to combo box for version selection with dynamic loading
- **Async Operations**: All version fetching is done in background threads to prevent UI blocking

### 4. Configuration Updates
- **Default Values**: Changed from hardcoded versions to "Latest" to enable dynamic selection
- **ServerConfig**: Updated both universal and stellar-specific configurations

## API Endpoints Used
- **Forge**: `https://files.minecraftforge.net/net/minecraftforge/forge/promotions_{version}.json`
- **Fabric**: `https://meta.fabricmc.net/v2/versions/loader`
- **Quilt**: `https://meta.quiltmc.org/v3/versions/loader`
- **NeoForge**: `https://api.neoforged.net/versions/{version}` and `https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge`

## Version Compatibility Logic
NeoForge uses a different versioning scheme where:
- Format: `XX.Y.ZZZ` where `XX.Y` corresponds to Minecraft version
- Example: `20.4.109` is for Minecraft 1.20.4
- The system parses both NeoForge and Minecraft versions to ensure compatibility

## Testing Results
The test shows successful version fetching for:
- NeoForge 1.20.1: `47.1.104`, `47.1.103`, `47.1.100`
- NeoForge 1.20.4: `20.4.0-beta`, `20.4.1-beta`, `20.4.2-beta`, etc.
- NeoForge 1.20.6: `20.6.1-beta`, `20.6.2-beta`, `20.6.3-beta`, etc.

## Error Handling
- Graceful fallback to predefined versions when APIs are unavailable
- Proper error logging for debugging
- User-friendly loading indicators
- Timeout handling for HTTP requests

## Benefits
1. **Automatic Version Detection**: No more hardcoded versions
2. **NeoForge Support**: Proper version compatibility checking
3. **Better User Experience**: Dynamic loading with visual feedback
4. **Maintainability**: Easy to add new mod loaders or update API endpoints
5. **Reliability**: Fallback versions ensure system always works

## Files Modified
- `ModLoaderVersionFetcher.java` (new utility class)
- `ModpackConfigDialog.java` (async version loading)
- `MainWindow.java` (dynamic version selection)
- `ServerConfig.java` (default value updates)

The system now properly auto-collects mod loader versions for NeoForge and provides a much better user experience for server creation.
