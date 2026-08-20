package com.zerog.stellarserverforge.gui;

import com.zerog.stellarserverforge.javamanaged.JavaProvisioningService;
import com.zerog.stellarserverforge.modloader.FabricQuiltInstaller;
import com.zerog.stellarserverforge.modloader.ForgeNeoForgeInstaller;
import com.zerog.stellarserverforge.modloader.ModLoaderMetadataService;
import com.zerog.stellarserverforge.modloader.ModLoaderVersionResolver;
import com.zerog.stellarserverforge.mojang.MojangManifestService;
import com.zerog.stellarserverforge.mojang.VanillaInstallService;
import com.zerog.stellarserverforge.net_port.PortConflictService;
import com.zerog.stellarserverforge.settings.EulaService;
import com.zerog.stellarserverforge.settings.ServerPropertiesService;
import com.zerog.stellarserverforge.settings.SettingsService;

import java.nio.file.Path;

/** Wires together the per-server-directory services shared across GUI panels. */
public class AppContext {

    public final Path serverDir;
    public final Path cacheDir;

    public final SettingsService settingsService;
    public final MojangManifestService mojangManifestService;
    public final VanillaInstallService vanillaInstallService;
    public final JavaProvisioningService javaProvisioningService;
    public final ServerPropertiesService serverPropertiesService;
    public final EulaService eulaService;
    public final PortConflictService portConflictService;
    public final ModLoaderMetadataService modLoaderMetadataService;
    public final ModLoaderVersionResolver modLoaderVersionResolver;
    public final ForgeNeoForgeInstaller forgeNeoForgeInstaller;
    public final FabricQuiltInstaller fabricQuiltInstaller;

    public AppContext(Path serverDir) {
        this.serverDir = serverDir;
        this.cacheDir = serverDir.resolve(".stellarforge-cache");

        this.settingsService = new SettingsService(serverDir);
        this.mojangManifestService = new MojangManifestService(cacheDir);
        this.vanillaInstallService = new VanillaInstallService(cacheDir, serverDir);
        this.javaProvisioningService = new JavaProvisioningService(cacheDir);
        this.serverPropertiesService = new ServerPropertiesService(serverDir);
        this.eulaService = new EulaService(serverDir);
        this.portConflictService = new PortConflictService();
        this.modLoaderMetadataService = new ModLoaderMetadataService(cacheDir);
        this.modLoaderVersionResolver = new ModLoaderVersionResolver();
        this.forgeNeoForgeInstaller = new ForgeNeoForgeInstaller(cacheDir, serverDir);
        this.fabricQuiltInstaller = new FabricQuiltInstaller(cacheDir, serverDir);
    }
}
