package org.example.giic_asset.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.Role;
import org.example.giic_asset.entity.enums.UserStatus;

@Getter
@NoArgsConstructor
public class SignupRequest {
    @NotBlank(message = "사번은 필수입니다.")
    @Pattern(regexp = "^[0-9]{7,8}$", message = "사번은 숫자 7~8자리여야 합니다.")
    private String employeeNo;
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*[0-9])\\S+$",
            message = "비밀번호에는 공백 없이 영문과 숫자가 각각 하나 이상 포함되어야 합니다.")
    private String password;
    @NotBlank(message = "이름은 필수입니다.")
    @Size(min = 2, max = 50, message = "이름은 2~50자여야 합니다.")
    @Pattern(
            regexp = "^[가-힣A-Za-z][가-힣A-Za-z .·'-]*$",
            message = "이름 형식이 올바르지 않습니다.")
    private String name;
    @Size(max = 50, message = "부서는 50자 이하여야 합니다.")
    @Pattern(
            regexp = "^(?:|(?=.*[가-힣A-Za-z0-9])[가-힣A-Za-z0-9 ()（）·._&/,-]+)$",
            message = "부서 형식이 올바르지 않습니다.")
    private String department;
    @NotBlank(message = "이메일은 필수입니다.")
    @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
    @Email(message = "올바른 이메일 형식이어야 합니다.")
    private String email;

    public User toEntity(String encodedPassword) {
        return User.builder()
                .employeeNo(employeeNo)
                .password(encodedPassword)
                .name(name)
                .department(department)
                .email(email)
                .role(Role.USER) // Default role
                .status(UserStatus.PENDING) // Default status
                .build();
    }
}
