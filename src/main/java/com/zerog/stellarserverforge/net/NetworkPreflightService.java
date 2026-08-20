package com.zerog.stellarserverforge.net;

import com.zerog.stellarserverforge.model.ModLoader;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * DNS resolution preflight for modloader maven hosts and the Mojang metadata hosts, replacing
 * the bat script's {@code Resolve-DnsName}/{@code ping} preflight (spec §6.1, §14.4). Uses
 * {@link InetAddress} resolution rather than shelling out to {@code ping}, per the spec's own
 * recommendation that ICMP ping is unreliable across networks/firewalls.
 */
public class NetworkPreflightService {

    public record PreflightResult(boolean ok, java.util.List<String> unresolvedHosts) {
    }

    public static String mavenHostFor(ModLoader loader) {
        return switch (loader) {
            case FORGE -> "maven.minecraftforge.net";
            case FABRIC -> "maven.fabricmc.net";
            case QUILT -> "maven.quiltmc.org";
            case NEOFORGE -> "maven.neoforged.net";
            case VANILLA -> null;
        };
    }

    /** Resolves the modloader's maven host plus both Mojang metadata hosts. */
    public PreflightResult checkModLoaderAndMojangHosts(ModLoader loader) {
        java.util.List<String> hosts = new java.util.ArrayList<>();
        String mavenHost = mavenHostFor(loader);
        if (mavenHost != null) {
            hosts.add(mavenHost);
        }
        hosts.add("launchermeta.mojang.com");
        hosts.add("piston-meta.mojang.com");

        java.util.List<String> unresolved = new java.util.ArrayList<>();
        for (String host : hosts) {
            if (!resolves(host)) {
                unresolved.add(host);
            }
        }
        return new PreflightResult(unresolved.isEmpty(), unresolved);
    }

    public boolean resolves(String host) {
        try {
            InetAddress.getAllByName(host);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
