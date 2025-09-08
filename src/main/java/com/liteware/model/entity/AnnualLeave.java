package com.liteware.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "annual_leaves")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AnnualLeave extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "annual_leave_id")
    private Long annualLeaveId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "leave_year", nullable = false)
    private Integer year;
    
    @Column(name = "total_hours", nullable = false)
    @Builder.Default
    private Double totalHours = 0.0;  // 총 연차 시간 (시간 단위)
    
    @Column(name = "used_hours", nullable = false)
    @Builder.Default
    private Double usedHours = 0.0;   // 사용한 연차 시간
    
    @Column(name = "remaining_hours", nullable = false)
    @Builder.Default
    private Double remainingHours = 0.0;  // 남은 연차 시간
    
    @Column(name = "carried_over_hours")
    @Builder.Default
    private Double carriedOverHours = 0.0;  // 이월 연차 시간
    
    @Column(name = "granted_date")
    private LocalDate grantedDate;  // 연차 부여일
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate;   // 연차 만료일
    
    // 연차 사용
    public void useLeave(double hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("사용할 연차 시간은 0보다 커야 합니다.");
        }
        
        if (hours > remainingHours) {
            throw new RuntimeException("연차가 부족합니다. 남은 시간: " + remainingHours + ", 요청 시간: " + hours);
        }
        
        this.usedHours += hours;
        this.remainingHours = totalHours + carriedOverHours - usedHours;
    }
    
    // 연차 복원 (반려 시)
    public void restoreLeave(double hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("복원할 연차 시간은 0보다 커야 합니다.");
        }
        
        this.usedHours = Math.max(0, usedHours - hours);
        this.remainingHours = totalHours + carriedOverHours - usedHours;
    }
    
    // 연차 추가 부여
    public void addLeave(double hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("추가할 연차 시간은 0보다 커야 합니다.");
        }
        
        this.totalHours += hours;
        this.remainingHours = totalHours + carriedOverHours - usedHours;
    }
    
    // 이월 연차 설정
    public void setCarriedOver(double hours) {
        this.carriedOverHours = Math.max(0, hours);
        this.remainingHours = totalHours + carriedOverHours - usedHours;
    }
    
    // 총 사용 가능한 연차 시간
    public Double getTotalAvailableHours() {
        return totalHours + carriedOverHours;
    }
    
    // 연차 사용률 (%)
    public Double getUsageRate() {
        double totalAvailable = getTotalAvailableHours();
        return totalAvailable > 0 ? (usedHours / totalAvailable) * 100 : 0.0;
    }
    
    // 일수 변환 (8시간 = 1일 기준)
    public Double getTotalDays() {
        return totalHours / 8.0;
    }
    
    public Double getUsedDays() {
        return usedHours / 8.0;
    }
    
    public Double getRemainingDays() {
        return remainingHours / 8.0;
    }
    
    // 연차 만료 여부
    public boolean isExpired() {
        return expiryDate != null && LocalDate.now().isAfter(expiryDate);
    }
    
    // 연차 현황 문자열
    public String getLeaveStatus() {
        return String.format("%.1f시간 (%.1f일) 사용 / %.1f시간 (%.1f일) 잔여", 
                usedHours, getUsedDays(), remainingHours, getRemainingDays());
    }
}