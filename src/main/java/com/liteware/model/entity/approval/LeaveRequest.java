package com.liteware.model.entity.approval;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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
    
    private Integer leaveDays;      // 휴가 일수
    
    private String reason;          // 휴가 사유
    
    private String emergencyContact; // 비상 연락처
    
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
    
    // 휴가 일수 자동 계산
    @PrePersist
    @PreUpdate
    public void calculateLeaveDays() {
        if (startDate != null && endDate != null) {
            this.leaveDays = (int) (endDate.toEpochDay() - startDate.toEpochDay() + 1);
        }
    }
}