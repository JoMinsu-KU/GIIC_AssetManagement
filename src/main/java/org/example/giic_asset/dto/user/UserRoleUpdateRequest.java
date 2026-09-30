package org.example.giic_asset.dto.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.giic_asset.entity.enums.Role;

@Getter
@NoArgsConstructor
public class UserRoleUpdateRequest {
    private Role role;
}
