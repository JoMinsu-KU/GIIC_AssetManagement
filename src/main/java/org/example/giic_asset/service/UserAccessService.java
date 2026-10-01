package org.example.giic_asset.service;

import lombok.RequiredArgsConstructor;
import org.example.giic_asset.dto.system.AccessDashboardResponse;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.UserAccess;
import org.example.giic_asset.entity.UserLoginEvent;
import org.example.giic_asset.entity.enums.UserStatus;
import org.example.giic_asset.repository.UserAccessRepository;
import org.example.giic_asset.repository.UserLoginEventRepository;
import org.example.giic_asset.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAccessService {
    private static final long ACTIVITY_INTERVAL_MS = 30_000;
    private final UserAccessRepository accessRepository;
    private final UserLoginEventRepository loginEventRepository;
    private final UserRepository userRepository;
    private final Map<String, Long> lastActivityWrites = new ConcurrentHashMap<>();

    @Transactional
    public void recordLogin(String employeeNo, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        accessRepository.upsertLogin(employeeNo, now, ipAddress);
        loginEventRepository.save(new UserLoginEvent(employeeNo, now, ipAddress));
        lastActivityWrites.put(employeeNo, System.currentTimeMillis());
    }

    @Transactional
    public void recordActivity(String employeeNo, String ipAddress) {
        long now = System.currentTimeMillis();
        Long previous = lastActivityWrites.get(employeeNo);
        if (previous != null && now - previous < ACTIVITY_INTERVAL_MS) return;
        accessRepository.upsertActivity(employeeNo, LocalDateTime.now(), ipAddress);
        lastActivityWrites.put(employeeNo, now);
    }

    @Transactional(readOnly = true)
    public AccessDashboardResponse getDashboard() {
        Map<String, UserAccess> accessByEmployee = accessRepository.findAll().stream()
                .collect(Collectors.toMap(UserAccess::getEmployeeNo, access -> access));
        List<User> users = userRepository.findAll();
        Map<String, String> names = users.stream()
                .collect(Collectors.toMap(User::getEmployeeNo, User::getName));
        LocalDateTime onlineSince = LocalDateTime.now().minusMinutes(2);
        List<AccessDashboardResponse.UserAccessRow> rows = users.stream().map(user -> {
            UserAccess access = accessByEmployee.get(user.getEmployeeNo());
            LocalDateTime lastActivity = access == null ? null : access.getLastActivityAt();
            boolean online = user.getStatus() == UserStatus.APPROVED && lastActivity != null
                    && !lastActivity.isBefore(onlineSince);
            return new AccessDashboardResponse.UserAccessRow(user.getEmployeeNo(), user.getName(),
                    user.getDepartment(), user.getRole(), user.getStatus(),
                    access == null ? null : access.getLastLoginAt(), lastActivity,
                    access == null ? null : displayIp(access.getLastLoginIp()),
                    access == null ? null : displayIp(access.getLastActivityIp()),
                    access == null ? 0 : access.getLoginCount(), online);
        }).sorted(Comparator.comparing(AccessDashboardResponse.UserAccessRow::lastActivityAt,
                Comparator.nullsLast(Comparator.reverseOrder()))).toList();
        List<AccessDashboardResponse.LoginEventRow> recentLogins = loginEventRepository
                .findTop50ByOrderByLoggedInAtDesc().stream()
                .map(event -> new AccessDashboardResponse.LoginEventRow(event.getEmployeeNo(),
                        names.getOrDefault(event.getEmployeeNo(), event.getEmployeeNo()),
                        event.getLoggedInAt(), displayIp(event.getIpAddress())))
                .toList();
        return new AccessDashboardResponse(rows.size(), rows.stream().filter(
                AccessDashboardResponse.UserAccessRow::online).count(), rows, recentLogins);
    }

    private String displayIp(String ip) {
        return "172.18.0.1".equals(ip) ? null : ip;
    }
}
