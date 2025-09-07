package com.liteware.model.entity.approval;

import com.liteware.model.entity.BaseEntity;
import com.liteware.model.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "approval_references",
       indexes = {
           @Index(name = "idx_ref_document", columnList = "doc_id"),
           @Index(name = "idx_ref_user", columnList = "user_id")
       })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalReference extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ref_id")
    private Long refId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_id", nullable = false)
    private ApprovalDocument document;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "sort_order")
    private Integer sortOrder;
    
    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;
}