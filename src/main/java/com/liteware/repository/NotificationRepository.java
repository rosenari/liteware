package com.liteware.repository;

import com.liteware.model.entity.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    Page<Notification> findByRecipientUserIdAndIsDeleted(Long userId, Boolean isDeleted, Pageable pageable);
    
    Page<Notification> findByRecipientUserIdAndIsReadAndIsDeleted(Long userId, Boolean isRead, Boolean isDeleted, Pageable pageable);
    
    List<Notification> findByRecipientUserIdAndIsReadAndIsDeleted(Long userId, Boolean isRead, Boolean isDeleted);
    
    long countByRecipientUserIdAndIsReadAndIsDeleted(Long userId, Boolean isRead, Boolean isDeleted);
    
    List<Notification> findByExpiredAtBeforeAndIsDeleted(LocalDateTime dateTime, Boolean isDeleted);
}