package org.example.giic_asset.repository;

import org.example.giic_asset.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientEmployeeNoOrderByCreatedAtDesc(String recipientId);

    long countByRecipientEmployeeNoAndIsReadFalse(String recipientId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Notification notification where notification.recipient.employeeNo = :recipientId")
    int deleteAllByRecipientEmployeeNo(@Param("recipientId") String recipientId);

    void deleteByRelatedEntityTypeAndRelatedEntityId(String relatedEntityType, Long relatedEntityId);
}
