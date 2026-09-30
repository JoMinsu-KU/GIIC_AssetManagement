package org.example.giic_asset.repository;

import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.Role;
import org.example.giic_asset.entity.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmployeeNo(String employeeNo);

    boolean existsByEmployeeNo(String employeeNo);

    List<User> findByStatus(UserStatus status);

    List<User> findByRole(Role role);

    List<User> findByRoleAndStatus(Role role, UserStatus status);

    List<User> findAllByOrderByCreatedAtDesc();
}
