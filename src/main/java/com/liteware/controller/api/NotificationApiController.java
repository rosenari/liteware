package com.liteware.controller.api;

import com.liteware.model.entity.notification.Notification;
import com.liteware.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationApiController {
    
    private final NotificationService notificationService;
    
    /**
     * 사용자 알림 목록 조회
     */
    @GetMapping
    public ResponseEntity<Page<Notification>> getNotifications(
            @RequestParam Long userId,
            @RequestParam(required = false) Boolean isRead,
            Pageable pageable) {
        
        Page<Notification> notifications = notificationService.getUserNotifications(userId, isRead, pageable);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * 읽지 않은 알림 개수 조회
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("count", notificationService.getUnreadCount(userId));
        return ResponseEntity.ok(response);
    }
    
    /**
     * 알림 읽음 처리
     */
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long notificationId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            notificationService.markAsRead(notificationId);
            response.put("success", true);
            response.put("message", "알림을 읽음 처리했습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to mark notification as read", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 여러 알림 읽음 처리
     */
    @PostMapping("/read-batch")
    public ResponseEntity<Map<String, Object>> markAsReadBatch(@RequestBody List<Long> notificationIds) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            notificationService.markAsReadBatch(notificationIds);
            response.put("success", true);
            response.put("message", notificationIds.size() + "개의 알림을 읽음 처리했습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to mark notifications as read", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 모든 알림 읽음 처리
     */
    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            notificationService.markAllAsRead(userId);
            response.put("success", true);
            response.put("message", "모든 알림을 읽음 처리했습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to mark all notifications as read", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 알림 삭제
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long notificationId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            notificationService.deleteNotification(notificationId);
            response.put("success", true);
            response.put("message", "알림이 삭제되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to delete notification", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 알림 생성 (테스트용)
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> createTestNotification(
            @RequestBody NotificationService.NotificationDto dto) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Notification notification = notificationService.createDetailedNotification(dto);
            response.put("success", true);
            response.put("message", "알림이 생성되었습니다.");
            response.put("notification", notification);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create notification", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}