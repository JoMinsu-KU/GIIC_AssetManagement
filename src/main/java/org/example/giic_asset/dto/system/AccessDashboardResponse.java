package org.example.giic_asset.dto.system;

import org.example.giic_asset.entity.enums.Role;
import org.example.giic_asset.entity.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AccessDashboardResponse(long totalUsers, long activeUsers,
                                      List<UserAccessRow> users, List<LoginEventRow> recentLogins) {
    public record UserAccessRow(String employeeNo, String name, String department, Role role,
                                UserStatus status, LocalDateTime lastLoginAt, LocalDateTime lastActivityAt,
                                String lastLoginIp, String lastActivityIp, long loginCount, boolean online) {
    }

    public record LoginEventRow(String employeeNo, String name, LocalDateTime loggedInAt,
                                String ipAddress) {
    }
}
