package org.example.giic_asset.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "user_access")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccess {
    @Id
    @Column(name = "employee_no", length = 20)
    private String employeeNo;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "last_activity_ip", length = 45)
    private String lastActivityIp;

    @Column(name = "login_count", nullable = false)
    private long loginCount;
}
