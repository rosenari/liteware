package com.liteware.model.entity.approval;

import com.liteware.model.entity.BaseEntity;
import com.liteware.model.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_lines",
       indexes = {
           @Index(name = "idx_doc_order", columnList = "doc_id, order_seq"),
           @Index(name = "idx_approver", columnList = "approver_id")
       })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalLine extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Long lineId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_id", nullable = false)
    private ApprovalDocument document;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_type", nullable = false, length = 20)
    private ApprovalType approvalType;
    
    @Column(name = "order_seq", nullable = false)
    private Integer orderSeq;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;
    
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "is_optional")
    @Builder.Default
    private Boolean isOptional = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegated_to")
    private User delegatedTo;
    
    @Column(name = "delegated_at")
    private LocalDateTime delegatedAt;
    
    public void approve(String comment) {
        this.status = ApprovalStatus.APPROVED;
        this.comment = comment;
        this.approvedAt = LocalDateTime.now();
    }
    
    public void reject(String comment) {
        this.status = ApprovalStatus.REJECTED;
        this.comment = comment;
        this.approvedAt = LocalDateTime.now();
    }
    
    public void delegate(User delegateTo) {
        this.delegatedTo = delegateTo;
        this.delegatedAt = LocalDateTime.now();
    }
}