package com.liteware.model.entity.approval;

import com.liteware.model.entity.BaseEntity;
import com.liteware.model.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "approval_documents",
       indexes = {
           @Index(name = "idx_doc_number", columnList = "doc_number"),
           @Index(name = "idx_drafter", columnList = "drafter_id"),
           @Index(name = "idx_current_approver", columnList = "current_approver_id"),
           @Index(name = "idx_status", columnList = "status")
       })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDocument extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doc_id")
    private Long docId;
    
    @Column(name = "doc_number", unique = true, nullable = false, length = 30)
    private String docNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 30)
    private DocumentType docType;
    
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "form_data", columnDefinition = "TEXT")
    private String formData;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.DRAFT;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "drafter_id", nullable = false)
    private User drafter;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_approver_id")
    private User currentApprover;

    @CreationTimestamp
    @Column(name = "drafted_at")
    private LocalDateTime draftedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false, length = 20)
    @Builder.Default
    private UrgencyType urgency = UrgencyType.NORMAL;
    
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true,  fetch = FetchType.EAGER)
    @OrderBy("orderSeq ASC")
    @Builder.Default
    private List<ApprovalLine> approvalLines = new ArrayList<>();
    
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Builder.Default
    private List<ApprovalAttachment> attachments = new ArrayList<>();
    
    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private LeaveRequest leaveRequest;
    
    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private OvertimeRequest overtimeRequest;
    
    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private ExpenseRequest expenseRequest;
    
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<ApprovalReference> references = new ArrayList<>();
    
    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;
    
    @PrePersist
    public void generateDocNumber() {
        if (this.docNumber == null) {
            this.docNumber = "DOC-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
    
    public void addApprovalLine(ApprovalLine line) {
        approvalLines.add(line);
        line.setDocument(this);
    }
    
    public void removeApprovalLine(ApprovalLine line) {
        approvalLines.remove(line);
        line.setDocument(null);
    }
    
    public void addAttachment(ApprovalAttachment attachment) {
        attachments.add(attachment);
        attachment.setDocument(this);
    }
    
    public void removeAttachment(ApprovalAttachment attachment) {
        attachments.remove(attachment);
        attachment.setDocument(null);
    }
    
    public void addReference(ApprovalReference reference) {
        references.add(reference);
        reference.setDocument(this);
    }
    
    public void removeReference(ApprovalReference reference) {
        references.remove(reference);
        reference.setDocument(null);
    }
    
    public void clearReferences() {
        references.clear();
    }
}