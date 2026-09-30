package org.example.giic_asset.event;

import java.time.LocalDateTime;

public record SignupRejectedEvent(
        String employeeNo,
        String name,
        String department,
        String email,
        LocalDateTime processedAt) {
}
