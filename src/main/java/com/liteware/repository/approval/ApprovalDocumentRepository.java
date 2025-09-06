package com.liteware.repository.approval;

import com.liteware.model.entity.User;
import com.liteware.model.entity.approval.ApprovalDocument;
import com.liteware.model.entity.approval.DocumentStatus;
import com.liteware.model.entity.approval.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalDocumentRepository extends JpaRepository<ApprovalDocument, Long> {
    
    Optional<ApprovalDocument> findByDocNumber(String docNumber);
    
    List<ApprovalDocument> findByDrafter(User drafter);
    
    List<ApprovalDocument> findByCurrentApprover(User currentApprover);
    
    List<ApprovalDocument> findByStatus(DocumentStatus status);
    
    List<ApprovalDocument> findByDocType(DocumentType docType);
    
    List<ApprovalDocument> findByCurrentApproverAndStatus(User currentApprover, DocumentStatus status);
    
    Page<ApprovalDocument> findByDrafter(User drafter, Pageable pageable);
    
    Page<ApprovalDocument> findByCurrentApprover(User currentApprover, Pageable pageable);
    
    @Query("SELECT d FROM ApprovalDocument d WHERE d.drafter = :user " +
           "AND d.status = :status AND d.isDeleted = false")
    List<ApprovalDocument> findByDrafterAndStatus(@Param("user") User user, 
                                                  @Param("status") DocumentStatus status);
    
    @Query("SELECT d FROM ApprovalDocument d JOIN d.approvalLines l " +
           "WHERE l.approver = :approver AND l.status = 'PENDING' " +
           "AND d.status = 'PENDING' AND d.isDeleted = false")
    List<ApprovalDocument> findPendingDocumentsByApprover(@Param("approver") User approver);
    
    @Query("SELECT d FROM ApprovalDocument d JOIN d.approvalLines l " +
           "WHERE l.approver = :approver AND l.status = 'APPROVED' " +
           "AND d.isDeleted = false")
    List<ApprovalDocument> findApprovedDocumentsByApprover(@Param("approver") User approver);
    
    @Query("SELECT d FROM ApprovalDocument d WHERE " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND d.isDeleted = false")
    Page<ApprovalDocument> searchDocuments(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT d FROM ApprovalDocument d WHERE d.draftedAt BETWEEN :startDate AND :endDate " +
           "AND d.isDeleted = false")
    List<ApprovalDocument> findByDraftedAtBetween(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(d) FROM ApprovalDocument d WHERE d.currentApprover = :approver " +
           "AND d.status = 'PENDING'")
    Long countPendingDocuments(@Param("approver") User approver);
    
    @Query("SELECT d FROM ApprovalDocument d LEFT JOIN FETCH d.approvalLines " +
           "WHERE d.docId = :docId")
    Optional<ApprovalDocument> findByIdWithApprovalLines(@Param("docId") Long docId);
    
    @Query("SELECT DISTINCT d FROM ApprovalDocument d LEFT JOIN d.approvalLines l " +
           "WHERE (d.drafter = :user OR l.approver = :user) " +
           "AND d.isDeleted = false " +
           "ORDER BY d.createdAt DESC")
    List<ApprovalDocument> findRecentDocumentsByUser(@Param("user") User user, Pageable pageable);
    
    /**
     * 특정 상태의 문서 수 조회
     */
    long countByStatus(DocumentStatus status);
}