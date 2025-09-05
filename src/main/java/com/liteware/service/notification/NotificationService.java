package com.liteware.service.notification;

import com.liteware.model.entity.notification.Notification;
import com.liteware.model.entity.notification.Notification.NotificationPriority;
import com.liteware.model.entity.notification.Notification.NotificationType;
import com.liteware.model.entity.User;
import com.liteware.repository.NotificationRepository;
import com.liteware.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    
    /**
     * 알림 생성
     */
    @Transactional
    public Notification createNotification(Long userId, NotificationType type, String title, String message) {
        User recipient = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .priority(NotificationPriority.NORMAL)
                .build();
        
        return notificationRepository.save(notification);
    }
    
    /**
     * 상세 알림 생성
     */
    @Transactional
    public Notification createDetailedNotification(NotificationDto dto) {
        User recipient = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + dto.getUserId()));
        
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(dto.getType())
                .title(dto.getTitle())
                .message(dto.getMessage())
                .relatedEntityType(dto.getRelatedEntityType())
                .relatedEntityId(dto.getRelatedEntityId())
                .actionUrl(dto.getActionUrl())
                .priority(dto.getPriority() != null ? dto.getPriority() : NotificationPriority.NORMAL)
                .expiredAt(dto.getExpiredAt())
                .build();
        
        Notification saved = notificationRepository.save(notification);
        
        // 실시간 알림 전송 (WebSocket, SSE 등)
        sendRealTimeNotification(saved);
        
        return saved;
    }
    
    /**
     * 사용자의 알림 목록 조회
     */
    public Page<Notification> getUserNotifications(Long userId, Boolean isRead, Pageable pageable) {
        if (isRead != null) {
            return notificationRepository.findByRecipientUserIdAndIsReadAndIsDeleted(userId, isRead, false, pageable);
        }
        return notificationRepository.findByRecipientUserIdAndIsDeleted(userId, false, pageable);
    }
    
    /**
     * 읽지 않은 알림 개수 조회
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientUserIdAndIsReadAndIsDeleted(userId, false, false);
    }
    
    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        
        notification.markAsRead();
        notificationRepository.save(notification);
    }
    
    /**
     * 여러 알림 읽음 처리
     */
    @Transactional
    public void markAsReadBatch(List<Long> notificationIds) {
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        notifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(notifications);
    }
    
    /**
     * 사용자의 모든 알림 읽음 처리
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = 
                notificationRepository.findByRecipientUserIdAndIsReadAndIsDeleted(userId, false, false);
        
        unreadNotifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unreadNotifications);
    }
    
    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        
        notification.markAsDeleted();
        notificationRepository.save(notification);
    }
    
    /**
     * 만료된 알림 정리
     */
    @Transactional
    public void cleanupExpiredNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<Notification> expiredNotifications = 
                notificationRepository.findByExpiredAtBeforeAndIsDeleted(now, false);
        
        expiredNotifications.forEach(Notification::markAsDeleted);
        notificationRepository.saveAll(expiredNotifications);
        
        log.info("Cleaned up {} expired notifications", expiredNotifications.size());
    }
    
    /**
     * 결재 요청 알림 생성
     */
    @Transactional
    public void createApprovalRequestNotification(Long approverId, Long documentId, String documentTitle) {
        NotificationDto dto = NotificationDto.builder()
                .userId(approverId)
                .type(NotificationType.APPROVAL_REQUEST)
                .title("결재 요청")
                .message(String.format("'%s' 문서에 대한 결재 요청이 있습니다.", documentTitle))
                .relatedEntityType("approval")
                .relatedEntityId(documentId)
                .actionUrl("/approval/" + documentId)
                .priority(NotificationPriority.HIGH)
                .build();
        
        createDetailedNotification(dto);
    }
    
    /**
     * 결재 완료 알림 생성
     */
    @Transactional
    public void createApprovalCompletedNotification(Long drafterId, Long documentId, String documentTitle, boolean isApproved) {
        NotificationType type = isApproved ? NotificationType.APPROVAL_APPROVED : NotificationType.APPROVAL_REJECTED;
        String status = isApproved ? "승인" : "반려";
        
        NotificationDto dto = NotificationDto.builder()
                .userId(drafterId)
                .type(type)
                .title("결재 " + status)
                .message(String.format("'%s' 문서가 %s되었습니다.", documentTitle, status))
                .relatedEntityType("approval")
                .relatedEntityId(documentId)
                .actionUrl("/approval/" + documentId)
                .priority(NotificationPriority.NORMAL)
                .build();
        
        createDetailedNotification(dto);
    }
    
    /**
     * 게시글 댓글 알림 생성
     */
    @Transactional
    public void createCommentNotification(Long postWriterId, Long postId, String postTitle, String commenterName) {
        NotificationDto dto = NotificationDto.builder()
                .userId(postWriterId)
                .type(NotificationType.BOARD_COMMENT)
                .title("새 댓글")
                .message(String.format("%s님이 '%s' 게시글에 댓글을 남겼습니다.", commenterName, postTitle))
                .relatedEntityType("post")
                .relatedEntityId(postId)
                .actionUrl("/board/post/" + postId)
                .priority(NotificationPriority.LOW)
                .build();
        
        createDetailedNotification(dto);
    }
    
    /**
     * 실시간 알림 전송 (WebSocket/SSE 구현 필요)
     */
    private void sendRealTimeNotification(Notification notification) {
        // TODO: WebSocket 또는 SSE를 통한 실시간 알림 전송
        log.debug("Sending real-time notification to user: {}", notification.getRecipient().getUserId());
    }
    
    // 알림 DTO
    @lombok.Getter
    @lombok.Setter
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NotificationDto {
        private Long userId;
        private NotificationType type;
        private String title;
        private String message;
        private String relatedEntityType;
        private Long relatedEntityId;
        private String actionUrl;
        private NotificationPriority priority;
        private LocalDateTime expiredAt;
    }
}