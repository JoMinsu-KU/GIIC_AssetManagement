package org.example.giic_asset.dto.user;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SignupRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsBlankEmailBecauseApprovalResultUsesIt() {
        SignupRequest request = validRequest();
        ReflectionTestUtils.setField(request, "email", "   ");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("email"));
    }

    @Test
    void acceptsValidSignupEmail() {
        assertThat(validator.validate(validRequest())).isEmpty();
    }

    @Test
    void rejectsMalformedEmail() {
        SignupRequest request = validRequest();
        ReflectionTestUtils.setField(request, "email", "invalid-email");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("email"));
    }

    @Test
    void rejectsInvalidEmployeeNumber() {
        SignupRequest request = validRequest();
        ReflectionTestUtils.setField(request, "employeeNo", ".");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("employeeNo"));
    }

    @Test
    void rejectsWeakPassword() {
        SignupRequest request = validRequest();
        ReflectionTestUtils.setField(request, "password", ".");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
    }

    @Test
    void rejectsInvalidName() {
        SignupRequest request = validRequest();
        ReflectionTestUtils.setField(request, "name", ".");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("name"));
    }

    @Test
    void rejectsPunctuationOnlyDepartment() {
        SignupRequest request = validRequest();
        ReflectionTestUtils.setField(request, "department", ".");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("department"));
    }

    private SignupRequest validRequest() {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "employeeNo", "2026001");
        ReflectionTestUtils.setField(request, "password", "secure-password1");
        ReflectionTestUtils.setField(request, "name", "Test User");
        ReflectionTestUtils.setField(request, "department", "GIIC");
        ReflectionTestUtils.setField(request, "email", "user@example.com");
        return request;
    }
}
