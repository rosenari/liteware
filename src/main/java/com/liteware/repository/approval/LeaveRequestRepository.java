package com.liteware.repository.approval;

import com.liteware.model.entity.approval.ApprovalDocument;
import com.liteware.model.entity.approval.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    
    Optional<LeaveRequest> findByDocument(ApprovalDocument document);
    
}