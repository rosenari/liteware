package com.liteware.service.notification;

import com.liteware.model.entity.User;
import com.liteware.model.entity.notification.Notification;
import com.liteware.model.entity.notification.Notification.NotificationPriority;
import com.liteware.model.entity.notification.Notification.NotificationType;
import com.liteware.repository.NotificationRepository;
import com.liteware.service.BaseServiceTest;
import com.liteware.service.notification.NotificationService.NotificationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationServiceTest extends BaseServiceTest {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    private User recipient;
    
    @BeforeEach
    void setUp() {
        recipient = createUser("recipient", "수신자", "recipient@example.com", department, position);
        recipient.addRole(userRole);
        userRepository.save(recipient);
    }
    
    @Test
    @DisplayName("알림 생성 성공")
    void createNotification_Success() {
        // when
        Notification notification = notificationService.createNotification(
                recipient.getUserId(),
                NotificationType.SYSTEM_NOTICE,
                "테스트 알림",
                "테스트 메시지입니다"
        );
        
        // then
        assertThat(notification).isNotNull();
        assertThat(notification.getRecipient().getUserId()).isEqualTo(recipient.getUserId());
        assertThat(notification.getType()).isEqualTo(NotificationType.SYSTEM_NOTICE);
        assertThat(notification.getTitle()).isEqualTo("테스트 알림");
        assertThat(notification.getMessage()).isEqualTo("테스트 메시지입니다");
        assertThat(notification.getPriority()).isEqualTo(NotificationPriority.NORMAL);
        assertThat(notification.getIsRead()).isFalse();
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자에게 알림 생성 시 예외 발생")
    void createNotification_UserNotFound_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> notificationService.createNotification(
                999999L,
                NotificationType.SYSTEM_NOTICE,
                "테스트 알림",
                "테스트 메시지"
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
    
    @Test
    @DisplayName("상세 알림 생성 성공")
    void createDetailedNotification_Success() {
        // given
        NotificationDto dto = NotificationDto.builder()
                .userId(recipient.getUserId())
                .type(NotificationType.APPROVAL_REQUEST)
                .title("결재 요청")
                .message("문서 결재 요청")
                .relatedEntityType("approval")
                .relatedEntityId(123L)
                .actionUrl("/approval/123")
                .priority(NotificationPriority.HIGH)
                .expiredAt(LocalDateTime.now().plusDays(7))
                .build();
        
        // when
        Notification notification = notificationService.createDetailedNotification(dto);
        
        // then
        assertThat(notification).isNotNull();
        assertThat(notification.getRelatedEntityType()).isEqualTo("approval");
        assertThat(notification.getRelatedEntityId()).isEqualTo(123L);
        assertThat(notification.getActionUrl()).isEqualTo("/approval/123");
        assertThat(notification.getPriority()).isEqualTo(NotificationPriority.HIGH);
        assertThat(notification.getExpiredAt()).isNotNull();
    }
    
    @Test
    @DisplayName("사용자 알림 목록 조회")
    void getUserNotifications_Success() {
        // given
        createTestNotification(recipient, NotificationType.SYSTEM_NOTICE, "알림1", false);
        createTestNotification(recipient, NotificationType.APPROVAL_REQUEST, "알림2", false);
        createTestNotification(recipient, NotificationType.BOARD_COMMENT, "알림3", true);
        
        // when - 전체 조회
        Page<Notification> allNotifications = notificationService.getUserNotifications(
                recipient.getUserId(), null, PageRequest.of(0, 10)
        );
        
        // then
        assertThat(allNotifications.getTotalElements()).isEqualTo(3);
        
        // when - 읽지 않은 알림만 조회
        Page<Notification> unreadNotifications = notificationService.getUserNotifications(
                recipient.getUserId(), false, PageRequest.of(0, 10)
        );
        
        // then
        assertThat(unreadNotifications.getTotalElements()).isEqualTo(2);
        
        // when - 읽은 알림만 조회
        Page<Notification> readNotifications = notificationService.getUserNotifications(
                recipient.getUserId(), true, PageRequest.of(0, 10)
        );
        
        // then
        assertThat(readNotifications.getTotalElements()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("읽지 않은 알림 개수 조회")
    void getUnreadCount_Success() {
        // given
        createTestNotification(recipient, NotificationType.SYSTEM_NOTICE, "알림1", false);
        createTestNotification(recipient, NotificationType.APPROVAL_REQUEST, "알림2", false);
        createTestNotification(recipient, NotificationType.BOARD_COMMENT, "알림3", true);
        
        // when
        long unreadCount = notificationService.getUnreadCount(recipient.getUserId());
        
        // then
        assertThat(unreadCount).isEqualTo(2);
    }
    
    @Test
    @DisplayName("알림 읽음 처리")
    void markAsRead_Success() {
        // given
        Notification notification = createTestNotification(recipient, NotificationType.SYSTEM_NOTICE, "테스트", false);
        assertThat(notification.getIsRead()).isFalse();
        
        // when
        notificationService.markAsRead(notification.getNotificationId());
        
        // then
        Notification updated = notificationRepository.findById(notification.getNotificationId()).orElseThrow();
        assertThat(updated.getIsRead()).isTrue();
        assertThat(updated.getReadAt()).isNotNull();
    }
    
    @Test
    @DisplayName("여러 알림 일괄 읽음 처리")
    void markAsReadBatch_Success() {
        // given
        Notification n1 = createTestNotification(recipient, NotificationType.SYSTEM_NOTICE, "알림1", false);
        Notification n2 = createTestNotification(recipient, NotificationType.SYSTEM_NOTICE, "알림2", false);
        Notification n3 = createTestNotification(recipient, NotificationType.SYSTEM_NOTICE, "알림3", false);
        
        List<Long> ids = Arrays.asList(n1.getNotificationId(), n2.getNotificationId());
        
        // when
        notificationService.markAsReadBatch(ids);
        
        // then
        assertThat(notificationRepository.findById(n1.getNotificationId()).orElseThrow().getIsRead()).isTrue();
        assertThat(notificationRepository.findById(n2.getNotificationId()).orElseThrow().getIsRead()).isTrue();
        assertThat(notificationRepository.findById(n3.getNotificationId()).orElseThrow().getIsRead()).isFalse();
    }
    
    @Test
    @DisplayName("사용자의 모든 알림 읽음 처리")
    void markAllAsRead_Success() {
        // given
        createTestNotification(recipient, NotificationType.SYSTEM_NOTICE, "알림1", false);
        createTestNotification(recipient, NotificationType.APPROVAL_REQUEST, "알림2", false);
        createTestNotification(recipient, NotificationType.BOARD_COMMENT, "알림3", false);
        
        // when
        notificationService.markAllAsRead(recipient.getUserId());
        
        // then
        long unreadCount = notificationService.getUnreadCount(recipient.getUserId());
        assertThat(unreadCount).isEqualTo(0);
    }
    
    @Test
    @DisplayName("알림 삭제")
    void deleteNotification_Success() {
        // given
        Notification notification = createTestNotification(recipient, NotificationType.SYSTEM_NOTICE, "테스트", false);
        
        // when
        notificationService.deleteNotification(notification.getNotificationId());
        
        // then
        Notification deleted = notificationRepository.findById(notification.getNotificationId()).orElseThrow();
        assertThat(deleted.getIsDeleted()).isTrue();
    }
    
    @Test
    @DisplayName("만료된 알림 정리")
    void cleanupExpiredNotifications_Success() {
        // given
        // 만료된 알림
        Notification expired = Notification.builder()
                .recipient(recipient)
                .type(NotificationType.SYSTEM_NOTICE)
                .title("만료된 알림")
                .message("테스트")
                .priority(NotificationPriority.NORMAL)
                .expiredAt(LocalDateTime.now().minusDays(1))
                .build();
        notificationRepository.save(expired);
        
        // 아직 만료되지 않은 알림
        Notification valid = Notification.builder()
                .recipient(recipient)
                .type(NotificationType.SYSTEM_NOTICE)
                .title("유효한 알림")
                .message("테스트")
                .priority(NotificationPriority.NORMAL)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();
        notificationRepository.save(valid);
        
        // when
        notificationService.cleanupExpiredNotifications();
        
        // then
        Notification expiredCheck = notificationRepository.findById(expired.getNotificationId()).orElseThrow();
        Notification validCheck = notificationRepository.findById(valid.getNotificationId()).orElseThrow();
        
        assertThat(expiredCheck.getIsDeleted()).isTrue();
        assertThat(validCheck.getIsDeleted()).isFalse();
    }
    
    @Test
    @DisplayName("결재 요청 알림 생성")
    void createApprovalRequestNotification_Success() {
        // when
        notificationService.createApprovalRequestNotification(
                recipient.getUserId(),
                100L,
                "연차 신청서"
        );
        
        // then
        Page<Notification> notifications = notificationService.getUserNotifications(
                recipient.getUserId(), null, PageRequest.of(0, 10)
        );
        
        assertThat(notifications.getTotalElements()).isEqualTo(1);
        Notification notification = notifications.getContent().get(0);
        assertThat(notification.getType()).isEqualTo(NotificationType.APPROVAL_REQUEST);
        assertThat(notification.getTitle()).isEqualTo("결재 요청");
        assertThat(notification.getMessage()).contains("연차 신청서");
        assertThat(notification.getPriority()).isEqualTo(NotificationPriority.HIGH);
        assertThat(notification.getActionUrl()).isEqualTo("/approval/100");
    }
    
    @Test
    @DisplayName("결재 완료 알림 생성 - 승인")
    void createApprovalCompletedNotification_Approved_Success() {
        // when
        notificationService.createApprovalCompletedNotification(
                recipient.getUserId(),
                200L,
                "구매 요청서",
                true
        );
        
        // then
        Page<Notification> notifications = notificationService.getUserNotifications(
                recipient.getUserId(), null, PageRequest.of(0, 10)
        );
        
        Notification notification = notifications.getContent().get(0);
        assertThat(notification.getType()).isEqualTo(NotificationType.APPROVAL_APPROVED);
        assertThat(notification.getTitle()).isEqualTo("결재 승인");
        assertThat(notification.getMessage()).contains("구매 요청서");
        assertThat(notification.getMessage()).contains("승인되었습니다");
    }
    
    @Test
    @DisplayName("결재 완료 알림 생성 - 반려")
    void createApprovalCompletedNotification_Rejected_Success() {
        // when
        notificationService.createApprovalCompletedNotification(
                recipient.getUserId(),
                300L,
                "경비 청구서",
                false
        );
        
        // then
        Page<Notification> notifications = notificationService.getUserNotifications(
                recipient.getUserId(), null, PageRequest.of(0, 10)
        );
        
        Notification notification = notifications.getContent().get(0);
        assertThat(notification.getType()).isEqualTo(NotificationType.APPROVAL_REJECTED);
        assertThat(notification.getTitle()).isEqualTo("결재 반려");
        assertThat(notification.getMessage()).contains("경비 청구서");
        assertThat(notification.getMessage()).contains("반려되었습니다");
    }
    
    @Test
    @DisplayName("게시글 댓글 알림 생성")
    void createCommentNotification_Success() {
        // when
        notificationService.createCommentNotification(
                recipient.getUserId(),
                50L,
                "공지사항",
                "홍길동"
        );
        
        // then
        Page<Notification> notifications = notificationService.getUserNotifications(
                recipient.getUserId(), null, PageRequest.of(0, 10)
        );
        
        Notification notification = notifications.getContent().get(0);
        assertThat(notification.getType()).isEqualTo(NotificationType.BOARD_COMMENT);
        assertThat(notification.getTitle()).isEqualTo("새 댓글");
        assertThat(notification.getMessage()).contains("홍길동님이");
        assertThat(notification.getMessage()).contains("공지사항");
        assertThat(notification.getPriority()).isEqualTo(NotificationPriority.LOW);
        assertThat(notification.getActionUrl()).isEqualTo("/board/post/50");
    }
    
    // Helper methods
    private Notification createTestNotification(User recipient, NotificationType type, String title, boolean isRead) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message("테스트 메시지")
                .priority(NotificationPriority.NORMAL)
                .build();
        
        if (isRead) {
            notification.markAsRead();
        }
        
        return notificationRepository.save(notification);
    }
}