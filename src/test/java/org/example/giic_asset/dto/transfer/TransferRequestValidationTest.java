package org.example.giic_asset.dto.transfer;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class TransferRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsBlankTransferLocation() {
        TransferRequest request = requestWithLocation("   ");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("transferLocation"));
    }

    @Test
    void acceptsTransferLocation() {
        TransferRequest request = requestWithLocation("연구교수실 1");

        assertThat(validator.validate(request)).isEmpty();
    }

    private TransferRequest requestWithLocation(String transferLocation) {
        TransferRequest request = new TransferRequest();
        ReflectionTestUtils.setField(request, "assetId", 1L);
        ReflectionTestUtils.setField(request, "transferLocation", transferLocation);
        return request;
    }
}
