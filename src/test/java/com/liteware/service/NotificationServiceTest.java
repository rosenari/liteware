package com.liteware.service;

import com.liteware.model.entity.User;
import com.liteware.model.entity.notification.Notification;
import com.liteware.model.entity.notification.Notification.NotificationPriority;
import com.liteware.model.entity.notification.Notification.NotificationType;
import com.liteware.repository.NotificationRepository;
import com.liteware.repository.UserRepository;
import com.liteware.service.notification.NotificationService;
import com.liteware.service.notification.NotificationService.NotificationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    
    @Mock
    private NotificationRepository notificationRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private NotificationService notificationService;
    
    private User user;
    private Notification notification;
    
    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(1L)
                .loginId("test001")
                .name("홍길동")
                .email("test@example.com")
                .build();
        
        notification = Notification.builder()
                .notificationId(1L)
                .recipient(user)
                .type(NotificationType.SYSTEM_NOTICE_NOTICE)
                .title("테스트 알림")
                .message("테스트 메시지")
                .priority(NotificationPriority.NORMAL)
                .isRead(false)
                .isDeleted(false)
                .build();
    }
    
    @Test
    @DisplayName("알림을 생성할 수 있어야 한다")
    void createNotification() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setNotificationId(1L);
            return saved;
        });
        
        Notification result = notificationService.createNotification(
                1L, NotificationType.SYSTEM_NOTICE, "제목", "메시지");
        
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("제목");
        assertThat(result.getMessage()).isEqualTo("메시지");
        assertThat(result.getType()).isEqualTo(NotificationType.SYSTEM_NOTICE);
        verify(notificationRepository).save(any(Notification.class));
    }
    
    @Test
    @DisplayName("상세 알림을 생성할 수 있어야 한다")
    void createDetailedNotification() {
        NotificationDto dto = NotificationDto.builder()
                .userId(1L)
                .type(NotificationType.APPROVAL_REQUEST)
                .title("결재 요청")
                .message("결재 요청이 있습니다")
                .relatedEntityType("approval")
                .relatedEntityId(100L)
                .actionUrl("/approval/100")
                .priority(NotificationPriority.HIGH)
                .expiredAt(LocalDateTime.now().plusDays(7))
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setNotificationId(1L);
            return saved;
        });
        
        Notification result = notificationService.createDetailedNotification(dto);
        
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(NotificationType.APPROVAL_REQUEST);
        assertThat(result.getPriority()).isEqualTo(NotificationPriority.HIGH);
        assertThat(result.getRelatedEntityType()).isEqualTo("approval");
        assertThat(result.getRelatedEntityId()).isEqualTo(100L);
        assertThat(result.getActionUrl()).isEqualTo("/approval/100");
        verify(notificationRepository).save(any(Notification.class));
    }
    
    @Test
    @DisplayName("사용자의 알림 목록을 조회할 수 있어야 한다")
    void getUserNotifications() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notifications = Arrays.asList(notification);
        Page<Notification> page = new PageImpl<>(notifications, pageable, 1);
        
        when(notificationRepository.findByRecipientUserIdAndIsDeleted(1L, false, pageable))
                .thenReturn(page);
        
        Page<Notification> result = notificationService.getUserNotifications(1L, null, pageable);
        
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(notification);
    }
    
    @Test
    @DisplayName("읽지 않은 알림만 조회할 수 있어야 한다")
    void getUserNotifications_UnreadOnly() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notifications = Arrays.asList(notification);
        Page<Notification> page = new PageImpl<>(notifications, pageable, 1);
        
        when(notificationRepository.findByRecipientUserIdAndIsReadAndIsDeleted(1L, false, false, pageable))
                .thenReturn(page);
        
        Page<Notification> result = notificationService.getUserNotifications(1L, false, pageable);
        
        assertThat(result.getContent()).hasSize(1);
        verify(notificationRepository).findByRecipientUserIdAndIsReadAndIsDeleted(1L, false, false, pageable);
    }
    
    @Test
    @DisplayName("읽지 않은 알림 개수를 조회할 수 있어야 한다")
    void getUnreadCount() {
        when(notificationRepository.countByRecipientUserIdAndIsReadAndIsDeleted(1L, false, false))
                .thenReturn(5L);
        
        long result = notificationService.getUnreadCount(1L);
        
        assertThat(result).isEqualTo(5L);
    }
    
    @Test
    @DisplayName("알림을 읽음 처리할 수 있어야 한다")
    void markAsRead() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        
        notificationService.markAsRead(1L);
        
        assertThat(notification.getIsRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }
    
    @Test
    @DisplayName("여러 알림을 읽음 처리할 수 있어야 한다")
    void markAsReadBatch() {
        Notification notification2 = Notification.builder()
                .notificationId(2L)
                .recipient(user)
                .type(NotificationType.SYSTEM_NOTICE_NOTICE)
                .title("알림2")
                .message("메시지2")
                .isRead(false)
                .build();
        
        List<Long> ids = Arrays.asList(1L, 2L);
        List<Notification> notifications = Arrays.asList(notification, notification2);
        
        when(notificationRepository.findAllById(ids)).thenReturn(notifications);
        when(notificationRepository.saveAll(notifications)).thenReturn(notifications);
        
        notificationService.markAsReadBatch(ids);
        
        assertThat(notification.getIsRead()).isTrue();
        assertThat(notification2.getIsRead()).isTrue();
        verify(notificationRepository).saveAll(notifications);
    }
    
    @Test
    @DisplayName("사용자의 모든 알림을 읽음 처리할 수 있어야 한다")
    void markAllAsRead() {
        List<Notification> unreadNotifications = Arrays.asList(notification);
        
        when(notificationRepository.findByRecipientUserIdAndIsReadAndIsDeleted(1L, false, false))
                .thenReturn(unreadNotifications);
        when(notificationRepository.saveAll(unreadNotifications)).thenReturn(unreadNotifications);
        
        notificationService.markAllAsRead(1L);
        
        assertThat(notification.getIsRead()).isTrue();
        verify(notificationRepository).saveAll(unreadNotifications);
    }
    
    @Test
    @DisplayName("알림을 삭제할 수 있어야 한다")
    void deleteNotification() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        
        notificationService.deleteNotification(1L);
        
        assertThat(notification.getIsDeleted()).isTrue();
        verify(notificationRepository).save(notification);
    }
    
    @Test
    @DisplayName("만료된 알림을 정리할 수 있어야 한다")
    void cleanupExpiredNotifications() {
        LocalDateTime expiredTime = LocalDateTime.now().minusDays(1);
        Notification expiredNotification = Notification.builder()
                .notificationId(2L)
                .recipient(user)
                .type(NotificationType.SYSTEM_NOTICE_NOTICE)
                .title("만료된 알림")
                .message("만료된 메시지")
                .expiredAt(expiredTime)
                .isDeleted(false)
                .build();
        
        List<Notification> expiredList = Arrays.asList(expiredNotification);
        
        when(notificationRepository.findByExpiredAtBeforeAndIsDeleted(any(LocalDateTime.class), eq(false)))
                .thenReturn(expiredList);
        when(notificationRepository.saveAll(expiredList)).thenReturn(expiredList);
        
        notificationService.cleanupExpiredNotifications();
        
        assertThat(expiredNotification.getIsDeleted()).isTrue();
        verify(notificationRepository).saveAll(expiredList);
    }
    
    @Test
    @DisplayName("결재 요청 알림을 생성할 수 있어야 한다")
    void createApprovalRequestNotification() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        notificationService.createApprovalRequestNotification(1L, 100L, "휴가 신청서");
        
        verify(notificationRepository).save(argThat(notification -> 
            notification.getType() == NotificationType.APPROVAL_REQUEST &&
            notification.getTitle().equals("결재 요청") &&
            notification.getMessage().contains("휴가 신청서") &&
            notification.getPriority() == NotificationPriority.HIGH &&
            notification.getActionUrl().equals("/approval/100")
        ));
    }
    
    @Test
    @DisplayName("결재 승인 알림을 생성할 수 있어야 한다")
    void createApprovalCompletedNotification_Approved() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        notificationService.createApprovalCompletedNotification(1L, 100L, "휴가 신청서", true);
        
        verify(notificationRepository).save(argThat(notification -> 
            notification.getType() == NotificationType.APPROVAL_APPROVED &&
            notification.getTitle().equals("결재 승인") &&
            notification.getMessage().contains("승인되었습니다")
        ));
    }
    
    @Test
    @DisplayName("결재 반려 알림을 생성할 수 있어야 한다")
    void createApprovalCompletedNotification_Rejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        notificationService.createApprovalCompletedNotification(1L, 100L, "휴가 신청서", false);
        
        verify(notificationRepository).save(argThat(notification -> 
            notification.getType() == NotificationType.APPROVAL_REJECTED &&
            notification.getTitle().equals("결재 반려") &&
            notification.getMessage().contains("반려되었습니다")
        ));
    }
    
    @Test
    @DisplayName("게시글 댓글 알림을 생성할 수 있어야 한다")
    void createCommentNotification() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        notificationService.createCommentNotification(1L, 50L, "공지사항", "김철수");
        
        verify(notificationRepository).save(argThat(notification -> 
            notification.getType() == NotificationType.BOARD_COMMENT &&
            notification.getTitle().equals("새 댓글") &&
            notification.getMessage().contains("김철수") &&
            notification.getMessage().contains("공지사항") &&
            notification.getPriority() == NotificationPriority.LOW &&
            notification.getActionUrl().equals("/board/post/50")
        ));
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자로 알림 생성 시 예외가 발생해야 한다")
    void createNotification_UserNotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> notificationService.createNotification(
                999L, NotificationType.SYSTEM_NOTICE, "제목", "메시지"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
    
    @Test
    @DisplayName("존재하지 않는 알림 읽음 처리 시 예외가 발생해야 한다")
    void markAsRead_NotificationNotFound_ThrowsException() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> notificationService.markAsRead(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Notification not found");
    }
}