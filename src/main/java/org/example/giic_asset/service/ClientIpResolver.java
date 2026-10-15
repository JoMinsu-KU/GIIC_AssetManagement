package org.example.giic_asset.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class ClientIpResolver {
    private final boolean hostProxyEnabled;

    public ClientIpResolver(@Value("${app.client-ip.host-proxy-enabled:false}") boolean hostProxyEnabled) {
        this.hostProxyEnabled = hostProxyEnabled;
    }

    public String resolve(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (isDockerProxy(remote)) {
            String forwarded = request.getHeader("X-Real-IP");
            if (!isValidIp(forwarded)) return null;
            return !hostProxyEnabled && isDockerGateway(forwarded, remote) ? null : forwarded;
        }
        return isValidIp(remote) ? remote : null;
    }

    private boolean isValidIp(String address) {
        if (address == null || address.length() > 45) return false;
        if (address.contains(":")) {
            if (!address.matches("[0-9a-fA-F:.]+")) return false;
            try {
                InetAddress.getByName(address);
                return true;
            } catch (UnknownHostException exception) {
                return false;
            }
        }
        if (!address.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) return false;
        for (String part : address.split("\\.")) {
            if (Integer.parseInt(part) > 255) return false;
        }
        return true;
    }

    private boolean isDockerGateway(String candidate, String peer) {
        String[] candidateParts = candidate.split("\\.");
        String[] peerParts = peer.split("\\.");
        return candidateParts.length == 4 && peerParts.length == 4
                && isDockerProxy(peer) && "1".equals(candidateParts[3])
                && candidateParts[0].equals(peerParts[0])
                && candidateParts[1].equals(peerParts[1])
                && candidateParts[2].equals(peerParts[2]);
    }

    private boolean isDockerProxy(String address) {
        if (address == null) return false;
        String[] parts = address.split("\\.");
        if (parts.length != 4 || !"172".equals(parts[0]) || !isValidIp(address)) return false;
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
