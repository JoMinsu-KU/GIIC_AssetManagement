package org.example.giic_asset.repository;

import org.example.giic_asset.entity.UserAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface UserAccessRepository extends JpaRepository<UserAccess, String> {
    @Modifying
    @Query(value = "INSERT INTO user_access (employee_no, last_login_at, last_activity_at, "
            + "last_login_ip, last_activity_ip, login_count) "
            + "VALUES (:employeeNo, :at, :at, :ip, :ip, 1) "
            + "ON DUPLICATE KEY UPDATE last_login_at = :at, last_activity_at = :at, "
            + "last_login_ip = :ip, last_activity_ip = :ip, login_count = login_count + 1",
            nativeQuery = true)
    void upsertLogin(@Param("employeeNo") String employeeNo, @Param("at") LocalDateTime at,
                     @Param("ip") String ip);

    @Modifying
    @Query(value = "INSERT INTO user_access (employee_no, last_activity_at, last_activity_ip, login_count) "
            + "VALUES (:employeeNo, :at, :ip, 0) "
            + "ON DUPLICATE KEY UPDATE last_activity_at = :at, last_activity_ip = :ip",
            nativeQuery = true)
    void upsertActivity(@Param("employeeNo") String employeeNo, @Param("at") LocalDateTime at,
                        @Param("ip") String ip);
}
