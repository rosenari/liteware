package com.liteware.service.approval;

import com.liteware.model.dto.ApprovalDocumentDto;
import com.liteware.model.dto.ApprovalLineDto;
import com.liteware.model.entity.User;
import com.liteware.model.entity.approval.*;
import com.liteware.service.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApprovalServiceTest extends BaseServiceTest {
    
    @Autowired
    private ApprovalService approvalService;
    
    private User drafter;
    private User approver1;
    private User approver2;
    private ApprovalDocument document;
    
    @BeforeEach
    void setUp() {
        // 기안자와 결재자 생성
        drafter = createUser("drafter", "기안자", "drafter@example.com", department, position);
        approver1 = createUser("approver1", "결재자1", "approver1@example.com", department, position);
        approver2 = createUser("approver2", "결재자2", "approver2@example.com", department, position);
        
        drafter.addRole(userRole);
        approver1.addRole(userRole);
        approver2.addRole(userRole);
        
        userRepository.save(drafter);
        userRepository.save(approver1);
        userRepository.save(approver2);
    }
    
    @Test
    @DisplayName("문서 기안 성공")
    void draftDocument_Success() {
        // given
        ApprovalDocumentDto dto = new ApprovalDocumentDto();
        dto.setDocType(DocumentType.LEAVE_REQUEST);
        dto.setTitle("연차 신청");
        dto.setContent("개인 사유로 연차 신청합니다.");
        dto.setDrafterId(drafter.getUserId());
        dto.setUrgency(UrgencyType.NORMAL);
        
        // when
        ApprovalDocument document = approvalService.draftDocument(dto);
        
        // then
        assertThat(document).isNotNull();
        assertThat(document.getDocType()).isEqualTo(DocumentType.LEAVE_REQUEST);
        assertThat(document.getTitle()).isEqualTo("연차 신청");
        assertThat(document.getContent()).isEqualTo("개인 사유로 연차 신청합니다.");
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(document.getDrafter().getUserId()).isEqualTo(drafter.getUserId());
        assertThat(document.getDocNumber()).isNotNull();
    }
    
    @Test
    @DisplayName("존재하지 않는 기안자로 문서 기안 시 예외 발생")
    void draftDocument_NonExistentDrafter_ThrowsException() {
        // given
        ApprovalDocumentDto dto = new ApprovalDocumentDto();
        dto.setDocType(DocumentType.LEAVE_REQUEST);
        dto.setTitle("연차 신청");
        dto.setDrafterId(999999L);
        
        // when & then
        assertThatThrownBy(() -> approvalService.draftDocument(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("기안자를 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("결재선 설정 성공")
    void setApprovalLine_Success() {
        // given
        document = createTestDocument();
        
        ApprovalLineDto line1 = new ApprovalLineDto();
        line1.setApproverId(approver1.getUserId());
        line1.setApprovalType(ApprovalType.APPROVAL);
        line1.setOrderSeq(1);
        line1.setIsOptional(false);
        
        ApprovalLineDto line2 = new ApprovalLineDto();
        line2.setApproverId(approver2.getUserId());
        line2.setApprovalType(ApprovalType.APPROVAL);
        line2.setOrderSeq(2);
        line2.setIsOptional(false);
        
        List<ApprovalLineDto> lines = Arrays.asList(line1, line2);
        
        // when
        List<ApprovalLine> savedLines = approvalService.setApprovalLine(document.getDocId(), lines);
        
        // then
        assertThat(savedLines).hasSize(2);
        assertThat(savedLines.get(0).getApprover().getUserId()).isEqualTo(approver1.getUserId());
        assertThat(savedLines.get(0).getOrderSeq()).isEqualTo(1);
        assertThat(savedLines.get(1).getApprover().getUserId()).isEqualTo(approver2.getUserId());
        assertThat(savedLines.get(1).getOrderSeq()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("진행중인 문서에 결재선 설정 시 예외 발생")
    void setApprovalLine_NonDraftDocument_ThrowsException() {
        // given
        document = createTestDocument();
        document.setStatus(DocumentStatus.PENDING);
        document = entityManager.merge(document);
        
        ApprovalLineDto line = new ApprovalLineDto();
        line.setApproverId(approver1.getUserId());
        line.setApprovalType(ApprovalType.APPROVAL);
        line.setOrderSeq(1);
        
        // when & then
        assertThatThrownBy(() -> approvalService.setApprovalLine(document.getDocId(), Arrays.asList(line)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("임시저장 상태의 문서만 결재선을 설정할 수 있습니다");
    }
    
    @Test
    @DisplayName("문서 상신 성공")
    void submitDocument_Success() {
        // given
        document = createTestDocument();
        setApprovalLines(document);
        
        // when
        ApprovalDocument submittedDoc = approvalService.submitDocument(document.getDocId());
        
        // then
        assertThat(submittedDoc.getStatus()).isEqualTo(DocumentStatus.PENDING);
        assertThat(submittedDoc.getCurrentApprover().getUserId()).isEqualTo(approver1.getUserId());
    }
    
    @Test
    @DisplayName("결재선 없는 문서 상신 시 예외 발생")
    void submitDocument_NoApprovalLine_ThrowsException() {
        // given
        document = createTestDocument();
        
        // when & then
        assertThatThrownBy(() -> approvalService.submitDocument(document.getDocId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("결재선이 설정되지 않았습니다");
    }
    
    @Test
    @DisplayName("문서 승인 성공")
    void approveDocument_Success() {
        // given
        document = createTestDocument();
        setApprovalLines(document);
        document = approvalService.submitDocument(document.getDocId());
        
        // when
        ApprovalDocument approvedDoc = approvalService.approveDocument(
                document.getDocId(), 
                approver1.getUserId(), 
                "승인합니다"
        );
        
        // then
        assertThat(approvedDoc.getCurrentApprover().getUserId()).isEqualTo(approver2.getUserId());
        assertThat(approvedDoc.getStatus()).isEqualTo(DocumentStatus.PENDING);
    }
    
    @Test
    @DisplayName("최종 승인 후 문서 상태 완료")
    void approveDocument_FinalApproval_CompletesDocument() {
        // given
        document = createTestDocument();
        setApprovalLines(document);
        document = approvalService.submitDocument(document.getDocId());
        
        // 첫 번째 결재자 승인
        document = approvalService.approveDocument(document.getDocId(), approver1.getUserId(), "승인");
        
        // when - 두 번째(최종) 결재자 승인
        ApprovalDocument finalDoc = approvalService.approveDocument(
                document.getDocId(), 
                approver2.getUserId(), 
                "최종 승인"
        );
        
        // then
        assertThat(finalDoc.getStatus()).isEqualTo(DocumentStatus.APPROVED);
        assertThat(finalDoc.getCurrentApprover()).isNull();
        assertThat(finalDoc.getCompletedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("권한 없는 사용자가 문서 승인 시 예외 발생")
    void approveDocument_UnauthorizedUser_ThrowsException() {
        // given
        document = createTestDocument();
        setApprovalLines(document);
        document = approvalService.submitDocument(document.getDocId());
        
        // when & then - approver2가 순서가 아닌데 승인 시도
        assertThatThrownBy(() -> approvalService.approveDocument(
                document.getDocId(), 
                approver2.getUserId(), 
                "승인"
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("현재 결재 순서가 아닙니다");
    }
    
    @Test
    @DisplayName("문서 반려 성공")
    void rejectDocument_Success() {
        // given
        document = createTestDocument();
        setApprovalLines(document);
        document = approvalService.submitDocument(document.getDocId());
        
        // when
        ApprovalDocument rejectedDoc = approvalService.rejectDocument(
                document.getDocId(), 
                approver1.getUserId(), 
                "내용이 부족합니다"
        );
        
        // then
        assertThat(rejectedDoc.getStatus()).isEqualTo(DocumentStatus.REJECTED);
        assertThat(rejectedDoc.getCurrentApprover()).isNull();
        assertThat(rejectedDoc.getCompletedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("문서 회수 성공")
    void cancelDocument_Success() {
        // given
        document = createTestDocument();
        setApprovalLines(document);
        document = approvalService.submitDocument(document.getDocId());
        
        // when
        ApprovalDocument cancelledDoc = approvalService.cancelDocument(
                document.getDocId(), 
                drafter.getUserId()
        );
        
        // then
        assertThat(cancelledDoc.getStatus()).isEqualTo(DocumentStatus.CANCELLED);
        assertThat(cancelledDoc.getCurrentApprover()).isNull();
        assertThat(cancelledDoc.getCompletedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("기안자가 아닌 사용자가 문서 회수 시 예외 발생")
    void cancelDocument_NotDrafter_ThrowsException() {
        // given
        document = createTestDocument();
        setApprovalLines(document);
        document = approvalService.submitDocument(document.getDocId());
        
        // when & then
        assertThatThrownBy(() -> approvalService.cancelDocument(
                document.getDocId(), 
                approver1.getUserId()
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("기안자만 문서를 회수할 수 있습니다");
    }
    
    @Test
    @DisplayName("대기중인 문서 조회")
    void getPendingDocuments_Success() {
        // given
        document = createTestDocument();
        setApprovalLines(document);
        document = approvalService.submitDocument(document.getDocId());
        
        // when
        List<ApprovalDocument> pendingDocs = approvalService.getPendingDocuments(approver1.getUserId());
        
        // then
        assertThat(pendingDocs).hasSize(1);
        assertThat(pendingDocs.get(0).getDocId()).isEqualTo(document.getDocId());
    }
    
    @Test
    @DisplayName("기안한 문서 목록 조회")
    void getDraftedDocuments_Success() {
        // given
        createTestDocument();
        createTestDocument(); // 2개 생성
        
        // when
        List<ApprovalDocument> draftedDocs = approvalService.getDraftedDocuments(drafter.getUserId());
        
        // then
        assertThat(draftedDocs).hasSize(2);
        assertThat(draftedDocs).allMatch(doc -> doc.getDrafter().getUserId().equals(drafter.getUserId()));
    }
    
    @Test
    @DisplayName("참조자 설정 성공")
    void setReferences_Success() {
        // given
        document = createTestDocument();
        User ref1 = createUser("ref1", "참조자1", "ref1@example.com", department, position);
        User ref2 = createUser("ref2", "참조자2", "ref2@example.com", department, position);
        userRepository.save(ref1);
        userRepository.save(ref2);
        
        // when
        approvalService.setReferences(document.getDocId(), Arrays.asList(ref1.getUserId(), ref2.getUserId()));
        
        // then
        ApprovalDocument updatedDoc = approvalService.getDocument(document.getDocId());
        assertThat(updatedDoc.getReferences()).hasSize(2);
    }
    
    @Test
    @DisplayName("첨부파일 추가 성공")
    void addAttachment_Success() {
        // given
        document = createTestDocument();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test content".getBytes()
        );
        
        // when
        approvalService.addAttachment(document.getDocId(), file);
        
        // then
        ApprovalDocument updatedDoc = approvalService.getDocument(document.getDocId());
        assertThat(updatedDoc.getAttachments()).hasSize(1);
        assertThat(updatedDoc.getAttachments().get(0).getOriginalFileName()).isEqualTo("test.txt");
    }
    
    @Test
    @DisplayName("대기중인 문서 개수 조회")
    void countPendingDocuments_Success() {
        // given
        document = createTestDocument();
        setApprovalLines(document);
        approvalService.submitDocument(document.getDocId());
        
        // when
        Long count = approvalService.countPendingDocuments(approver1.getUserId());
        
        // then
        assertThat(count).isEqualTo(1L);
    }
    
    @Test
    @DisplayName("문서 검색 성공")
    void searchDocuments_Success() {
        // given
        ApprovalDocumentDto dto = new ApprovalDocumentDto();
        dto.setDocType(DocumentType.LEAVE_REQUEST);
        dto.setTitle("특별 연차 신청");
        dto.setContent("특별한 사유로 연차 신청합니다.");
        dto.setDrafterId(drafter.getUserId());
        approvalService.draftDocument(dto);
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // when
        Page<ApprovalDocument> results = approvalService.searchDocuments("특별", pageable);
        
        // then
        assertThat(results.getTotalElements()).isGreaterThan(0);
        assertThat(results.getContent()).anyMatch(doc -> doc.getTitle().contains("특별"));
    }
    
    @Test
    @DisplayName("결재 위임 성공")
    void delegateApproval_Success() {
        // given
        document = createTestDocument();
        setApprovalLines(document);
        document = approvalService.submitDocument(document.getDocId());
        
        User delegate = createUser("delegate", "대리결재자", "delegate@example.com", department, position);
        userRepository.save(delegate);
        
        // when
        approvalService.delegateApproval(
                approver1.getUserId(), 
                delegate.getUserId(),
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(7)
        );
        
        // then
        ApprovalDocument updatedDoc = approvalService.getDocument(document.getDocId());
        assertThat(updatedDoc.getCurrentApprover().getUserId()).isEqualTo(delegate.getUserId());
    }
    
    // Helper methods
    private ApprovalDocument createTestDocument() {
        ApprovalDocumentDto dto = new ApprovalDocumentDto();
        dto.setDocType(DocumentType.LEAVE_REQUEST);
        dto.setTitle("연차 신청");
        dto.setContent("개인 사유로 연차 신청합니다.");
        dto.setDrafterId(drafter.getUserId());
        dto.setUrgency(UrgencyType.NORMAL);
        
        return approvalService.draftDocument(dto);
    }
    
    private void setApprovalLines(ApprovalDocument doc) {
        ApprovalLineDto line1 = new ApprovalLineDto();
        line1.setApproverId(approver1.getUserId());
        line1.setApprovalType(ApprovalType.APPROVAL);
        line1.setOrderSeq(1);
        line1.setIsOptional(false);
        
        ApprovalLineDto line2 = new ApprovalLineDto();
        line2.setApproverId(approver2.getUserId());
        line2.setApprovalType(ApprovalType.APPROVAL);
        line2.setOrderSeq(2);
        line2.setIsOptional(false);
        
        approvalService.setApprovalLine(doc.getDocId(), Arrays.asList(line1, line2));
    }
}