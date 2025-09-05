package com.liteware.model.entity.approval;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "overtime_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvertimeRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long overtimeRequestId;
    
    @OneToOne
    @JoinColumn(name = "doc_id", nullable = false)
    private ApprovalDocument document;
    
    @Column(nullable = false)
    private LocalDate overtimeDate;     // 연장근무 날짜
    
    @Column(nullable = false)
    private LocalTime startTime;        // 시작 시간
    
    @Column(nullable = false)
    private LocalTime endTime;          // 종료 시간
    
    private Double overtimeHours;       // 연장근무 시간
    
    @Column(nullable = false)
    private String reason;              // 연장근무 사유
    
    @Column(columnDefinition = "TEXT")
    private String workDetails;         // 업무 내용
    
    @Enumerated(EnumType.STRING)
    private OvertimeType overtimeType;  // 연장근무 유형
    
    @Enumerated(EnumType.STRING)
    private CompensationType compensationType; // 보상 유형
    
    private String approverComment;     // 승인자 코멘트
    
    // 연장근무 유형
    public enum OvertimeType {
        WEEKDAY("평일연장"),
        WEEKEND("주말근무"),
        HOLIDAY("휴일근무"),
        NIGHT("야간근무");
        
        private final String description;
        
        OvertimeType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 보상 유형
    public enum CompensationType {
        PAYMENT("수당지급"),
        COMPENSATORY_LEAVE("대체휴가"),
        BOTH("수당+대체휴가");
        
        private final String description;
        
        CompensationType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 연장근무 시간 자동 계산
    @PrePersist
    @PreUpdate
    public void calculateOvertimeHours() {
        if (startTime != null && endTime != null) {
            long minutes = java.time.Duration.between(startTime, endTime).toMinutes();
            if (minutes < 0) {
                // 종료 시간이 다음 날인 경우
                minutes += 24 * 60;
            }
            this.overtimeHours = minutes / 60.0;
        }
    }
}