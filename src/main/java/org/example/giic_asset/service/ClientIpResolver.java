package org.example.giic_asset.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {
    public String resolve(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        String forwarded = request.getHeader("X-Real-IP");
        if (isDockerProxy(remote) && forwarded != null && forwarded.length() <= 45
                && forwarded.matches("[0-9a-fA-F:.]+")) {
            return isDockerGateway(forwarded, remote) ? null : forwarded;
        }
        return isDockerGateway(remote, remote) ? null : remote;
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
        if (parts.length != 4 || !"172".equals(parts[0])) return false;
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
