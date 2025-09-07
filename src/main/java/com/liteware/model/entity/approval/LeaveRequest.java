package com.liteware.model.entity.approval;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.Duration;

@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long leaveRequestId;
    
    @OneToOne
    @JoinColumn(name = "doc_id", nullable = false)
    private ApprovalDocument document;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveType leaveType;
    
    @Column(nullable = false)
    private LocalDate startDate;
    
    @Column(nullable = false)
    private LocalDate endDate;
    
    private LocalTime startTime;     // 시작 시간 (시간 단위 휴가용)
    
    private LocalTime endTime;       // 종료 시간 (시간 단위 휴가용)
    
    @Builder.Default
    @Column(nullable = false)
    private Boolean isHourlyLeave = false;  // 시간 단위 휴가 여부
    
    private Double leaveHours;       // 휴가 시간 (시간 단위)
    
    private Integer leaveDays;       // 휴가 일수 (일 단위)
    
    private String reason;          // 휴가 사유
    
    private String emergencyContact; // 비상 연락처
    
    private String substitute;      // 대체 근무자
    
    private String handoverTo;      // 업무 인수인계자
    
    @Column(columnDefinition = "TEXT")
    private String handoverDetails; // 인수인계 내용
    
    // 휴가 유형
    public enum LeaveType {
        ANNUAL("연차"),
        SICK("병가"),
        SPECIAL("경조사"),
        MATERNITY("출산휴가"),
        PATERNITY("배우자출산휴가"),
        CHILDCARE("육아휴직"),
        OTHER("기타");
        
        private final String description;
        
        LeaveType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 휴가 일수/시간 자동 계산
    @PrePersist
    @PreUpdate
    public void calculateLeaveTime() {
        if (isHourlyLeave != null && isHourlyLeave) {
            // 시간 단위 휴가인 경우
            if (startDate != null && endDate != null && startTime != null && endTime != null) {
                LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);
                LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);
                
                Duration duration = Duration.between(startDateTime, endDateTime);
                this.leaveHours = duration.toMinutes() / 60.0; // 분을 시간으로 변환
                
                // 일수도 계산 (8시간 = 1일 기준)
                this.leaveDays = (int) Math.ceil(leaveHours / 8.0);
            }
        } else {
            // 일 단위 휴가인 경우
            if (startDate != null && endDate != null) {
                this.leaveDays = (int) (endDate.toEpochDay() - startDate.toEpochDay() + 1);
                this.leaveHours = leaveDays * 8.0; // 1일 = 8시간으로 환산
            }
        }
    }
    
    // 휴가 기간 문자열 반환
    public String getLeaveTimeDisplay() {
        if (isHourlyLeave != null && isHourlyLeave) {
            return String.format("%.1f시간", leaveHours);
        } else {
            return leaveDays + "일";
        }
    }
}