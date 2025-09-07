package com.liteware.service;

import com.liteware.model.dto.ApprovalDocumentDto;
import com.liteware.model.dto.ApprovalLineDto;
import com.liteware.model.entity.*;
import com.liteware.model.entity.approval.*;
import com.liteware.repository.UserRepository;
import com.liteware.repository.approval.ApprovalDocumentRepository;
import com.liteware.repository.approval.ApprovalLineRepository;
import com.liteware.service.approval.ApprovalService;
import com.liteware.service.approval.ApprovalWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {
    
    @Mock
    private ApprovalDocumentRepository documentRepository;
    
    @Mock
    private ApprovalLineRepository approvalLineRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ApprovalWorkflowService workflowService;
    
    @InjectMocks
    private ApprovalService approvalService;
    
    private User drafter;
    private User approver1;
    private User approver2;
    private ApprovalDocument document;
    private ApprovalDocumentDto documentDto;
    
    @BeforeEach
    void setUp() {
        drafter = User.builder()
                .userId(1L)
                .loginId("drafter")
                .name("기안자")
                .build();
        
        approver1 = User.builder()
                .userId(2L)
                .loginId("approver1")
                .name("결재자1")
                .build();
        
        approver2 = User.builder()
                .userId(3L)
                .loginId("approver2")
                .name("결재자2")
                .build();
        
        document = ApprovalDocument.builder()
                .docId(1L)
                .docNumber("DOC-2025-001")
                .docType(DocumentType.LEAVE_REQUEST)
                .title("연차 휴가 신청")
                .content("연차 휴가 신청합니다")
                .status(DocumentStatus.DRAFT)
                .drafter(drafter)
                .urgency(UrgencyType.NORMAL)
                .build();
        
        documentDto = ApprovalDocumentDto.builder()
                .docType(DocumentType.LEAVE_REQUEST)
                .title("연차 휴가 신청")
                .content("연차 휴가 신청합니다")
                .drafterId(1L)
                .urgency(UrgencyType.NORMAL)
                .build();
    }
    
    @Test
    @DisplayName("결재 문서를 기안할 수 있어야 한다")
    void draftDocument() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(drafter));
        when(documentRepository.save(any(ApprovalDocument.class))).thenAnswer(invocation -> {
            ApprovalDocument doc = invocation.getArgument(0);
            doc.setDocId(1L);
            doc.setDocNumber("DOC-2025-001");
            return doc;
        });
        
        ApprovalDocument drafted = approvalService.draftDocument(documentDto);
        
        assertThat(drafted).isNotNull();
        assertThat(drafted.getDocId()).isEqualTo(1L);
        assertThat(drafted.getTitle()).isEqualTo("연차 휴가 신청");
        assertThat(drafted.getStatus()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(drafted.getDrafter()).isEqualTo(drafter);
        
        verify(documentRepository).save(any(ApprovalDocument.class));
    }
    
    @Test
    @DisplayName("결재선을 설정할 수 있어야 한다")
    void setApprovalLine() {
        List<ApprovalLineDto> lineDtos = Arrays.asList(
                ApprovalLineDto.builder()
                        .approverId(2L)
                        .approvalType(ApprovalType.APPROVAL)
                        .orderSeq(1)
                        .build(),
                ApprovalLineDto.builder()
                        .approverId(3L)
                        .approvalType(ApprovalType.APPROVAL)
                        .orderSeq(2)
                        .build()
        );
        
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver1));
        when(userRepository.findById(3L)).thenReturn(Optional.of(approver2));
        when(approvalLineRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        List<ApprovalLine> lines = approvalService.setApprovalLine(1L, lineDtos);
        
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).getApprover()).isEqualTo(approver1);
        assertThat(lines.get(0).getOrderSeq()).isEqualTo(1);
        assertThat(lines.get(1).getApprover()).isEqualTo(approver2);
        assertThat(lines.get(1).getOrderSeq()).isEqualTo(2);
        
        verify(approvalLineRepository).saveAll(anyList());
    }
    
    @Test
    @DisplayName("문서를 상신할 수 있어야 한다")
    void submitDocument() {
        ApprovalLine line1 = ApprovalLine.builder()
                .lineId(1L)
                .document(document)
                .approver(approver1)
                .approvalType(ApprovalType.APPROVAL)
                .orderSeq(1)
                .status(ApprovalStatus.PENDING)
                .build();
        
        document.getApprovalLines().add(line1);
        
        when(documentRepository.findByIdWithApprovalLines(1L)).thenReturn(Optional.of(document));
        when(approvalLineRepository.findByDocumentAndOrderSeq(document, 1))
                .thenReturn(Optional.of(line1));
        when(documentRepository.save(any(ApprovalDocument.class))).thenReturn(document);
        
        ApprovalDocument submitted = approvalService.submitDocument(1L);
        
        assertThat(submitted.getStatus()).isEqualTo(DocumentStatus.PENDING);
        assertThat(submitted.getCurrentApprover()).isEqualTo(approver1);
        assertThat(submitted.getDraftedAt()).isNotNull();
        
        verify(documentRepository).save(document);
    }
    
    @Test
    @DisplayName("임시저장 상태가 아닌 문서는 상신할 수 없어야 한다")
    void cannotSubmitNonDraftDocument() {
        document.setStatus(DocumentStatus.PENDING);
        when(documentRepository.findByIdWithApprovalLines(1L)).thenReturn(Optional.of(document));
        
        assertThatThrownBy(() -> approvalService.submitDocument(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("임시저장 상태의 문서만 상신할 수 있습니다");
    }
    
    @Test
    @DisplayName("문서를 승인할 수 있어야 한다")
    void approveDocument() {
        document.setStatus(DocumentStatus.PENDING);
        document.setCurrentApprover(approver1);
        
        ApprovalLine line1 = ApprovalLine.builder()
                .lineId(1L)
                .document(document)
                .approver(approver1)
                .approvalType(ApprovalType.APPROVAL)
                .orderSeq(1)
                .status(ApprovalStatus.PENDING)
                .build();
        
        ApprovalLine line2 = ApprovalLine.builder()
                .lineId(2L)
                .document(document)
                .approver(approver2)
                .approvalType(ApprovalType.APPROVAL)
                .orderSeq(2)
                .status(ApprovalStatus.PENDING)
                .build();
        
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(approvalLineRepository.findByDocumentAndApprover(document, approver1))
                .thenReturn(Optional.of(line1));
        when(approvalLineRepository.findByDocumentAndOrderSeq(document, 2))
                .thenReturn(Optional.of(line2));
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver1));
        when(documentRepository.save(any(ApprovalDocument.class))).thenReturn(document);
        when(approvalLineRepository.save(any(ApprovalLine.class))).thenReturn(line1);
        
        ApprovalDocument approved = approvalService.approveDocument(1L, 2L, "승인합니다");
        
        assertThat(line1.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(line1.getComment()).isEqualTo("승인합니다");
        assertThat(line1.getApprovedAt()).isNotNull();
        assertThat(approved.getCurrentApprover()).isEqualTo(approver2);
        
        verify(approvalLineRepository).save(line1);
        verify(documentRepository).save(document);
    }
    
    @Test
    @DisplayName("최종 승인 시 문서 상태가 완료로 변경되어야 한다")
    void finalApprovalCompletesDocument() {
        document.setStatus(DocumentStatus.PENDING);
        document.setCurrentApprover(approver2);
        
        ApprovalLine line2 = ApprovalLine.builder()
                .lineId(2L)
                .document(document)
                .approver(approver2)
                .approvalType(ApprovalType.APPROVAL)
                .orderSeq(2)
                .status(ApprovalStatus.PENDING)
                .build();
        
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(approvalLineRepository.findByDocumentAndApprover(document, approver2))
                .thenReturn(Optional.of(line2));
        when(approvalLineRepository.findByDocumentAndOrderSeq(document, 3))
                .thenReturn(Optional.empty());
        when(userRepository.findById(3L)).thenReturn(Optional.of(approver2));
        when(documentRepository.save(any(ApprovalDocument.class))).thenReturn(document);
        when(approvalLineRepository.save(any(ApprovalLine.class))).thenReturn(line2);
        
        ApprovalDocument approved = approvalService.approveDocument(1L, 3L, "최종 승인");
        
        assertThat(approved.getStatus()).isEqualTo(DocumentStatus.APPROVED);
        assertThat(approved.getCompletedAt()).isNotNull();
        assertThat(approved.getCurrentApprover()).isNull();
        
        verify(documentRepository).save(document);
    }
    
    @Test
    @DisplayName("문서를 반려할 수 있어야 한다")
    void rejectDocument() {
        document.setStatus(DocumentStatus.PENDING);
        document.setCurrentApprover(approver1);
        
        ApprovalLine line1 = ApprovalLine.builder()
                .lineId(1L)
                .document(document)
                .approver(approver1)
                .approvalType(ApprovalType.APPROVAL)
                .orderSeq(1)
                .status(ApprovalStatus.PENDING)
                .build();
        
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(approvalLineRepository.findByDocumentAndApprover(document, approver1))
                .thenReturn(Optional.of(line1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver1));
        when(documentRepository.save(any(ApprovalDocument.class))).thenReturn(document);
        when(approvalLineRepository.save(any(ApprovalLine.class))).thenReturn(line1);
        
        ApprovalDocument rejected = approvalService.rejectDocument(1L, 2L, "내용 보완 필요");
        
        assertThat(rejected.getStatus()).isEqualTo(DocumentStatus.REJECTED);
        assertThat(line1.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(line1.getComment()).isEqualTo("내용 보완 필요");
        assertThat(rejected.getCompletedAt()).isNotNull();
        
        verify(approvalLineRepository).save(line1);
        verify(documentRepository).save(document);
    }
    
    @Test
    @DisplayName("현재 결재자가 아닌 사용자는 결재할 수 없어야 한다")
    void cannotApproveByNonCurrentApprover() {
        document.setStatus(DocumentStatus.PENDING);
        document.setCurrentApprover(approver1);
        
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(userRepository.findById(3L)).thenReturn(Optional.of(approver2));
        
        assertThatThrownBy(() -> approvalService.approveDocument(1L, 3L, "승인"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("현재 결재 순서가 아닙니다");
    }
    
    @Test
    @DisplayName("문서를 회수할 수 있어야 한다")
    void cancelDocument() {
        document.setStatus(DocumentStatus.PENDING);
        document.setDrafter(drafter);
        
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(userRepository.findById(1L)).thenReturn(Optional.of(drafter));
        when(documentRepository.save(any(ApprovalDocument.class))).thenReturn(document);
        
        ApprovalDocument cancelled = approvalService.cancelDocument(1L, 1L);
        
        assertThat(cancelled.getStatus()).isEqualTo(DocumentStatus.CANCELLED);
        assertThat(cancelled.getCompletedAt()).isNotNull();
        
        verify(documentRepository).save(document);
    }
    
    @Test
    @DisplayName("기안자만 문서를 회수할 수 있어야 한다")
    void onlyDrafterCanCancelDocument() {
        document.setStatus(DocumentStatus.PENDING);
        document.setDrafter(drafter);
        
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver1));
        
        assertThatThrownBy(() -> approvalService.cancelDocument(1L, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("기안자만 문서를 회수할 수 있습니다");
    }
    
    @Test
    @DisplayName("결재 대기 문서 목록을 조회할 수 있어야 한다")
    void getPendingDocuments() {
        List<ApprovalDocument> documents = Arrays.asList(document);
        
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver1));
        when(documentRepository.findByCurrentApproverAndStatus(approver1, DocumentStatus.PENDING))
                .thenReturn(documents);
        
        List<ApprovalDocument> result = approvalService.getPendingDocuments(2L);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(document);
    }
    
    @Test
    @DisplayName("기안한 문서 목록을 조회할 수 있어야 한다")
    void getDraftedDocuments() {
        List<ApprovalDocument> documents = Arrays.asList(document);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(drafter));
        when(documentRepository.findByDrafter(drafter)).thenReturn(documents);
        
        List<ApprovalDocument> result = approvalService.getDraftedDocuments(1L);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDrafter()).isEqualTo(drafter);
    }
    
    @Test
    @DisplayName("결재선을 제거할 수 있어야 한다")
    void clearApprovalLine() {
        ApprovalLine line1 = ApprovalLine.builder()
                .lineId(1L)
                .document(document)
                .approver(approver1)
                .build();
        
        document.getApprovalLines().add(line1);
        
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        doNothing().when(approvalLineRepository).deleteByDocument(document);
        when(documentRepository.save(any(ApprovalDocument.class))).thenReturn(document);
        
        approvalService.clearApprovalLine(1L);
        
        assertThat(document.getApprovalLines()).isEmpty();
        verify(approvalLineRepository).deleteByDocument(document);
        verify(documentRepository).save(document);
    }
    
    @Test
    @DisplayName("참조자를 설정할 수 있어야 한다")
    void setReferences() {
        List<Long> referenceUserIds = Arrays.asList(2L, 3L);
        
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver1));
        when(userRepository.findById(3L)).thenReturn(Optional.of(approver2));
        when(documentRepository.save(any(ApprovalDocument.class))).thenReturn(document);
        
        approvalService.setReferences(1L, referenceUserIds);
        
        assertThat(document.getReferences()).hasSize(2);
        verify(documentRepository).save(document);
    }
    
    @Test
    @DisplayName("참조된 문서를 조회할 수 있어야 한다")
    void getReferencedDocuments() {
        List<ApprovalDocument> documents = Arrays.asList(document);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(documentRepository.findDocumentsByReferenceUser(user)).thenReturn(documents);
        
        List<ApprovalDocument> result = approvalService.getReferencedDocuments(1L);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(document);
    }
    
    @Test
    @DisplayName("문서를 수정할 수 있어야 한다")
    void updateDocument() {
        ApprovalDocumentDto updateDto = ApprovalDocumentDto.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .urgency(UrgencyType.URGENT)
                .build();
        
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(ApprovalDocument.class))).thenReturn(document);
        
        ApprovalDocument updated = approvalService.updateDocument(1L, updateDto);
        
        assertThat(updated.getTitle()).isEqualTo("수정된 제목");
        assertThat(updated.getContent()).isEqualTo("수정된 내용");
        assertThat(updated.getUrgency()).isEqualTo(UrgencyType.URGENT);
        
        verify(documentRepository).save(document);
    }
    
    @Test
    @DisplayName("결재를 위임할 수 있어야 한다")
    void delegateApproval() {
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(7);
        
        ApprovalLine line1 = ApprovalLine.builder()
                .lineId(1L)
                .document(document)
                .approver(approver1)
                .status(ApprovalStatus.PENDING)
                .build();
        
        List<ApprovalLine> pendingLines = Arrays.asList(line1);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(approver1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(approver2));
        when(approvalLineRepository.findByApproverAndStatus(approver1, ApprovalStatus.PENDING))
                .thenReturn(pendingLines);
        when(approvalLineRepository.saveAll(anyList())).thenReturn(pendingLines);
        
        approvalService.delegateApproval(1L, 2L, startDate, endDate);
        
        assertThat(line1.getDelegatedTo()).isEqualTo(approver2);
        assertThat(line1.getDelegatedAt()).isNotNull();
        
        verify(approvalLineRepository).saveAll(pendingLines);
    }
}