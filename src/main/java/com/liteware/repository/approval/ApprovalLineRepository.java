package com.liteware.repository.approval;

import com.liteware.model.entity.User;
import com.liteware.model.entity.approval.ApprovalDocument;
import com.liteware.model.entity.approval.ApprovalLine;
import com.liteware.model.entity.approval.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalLineRepository extends JpaRepository<ApprovalLine, Long> {
    
    List<ApprovalLine> findByDocument(ApprovalDocument document);
    
    List<ApprovalLine> findByDocumentOrderByOrderSeq(ApprovalDocument document);
    
    List<ApprovalLine> findByApprover(User approver);
    
    List<ApprovalLine> findByApproverAndStatus(User approver, ApprovalStatus status);
    
    Optional<ApprovalLine> findByDocumentAndApprover(ApprovalDocument document, User approver);
    
    Optional<ApprovalLine> findByDocumentAndOrderSeq(ApprovalDocument document, Integer orderSeq);
    
    @Query("SELECT l FROM ApprovalLine l WHERE l.document = :document " +
           "AND l.status = 'PENDING' ORDER BY l.orderSeq ASC")
    List<ApprovalLine> findPendingLinesByDocument(@Param("document") ApprovalDocument document);
    
    @Query("SELECT l FROM ApprovalLine l WHERE l.document = :document " +
           "AND l.approvalType = 'APPROVAL' AND l.status = 'APPROVED'")
    List<ApprovalLine> findApprovedLinesByDocument(@Param("document") ApprovalDocument document);
    
    @Query("SELECT COUNT(l) FROM ApprovalLine l WHERE l.document = :document " +
           "AND l.approvalType = 'APPROVAL' AND l.status = 'PENDING'")
    Long countPendingApprovals(@Param("document") ApprovalDocument document);
    
    @Query("SELECT MIN(l.orderSeq) FROM ApprovalLine l WHERE l.document = :document " +
           "AND l.status = 'PENDING'")
    Integer findNextApprovalOrder(@Param("document") ApprovalDocument document);
    
    void deleteByDocument(ApprovalDocument document);
    
    @Query("SELECT l FROM ApprovalLine l WHERE l.approver.userId = :approverId AND l.status = :status")
    List<ApprovalLine> findByApproverIdAndStatus(@Param("approverId") Long approverId, @Param("status") ApprovalStatus status);
    
    Optional<ApprovalLine> findByDocumentAndDelegatedTo(ApprovalDocument document, User delegatedTo);
    
    @Query("SELECT l FROM ApprovalLine l WHERE l.delegatedTo = :delegatedTo AND l.status = 'PENDING'")
    List<ApprovalLine> findPendingDelegatedLines(@Param("delegatedTo") User delegatedTo);
    
    @Query("SELECT l FROM ApprovalLine l WHERE l.approver = :approver AND l.delegatedTo IS NOT NULL")
    List<ApprovalLine> findDelegatedLinesByApprover(@Param("approver") User approver);
}