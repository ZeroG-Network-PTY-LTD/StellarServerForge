package com.zerog.stellarserverforge.net_port;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Native Java UPnP port forwarding via {@code weupnp}, replacing the bat script's bundled
 * {@code Portforwarded.Server.exe} .NET dependency (spec §9.2, §14.4 recommendation).
 */
public class UpnpService {

    public GatewayDevice discoverGateway() throws IOException {
        try {
            GatewayDiscover discover = new GatewayDiscover();
            discover.discover();
            return discover.getValidGateway();
        } catch (Exception e) {
            throw new IOException("UPnP gateway discovery failed: " + e.getMessage(), e);
        }
    }

    public boolean addMapping(GatewayDevice gateway, String protocol, int externalPort, int internalPort, String localIp,
                               String description) throws IOException {
        try {
            return gateway.addPortMapping(externalPort, internalPort, localIp, protocol, description);
        } catch (Exception e) {
            throw new IOException("Failed to add UPnP port mapping: " + e.getMessage(), e);
        }
    }

    public void removeMapping(GatewayDevice gateway, String protocol, int externalPort) throws IOException {
        try {
            gateway.deletePortMapping(externalPort, protocol);
        } catch (Exception e) {
            throw new IOException("Failed to remove UPnP port mapping: " + e.getMessage(), e);
        }
    }

    public boolean hasMapping(GatewayDevice gateway, String protocol, int externalPort) throws IOException {
        try {
            PortMappingEntry entry = new PortMappingEntry();
            return gateway.getSpecificPortMappingEntry(externalPort, protocol, entry);
        } catch (Exception e) {
            throw new IOException("Failed to query UPnP port mapping: " + e.getMessage(), e);
        }
    }

    public InetAddress localAddress(GatewayDevice gateway) {
        return gateway.getLocalAddress();
    }
}
