package com.liteware.service.approval;

import com.liteware.model.dto.ApprovalDocumentDto;
import com.liteware.model.dto.ApprovalLineDto;
import com.liteware.model.entity.User;
import com.liteware.model.entity.approval.*;
import com.liteware.repository.UserRepository;
import com.liteware.repository.approval.ApprovalDocumentRepository;
import com.liteware.repository.approval.ApprovalLineRepository;
import com.liteware.service.leave.AnnualLeaveService;
import com.liteware.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalService {
    
    private final ApprovalDocumentRepository documentRepository;
    private final ApprovalLineRepository approvalLineRepository;
    private final UserRepository userRepository;
    private final ApprovalWorkflowService workflowService;
    private final AnnualLeaveService annualLeaveService;
    private final NotificationService notificationService;
    
    public ApprovalDocument createDocument(ApprovalDocumentDto dto) {
        return draftDocument(dto);
    }
    
    public Page<ApprovalDocument> getAllDocuments(Pageable pageable) {
        return documentRepository.findAll(pageable);
    }
    
    public Page<ApprovalDocument> getPendingDocuments(Long approverId, Pageable pageable) {
        List<ApprovalDocument> pendingDocs = getPendingDocuments(approverId);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), pendingDocs.size());
        
        if (start > pendingDocs.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, pendingDocs.size());
        }
        
        return new PageImpl<>(pendingDocs.subList(start, end), pageable, pendingDocs.size());
    }
    
    public Page<ApprovalDocument> getDraftedDocuments(Long drafterId, Pageable pageable) {
        List<ApprovalDocument> draftedDocs = getDraftedDocuments(drafterId);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), draftedDocs.size());
        
        if (start > draftedDocs.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, draftedDocs.size());
        }
        
        return new PageImpl<>(draftedDocs.subList(start, end), pageable, draftedDocs.size());
    }
    
    public long getApprovedCount(Long userId) {
        return approvalLineRepository.findByApproverIdAndStatus(userId, ApprovalStatus.APPROVED).size();
    }
    
    public long getRejectedCount(Long userId) {
        return approvalLineRepository.findByApproverIdAndStatus(userId, ApprovalStatus.REJECTED).size();
    }
    
    public ApprovalDocument draftDocument(ApprovalDocumentDto dto) {
        User drafter = userRepository.findById(dto.getDrafterId())
                .orElseThrow(() -> new RuntimeException("기안자를 찾을 수 없습니다"));
        
        ApprovalDocument document = ApprovalDocument.builder()
                .docType(dto.getDocType())
                .title(dto.getTitle())
                .content(dto.getContent())
                .formData(dto.getFormData())
                .drafter(drafter)
                .status(DocumentStatus.DRAFT)
                .urgency(dto.getUrgency())
                .build();
        
        return documentRepository.save(document);
    }
    
    @Transactional
    public List<ApprovalLine> setApprovalLine(Long docId, List<ApprovalLineDto> lineDtos) {
        ApprovalDocument document = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));
        
        if (document.getStatus() != DocumentStatus.DRAFT) {
            throw new RuntimeException("임시저장 상태의 문서만 결재선을 설정할 수 있습니다");
        }
        
        // 기존 결재선 삭제
        approvalLineRepository.deleteByDocument(document);
        document.getApprovalLines().clear();
        
        List<ApprovalLine> lines = new ArrayList<>();
        for (ApprovalLineDto dto : lineDtos) {
            User approver = userRepository.findById(dto.getApproverId())
                    .orElseThrow(() -> new RuntimeException("결재자를 찾을 수 없습니다: " + dto.getApproverId()));
            
            ApprovalLine line = ApprovalLine.builder()
                    .document(document)
                    .approver(approver)
                    .approvalType(dto.getApprovalType())
                    .orderSeq(dto.getOrderSeq())
                    .status(ApprovalStatus.PENDING)
                    .isOptional(dto.getIsOptional() != null ? dto.getIsOptional() : false)
                    .build();
            
            lines.add(line);
            document.addApprovalLine(line);
        }
        
        // 결재선 저장
        List<ApprovalLine> savedLines = approvalLineRepository.saveAll(lines);
        
        // 문서 업데이트
        documentRepository.save(document);
        
        log.info("Document {} approval lines updated: {} lines", docId, savedLines.size());
        
        return savedLines;
    }
    
    public ApprovalDocument submitDocument(Long docId) {
        ApprovalDocument document = documentRepository.findByIdWithApprovalLines(docId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));
        
        if (document.getStatus() != DocumentStatus.DRAFT) {
            throw new RuntimeException("임시저장 상태의 문서만 상신할 수 있습니다");
        }
        
        if (document.getApprovalLines().isEmpty()) {
            throw new RuntimeException("결재선이 설정되지 않았습니다");
        }
        
        ApprovalLine firstLine = approvalLineRepository.findByDocumentAndOrderSeq(document, 1)
                .orElseThrow(() -> new RuntimeException("첫 번째 결재자를 찾을 수 없습니다"));
        
        document.setStatus(DocumentStatus.PENDING);
        document.setDraftedAt(LocalDateTime.now());
        document.setCurrentApprover(firstLine.getApprover());
        
        ApprovalDocument savedDocument = documentRepository.save(document);
        
        // 승인자에게 알림 전송
        notificationService.createApprovalRequestNotification(
            firstLine.getApprover().getUserId(),
            savedDocument.getDocId(),
            savedDocument.getTitle()
        );
        
        log.info("Document submitted: {}", savedDocument.getDocNumber());
        
        return savedDocument;
    }
    
    public ApprovalDocument approveDocument(Long docId, Long approverId, String comment) {
        ApprovalDocument document = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));
        
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("결재자를 찾을 수 없습니다"));
        
        if (document.getStatus() != DocumentStatus.PENDING) {
            throw new RuntimeException("진행중인 문서만 결재할 수 있습니다");
        }
        
        if (document.getCurrentApprover() == null || 
            !document.getCurrentApprover().getUserId().equals(approverId)) {
            throw new RuntimeException("현재 결재 순서가 아닙니다");
        }
        
        ApprovalLine currentLine = approvalLineRepository.findByDocumentAndApprover(document, approver)
                .orElseThrow(() -> new RuntimeException("결재선에서 결재자를 찾을 수 없습니다"));
        
        currentLine.approve(comment);
        approvalLineRepository.save(currentLine);
        
        Integer nextOrder = currentLine.getOrderSeq() + 1;
        ApprovalLine nextLine = approvalLineRepository.findByDocumentAndOrderSeq(document, nextOrder)
                .orElse(null);
        
        if (nextLine != null) {
            document.setCurrentApprover(nextLine.getApprover());
            // 다음 결재자에게 알림 전송
            notificationService.createApprovalRequestNotification(
                nextLine.getApprover().getUserId(),
                document.getDocId(),
                document.getTitle()
            );
        } else {
            document.setStatus(DocumentStatus.APPROVED);
            document.setCurrentApprover(null);
            document.setCompletedAt(LocalDateTime.now());
            workflowService.onDocumentApproved(document);
            // 기안자에게 승인 완료 알림
            notificationService.createApprovalCompletedNotification(
                document.getDrafter().getUserId(),
                document.getDocId(),
                document.getTitle(),
                true
            );
        }
        
        log.info("Document approved by {}: {}", approver.getName(), document.getDocNumber());
        
        return documentRepository.save(document);
    }
    
    public ApprovalDocument rejectDocument(Long docId, Long approverId, String reason) {
        ApprovalDocument document = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));
        
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("결재자를 찾을 수 없습니다"));
        
        if (document.getStatus() != DocumentStatus.PENDING) {
            throw new RuntimeException("진행중인 문서만 반려할 수 있습니다");
        }
        
        if (!document.getCurrentApprover().getUserId().equals(approverId)) {
            throw new RuntimeException("현재 결재 순서가 아닙니다");
        }
        
        ApprovalLine currentLine = approvalLineRepository.findByDocumentAndApprover(document, approver)
                .orElseThrow(() -> new RuntimeException("결재선에서 결재자를 찾을 수 없습니다"));
        
        currentLine.reject(reason);
        approvalLineRepository.save(currentLine);
        
        document.setStatus(DocumentStatus.REJECTED);
        document.setCurrentApprover(null);
        document.setCompletedAt(LocalDateTime.now());
        
        workflowService.onDocumentRejected(document, reason);
        
        // 기안자에게 반려 알림
        notificationService.createApprovalCompletedNotification(
            document.getDrafter().getUserId(),
            document.getDocId(),
            document.getTitle(),
            false
        );
        
        log.info("Document rejected by {}: {}", approver.getName(), document.getDocNumber());
        
        return documentRepository.save(document);
    }
    
    public ApprovalDocument cancelDocument(Long docId, Long userId) {
        ApprovalDocument document = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        if (!document.getDrafter().getUserId().equals(userId)) {
            throw new RuntimeException("기안자만 문서를 회수할 수 있습니다");
        }
        
        if (document.getStatus() != DocumentStatus.PENDING) {
            throw new RuntimeException("진행중인 문서만 회수할 수 있습니다");
        }
        
        document.setStatus(DocumentStatus.CANCELLED);
        document.setCurrentApprover(null);
        document.setCompletedAt(LocalDateTime.now());
        
        log.info("Document cancelled by drafter: {}", document.getDocNumber());
        
        return documentRepository.save(document);
    }
    
    @Transactional(readOnly = true)
    public ApprovalDocument getDocument(Long docId) {
        ApprovalDocument document = documentRepository.findByIdWithApprovalLines(docId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));
        
        // 첨부파일을 lazy하게 로드 (필요시 별도 쿼리로)
        // Hibernate.initialize(document.getAttachments());
        
        return document;
    }
    
    public ApprovalDocument updateDocument(ApprovalDocument document) {
        if (document.getStatus() != DocumentStatus.DRAFT) {
            throw new RuntimeException("기안 상태의 문서만 수정할 수 있습니다");
        }
        
        return documentRepository.save(document);
    }
    
    @Transactional(readOnly = true)
    public List<ApprovalDocument> getPendingDocuments(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        return documentRepository.findByCurrentApproverAndStatus(user, DocumentStatus.PENDING);
    }
    
    @Transactional(readOnly = true)
    public List<ApprovalDocument> getDraftedDocuments(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        return documentRepository.findByDrafter(user);
    }
    
    public void delegateApproval(Long fromUserId, Long toUserId, LocalDateTime startDate, LocalDateTime endDate) {
        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        User toUser = userRepository.findById(toUserId)
                .orElseThrow(() -> new RuntimeException("대리 결재자를 찾을 수 없습니다"));
        
        List<ApprovalDocument> pendingDocuments = documentRepository
                .findByCurrentApproverAndStatus(fromUser, DocumentStatus.PENDING);
        
        for (ApprovalDocument document : pendingDocuments) {
            List<ApprovalLine> lines = approvalLineRepository.findByDocument(document);
            for (ApprovalLine line : lines) {
                if (line.getApprover().equals(fromUser) && 
                    line.getStatus() == ApprovalStatus.PENDING) {
                    line.setDelegatedTo(toUser);
                    line.setDelegatedAt(LocalDateTime.now());
                    approvalLineRepository.save(line);
                    
                    if (document.getCurrentApprover().equals(fromUser)) {
                        document.setCurrentApprover(toUser);
                        documentRepository.save(document);
                    }
                }
            }
        }
        
        log.info("Delegated approval from {} to {} for period {} to {}", 
                fromUser.getName(), toUser.getName(), startDate, endDate);
    }
    
    public ApprovalDocument approveDelegatedDocument(Long docId, Long delegateUserId, String comment) {
        ApprovalDocument document = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));
        
        User delegateUser = userRepository.findById(delegateUserId)
                .orElseThrow(() -> new RuntimeException("대리 결재자를 찾을 수 없습니다"));
        
        if (document.getStatus() != DocumentStatus.PENDING) {
            throw new RuntimeException("진행중인 문서만 결재할 수 있습니다");
        }
        
        ApprovalLine currentLine = approvalLineRepository
                .findByDocumentAndDelegatedTo(document, delegateUser)
                .orElseThrow(() -> new RuntimeException("대리 결재 권한이 없습니다"));
        
        currentLine.approve(comment + " (대리결재: " + delegateUser.getName() + ")");
        approvalLineRepository.save(currentLine);
        
        Integer nextOrder = currentLine.getOrderSeq() + 1;
        ApprovalLine nextLine = approvalLineRepository
                .findByDocumentAndOrderSeq(document, nextOrder)
                .orElse(null);
        
        if (nextLine != null) {
            document.setCurrentApprover(nextLine.getApprover());
        } else {
            document.setStatus(DocumentStatus.APPROVED);
            document.setCurrentApprover(null);
            document.setCompletedAt(LocalDateTime.now());
            workflowService.onDocumentApproved(document);
        }
        
        log.info("Document approved by delegate {}: {}", delegateUser.getName(), document.getDocNumber());
        
        return documentRepository.save(document);
    }
    
    @Transactional(readOnly = true)
    public Page<ApprovalDocument> searchDocuments(String keyword, Pageable pageable) {
        return documentRepository.searchDocuments(keyword, pageable);
    }
    
    @Transactional(readOnly = true)
    public Long countPendingDocuments(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        return documentRepository.countPendingDocuments(user);
    }
    
    @Transactional(readOnly = true)
    public List<ApprovalDocument> getApprovedDocumentsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        return documentRepository.findApprovedDocumentsByApprover(user);
    }
    
    @Transactional(readOnly = true)
    public List<ApprovalDocument> getRecentDocuments(Long userId, int limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        // 사용자와 관련된 최근 문서 조회 (기안자 또는 결재자)
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return documentRepository.findRecentDocumentsByUserWithStatus(user, DocumentStatus.PENDING, pageable);
    }
    
    public ApprovalDocument updateDocument(Long docId, ApprovalDocumentDto dto) {
        ApprovalDocument document = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));
        
        if (document.getStatus() != DocumentStatus.DRAFT) {
            throw new RuntimeException("임시저장 상태의 문서만 수정할 수 있습니다");
        }
        
        document.setTitle(dto.getTitle());
        document.setContent(dto.getContent());
        document.setFormData(dto.getFormData());
        document.setUrgency(dto.getUrgency());
        
        return documentRepository.save(document);
    }
    
    /**
     * 전체 승인된 결재 문서 수 조회
     */
    @Transactional(readOnly = true)
    public long getTotalApprovedCount() {
        return documentRepository.countByStatus(DocumentStatus.APPROVED);
    }
    
    /**
     * 결재선 제거 (기안 문서만 가능)
     */
    @Transactional
    public void clearApprovalLine(Long docId) {
        ApprovalDocument document = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));
        
        if (document.getStatus() != DocumentStatus.DRAFT) {
            throw new RuntimeException("기안 상태의 문서만 결재선을 제거할 수 있습니다");
        }
        
        // 기존 결재선 제거
        approvalLineRepository.deleteByDocument(document);
        
        // 문서의 결재선 리스트 초기화
        document.getApprovalLines().clear();
        document.setCurrentApprover(null);
        
        documentRepository.save(document);
        
        log.info("Approval line cleared for document: {}", document.getDocNumber());
    }
    
    public void setReferences(Long docId, List<Long> referenceUserIds) {
        ApprovalDocument document = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다"));
        
        // 기존 참조자 제거
        document.clearReferences();
        
        // 새 참조자 추가
        if (referenceUserIds != null && !referenceUserIds.isEmpty()) {
            for (int i = 0; i < referenceUserIds.size(); i++) {
                User user = userRepository.findById(referenceUserIds.get(i))
                        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
                
                ApprovalReference reference = ApprovalReference.builder()
                        .document(document)
                        .user(user)
                        .sortOrder(i + 1)
                        .isRead(false)
                        .build();
                
                document.addReference(reference);
            }
        }
        
        documentRepository.save(document);
    }
    
    @Transactional(readOnly = true)
    public List<ApprovalDocument> getReferencedDocuments(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        return documentRepository.findDocumentsByReferenceUser(user);
    }
}