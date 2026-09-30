package org.example.giic_asset.repository;

import org.example.giic_asset.entity.Asset;
import org.example.giic_asset.entity.Transfer;
import org.example.giic_asset.entity.User;
import org.example.giic_asset.entity.enums.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Long>, JpaSpecificationExecutor<Transfer> {
    Page<Transfer> findByFromUserEmployeeNo(String fromUserId, Pageable pageable);

    Page<Transfer> findByFromUserEmployeeNoAndStatus(String fromUserId, TransferStatus status, Pageable pageable);

    Page<Transfer> findByRecipientName(String recipientName, Pageable pageable);

    Page<Transfer> findByRecipientNameAndStatus(String recipientName, TransferStatus status, Pageable pageable);

    void deleteByAssetId(Long assetId);

    // 본인 이관 신청 목록 조회 (USER용)
    List<Transfer> findByRequestedByOrderByCreatedAtDesc(User requestedBy);

    // 자산별 이관 목록 조회
    List<Transfer> findByAssetOrderByCreatedAtDesc(Asset asset);

    // 상태별 조회
    List<Transfer> findByStatusOrderByCreatedAtDesc(TransferStatus status);

    List<Transfer> findAllByOrderByCreatedAtDesc();
}
