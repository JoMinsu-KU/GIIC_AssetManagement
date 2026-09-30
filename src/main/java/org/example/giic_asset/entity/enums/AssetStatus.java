package org.example.giic_asset.entity.enums;

public enum AssetStatus {
    NORMAL, // 정상/사용중
    ABNORMAL, // 비정상/고장
    UNUSED, // 미사용/미배정
    DISPOSED, // 폐기
    REPAIRING, // 수리중
    TRANSFER_PENDING, // 이관 대기중
    DISPOSAL_PENDING, // 폐기 신청됨 (승인 대기)
    DISPOSAL_REJECTED, // 폐기 거절됨 (재신청 필요)
    LOSS_PENDING, // 손망 승인 대기
    LOST // 손망
}
