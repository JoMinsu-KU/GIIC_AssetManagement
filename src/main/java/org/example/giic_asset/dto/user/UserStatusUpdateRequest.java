package org.example.giic_asset.dto.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.giic_asset.entity.enums.Role;
import org.example.giic_asset.entity.enums.UserStatus;

@Getter
@NoArgsConstructor
public class UserStatusUpdateRequest {
    private UserStatus status;
    private Role role;
}
