package com.liteware.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attendance extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attendanceId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private LocalDate workDate;
    
    @Column
    private LocalDateTime checkInTime;
    
    @Column
    private LocalDateTime checkOutTime;
    
    @Column
    private String memo;
    
    // 근무 시간 계산 메서드 (초 단위 반올림)
    public Duration getWorkingDuration() {
        if (checkInTime == null || checkOutTime == null) {
            return Duration.ZERO;
        }
        Duration duration = Duration.between(checkInTime, checkOutTime);
        
        // 초 단위를 분 단위로 반올림 (30초 이상이면 1분으로 올림)
        long totalSeconds = duration.getSeconds();
        long minutes = totalSeconds / 60;
        long remainingSeconds = totalSeconds % 60;
        
        if (remainingSeconds >= 30) {
            minutes += 1;
        }
        
        return Duration.ofMinutes(minutes);
    }
    
    // 근무 시간을 "X시간 Y분" 형태로 반환
    public String getFormattedWorkingHours() {
        Duration duration = getWorkingDuration();
        if (duration.isZero()) {
            return "0시간 0분";
        }
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        return String.format("%d시간 %d분", hours, minutes);
    }
    
    // 출근 여부 확인
    public boolean isCheckedIn() {
        return checkInTime != null;
    }
    
    // 퇴근 여부 확인
    public boolean isCheckedOut() {
        return checkOutTime != null;
    }
}