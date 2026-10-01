package org.example.giic_asset.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "user_login_events", indexes = @Index(name = "idx_user_login_events_at", columnList = "logged_in_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserLoginEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_no", nullable = false, length = 20)
    private String employeeNo;

    @Column(name = "logged_in_at", nullable = false)
    private LocalDateTime loggedInAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    public UserLoginEvent(String employeeNo, LocalDateTime loggedInAt, String ipAddress) {
        this.employeeNo = employeeNo;
        this.loggedInAt = loggedInAt;
        this.ipAddress = ipAddress;
    }
}
