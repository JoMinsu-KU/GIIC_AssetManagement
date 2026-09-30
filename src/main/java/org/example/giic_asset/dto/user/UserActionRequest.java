package org.example.giic_asset.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserActionRequest {

    @NotBlank(message = "대상 사용자 사번은 필수입니다.")
    private String employeeNo;
}
