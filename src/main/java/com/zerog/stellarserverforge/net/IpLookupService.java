package com.zerog.stellarserverforge.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Detects the public IP (for display and CGNAT detection) and local LAN IPv4, replacing the bat
 * script's ip-api.com/ipify HTTP calls and {@code ipconfig} shell-out (spec: {@code check_ips}).
 */
public class IpLookupService {

    private final HttpFetcher http = new HttpFetcher();
    private final ObjectMapper mapper = new ObjectMapper();

    public String fetchPublicIp() {
        try {
            String body = http.getString("http://ip-api.com/json/?fields=query");
            JsonNode node = mapper.readTree(body);
            String ip = node.path("query").asText();
            if (!ip.isBlank()) {
                return ip;
            }
        } catch (Exception ignored) {
            // Fall through to the fallback.
        }
        try {
            String body = http.getString("https://api.ipify.org?format=json");
            JsonNode node = mapper.readTree(body);
            return node.path("ip").asText();
        } catch (Exception e) {
            return null;
        }
    }

    /** Whether the given public IP is in the CGNAT range (100.64.0.0/10), where UPnP cannot work. */
    public static boolean isCgnat(String publicIp) {
        if (publicIp == null) {
            return false;
        }
        String[] parts = publicIp.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            return first == 100 && second >= 64 && second <= 127;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String findLocalIpv4() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {
            // Fall through to null.
        }
        return null;
    }
}
