package org.example.giic_asset.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.giic_asset.entity.enums.Role;
import org.example.giic_asset.entity.enums.UserStatus;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @Column(name = "employee_no", nullable = false, length = 20)
    private String employeeNo;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    private String department;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    public void updateStatusAndRole(UserStatus status, Role role) {
        if (status != null)
            this.status = status;
        if (role != null)
            this.role = role;
    }
}
