package org.example.giic_asset.event;

import java.time.LocalDateTime;

public record SignupApprovedEvent(
        String employeeNo,
        String name,
        String department,
        String email,
        LocalDateTime processedAt) {
}
