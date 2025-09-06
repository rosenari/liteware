package com.liteware.scheduler;

import com.liteware.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {
    
    private final NotificationService notificationService;
    
    /**
     * 매일 자정에 만료된 알림 정리
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupExpiredNotifications() {
        log.info("Starting scheduled task: Cleanup expired notifications");
        try {
            notificationService.cleanupExpiredNotifications();
            log.info("Completed scheduled task: Cleanup expired notifications");
        } catch (Exception e) {
            log.error("Error during scheduled cleanup of expired notifications", e);
        }
    }
    
    /**
     * 30일이 지난 읽은 알림 정리 (매주 일요일 새벽 3시)
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void cleanupOldReadNotifications() {
        log.info("Starting scheduled task: Cleanup old read notifications");
        try {
            // TODO: Implement cleanup of old read notifications
            // This would require adding a method in NotificationService
            log.info("Completed scheduled task: Cleanup old read notifications");
        } catch (Exception e) {
            log.error("Error during scheduled cleanup of old read notifications", e);
        }
    }
}