package org.example.giic_asset.entity.enums;

public enum UserStatus {
    PENDING, // 승인 대기
    APPROVED, // 승인됨 (기존 값 - ACTIVE와 동일하게 사용)
    REJECTED, // 거절됨
    INACTIVE // 비활성화
}
