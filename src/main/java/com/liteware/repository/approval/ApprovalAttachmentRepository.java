package com.liteware.repository.approval;

import com.liteware.model.entity.approval.ApprovalAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalAttachmentRepository extends JpaRepository<ApprovalAttachment, Long> {
    
}