package com.liteware.model.entity.notification;

import com.liteware.model.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User recipient;             // 수신자
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;      // 알림 유형
    
    @Column(nullable = false)
    private String title;               // 알림 제목
    
    @Column(columnDefinition = "TEXT")
    private String message;             // 알림 내용
    
    private String relatedEntityType;   // 관련 엔티티 타입
    
    private Long relatedEntityId;       // 관련 엔티티 ID
    
    private String actionUrl;           // 액션 URL
    
    @Builder.Default
    private Boolean isRead = false;     // 읽음 여부
    
    private LocalDateTime readAt;       // 읽은 시간
    
    @Builder.Default
    private Boolean isDeleted = false;  // 삭제 여부
    
    @Enumerated(EnumType.STRING)
    private NotificationPriority priority; // 우선순위
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    private LocalDateTime expiredAt;    // 만료 시간
    
    // 알림 유형
    public enum NotificationType {
        APPROVAL_REQUEST("결재요청"),
        APPROVAL_APPROVED("결재승인"),
        APPROVAL_REJECTED("결재반려"),
        APPROVAL_COMPLETED("결재완료"),
        BOARD_COMMENT("게시글댓글"),
        BOARD_MENTION("게시글멘션"),
        SCHEDULE_REMINDER("일정알림"),
        SYSTEM_NOTICE("시스템공지"),
        USER_MESSAGE("개인메시지");
        
        private final String description;
        
        NotificationType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 알림 우선순위
    public enum NotificationPriority {
        LOW("낮음"),
        NORMAL("보통"),
        HIGH("높음"),
        URGENT("긴급");
        
        private final String description;
        
        NotificationPriority(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 알림 읽음 처리
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
    
    // 알림 삭제 처리
    public void markAsDeleted() {
        this.isDeleted = true;
    }
    
    // 만료 여부 확인
    public boolean isExpired() {
        return expiredAt != null && LocalDateTime.now().isAfter(expiredAt);
    }
}