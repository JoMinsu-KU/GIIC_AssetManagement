package org.example.giic_asset.repository;

import org.example.giic_asset.entity.UserLoginEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserLoginEventRepository extends JpaRepository<UserLoginEvent, Long> {
    List<UserLoginEvent> findTop50ByOrderByLoggedInAtDesc();
}
