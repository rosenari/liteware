package com.liteware.service;

import com.liteware.model.entity.User;
import com.liteware.model.entity.approval.ApprovalDocument;
import com.liteware.model.entity.approval.DocumentStatus;
import com.liteware.model.entity.approval.DocumentType;
import com.liteware.model.entity.approval.LeaveRequest;
import com.liteware.service.approval.ApprovalWorkflowService;
import com.liteware.service.leave.AnnualLeaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalWorkflowServiceTest {
    
    @Mock
    private AnnualLeaveService annualLeaveService;
    
    @InjectMocks
    private ApprovalWorkflowService approvalWorkflowService;
    
    private ApprovalDocument document;
    private LeaveRequest leaveRequest;
    private User user;
    
    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(1L)
                .loginId("test001")
                .name("홍길동")
                .build();
        
        document = ApprovalDocument.builder()
                .documentId(100L)
                .docNumber("DOC-2025-0001")
                .docType(DocumentType.LEAVE_REQUEST)
                .drafter(user)
                .status(DocumentStatus.APPROVED)
                .build();
        
        leaveRequest = LeaveRequest.builder()
                .requestId(1L)
                .document(document)
                .leaveType(LeaveRequest.LeaveType.ANNUAL)
                .startDate(LocalDate.of(2025, 3, 1))
                .endDate(LocalDate.of(2025, 3, 3))
                .leaveDays(3.0)
                .reason("개인 사유")
                .build();
        
        document.setLeaveRequest(leaveRequest);
    }
    
    @Test
    @DisplayName("휴가 신청 승인 시 연차가 차감되어야 한다")
    void onDocumentApproved_LeaveRequest_Annual() {
        approvalWorkflowService.onDocumentApproved(document);
        
        verify(annualLeaveService).useAnnualLeave(leaveRequest);
    }
    
    @Test
    @DisplayName("병가 신청 승인 시 연차가 차감되지 않아야 한다")
    void onDocumentApproved_LeaveRequest_Sick() {
        leaveRequest.setLeaveType(LeaveRequest.LeaveType.SICK);
        
        approvalWorkflowService.onDocumentApproved(document);
        
        verify(annualLeaveService, never()).useAnnualLeave(any());
    }
    
    @Test
    @DisplayName("휴가 신청이 없는 문서 승인 시 오류가 발생하지 않아야 한다")
    void onDocumentApproved_LeaveRequest_Null() {
        document.setLeaveRequest(null);
        
        assertThatCode(() -> approvalWorkflowService.onDocumentApproved(document))
                .doesNotThrowAnyException();
        
        verify(annualLeaveService, never()).useAnnualLeave(any());
    }
    
    @Test
    @DisplayName("연장근무 신청 승인 시 정상 처리되어야 한다")
    void onDocumentApproved_OvertimeRequest() {
        document.setDocType(DocumentType.OVERTIME_REQUEST);
        
        assertThatCode(() -> approvalWorkflowService.onDocumentApproved(document))
                .doesNotThrowAnyException();
        
        verify(annualLeaveService, never()).useAnnualLeave(any());
    }
    
    @Test
    @DisplayName("경비 청구 승인 시 정상 처리되어야 한다")
    void onDocumentApproved_ExpenseRequest() {
        document.setDocType(DocumentType.EXPENSE_REQUEST);
        
        assertThatCode(() -> approvalWorkflowService.onDocumentApproved(document))
                .doesNotThrowAnyException();
        
        verify(annualLeaveService, never()).useAnnualLeave(any());
    }
    
    @Test
    @DisplayName("구매 요청 승인 시 정상 처리되어야 한다")
    void onDocumentApproved_PurchaseRequest() {
        document.setDocType(DocumentType.PURCHASE_REQUEST);
        
        assertThatCode(() -> approvalWorkflowService.onDocumentApproved(document))
                .doesNotThrowAnyException();
        
        verify(annualLeaveService, never()).useAnnualLeave(any());
    }
    
    @Test
    @DisplayName("기본 문서 타입 승인 시 정상 처리되어야 한다")
    void onDocumentApproved_DefaultType() {
        document.setDocType(DocumentType.GENERAL);
        
        assertThatCode(() -> approvalWorkflowService.onDocumentApproved(document))
                .doesNotThrowAnyException();
        
        verify(annualLeaveService, never()).useAnnualLeave(any());
    }
    
    @Test
    @DisplayName("휴가 신청 반려 시 연차가 복원되어야 한다")
    void onDocumentRejected_LeaveRequest_Annual() {
        document.setStatus(DocumentStatus.REJECTED);
        
        approvalWorkflowService.onDocumentRejected(document, "서류 미비");
        
        verify(annualLeaveService).restoreAnnualLeave(leaveRequest);
    }
    
    @Test
    @DisplayName("병가 신청 반려 시 연차가 복원되지 않아야 한다")
    void onDocumentRejected_LeaveRequest_Sick() {
        leaveRequest.setLeaveType(LeaveRequest.LeaveType.SICK);
        document.setStatus(DocumentStatus.REJECTED);
        
        approvalWorkflowService.onDocumentRejected(document, "서류 미비");
        
        verify(annualLeaveService, never()).restoreAnnualLeave(any());
    }
    
    @Test
    @DisplayName("승인된 문서를 반려 처리해도 연차가 복원되지 않아야 한다")
    void onDocumentRejected_AlreadyApproved() {
        document.setStatus(DocumentStatus.APPROVED);
        
        approvalWorkflowService.onDocumentRejected(document, "서류 미비");
        
        verify(annualLeaveService, never()).restoreAnnualLeave(any());
    }
    
    @Test
    @DisplayName("휴가 신청이 없는 문서 반려 시 오류가 발생하지 않아야 한다")
    void onDocumentRejected_LeaveRequest_Null() {
        document.setLeaveRequest(null);
        document.setStatus(DocumentStatus.REJECTED);
        
        assertThatCode(() -> approvalWorkflowService.onDocumentRejected(document, "서류 미비"))
                .doesNotThrowAnyException();
        
        verify(annualLeaveService, never()).restoreAnnualLeave(any());
    }
    
    @Test
    @DisplayName("연장근무 신청 반려 시 정상 처리되어야 한다")
    void onDocumentRejected_OvertimeRequest() {
        document.setDocType(DocumentType.OVERTIME_REQUEST);
        document.setStatus(DocumentStatus.REJECTED);
        
        assertThatCode(() -> approvalWorkflowService.onDocumentRejected(document, "인원 초과"))
                .doesNotThrowAnyException();
        
        verify(annualLeaveService, never()).restoreAnnualLeave(any());
    }
    
    @Test
    @DisplayName("연차 차감 중 오류 발생 시 예외가 전파되어야 한다")
    void onDocumentApproved_LeaveRequest_Exception() {
        doThrow(new RuntimeException("연차 부족")).when(annualLeaveService).useAnnualLeave(leaveRequest);
        
        assertThatThrownBy(() -> approvalWorkflowService.onDocumentApproved(document))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("휴가 승인 처리 중 오류가 발생했습니다");
    }
    
    @Test
    @DisplayName("연차 복원 중 오류 발생 시 예외가 로깅되고 처리가 계속되어야 한다")
    void onDocumentRejected_LeaveRequest_RestoreException() {
        document.setStatus(DocumentStatus.REJECTED);
        doThrow(new RuntimeException("복원 오류")).when(annualLeaveService).restoreAnnualLeave(leaveRequest);
        
        // 예외가 발생해도 메서드는 정상 완료되어야 함
        assertThatCode(() -> approvalWorkflowService.onDocumentRejected(document, "서류 미비"))
                .doesNotThrowAnyException();
        
        verify(annualLeaveService).restoreAnnualLeave(leaveRequest);
    }
    
    @Test
    @DisplayName("특별휴가 승인 시 연차가 차감되지 않아야 한다")
    void onDocumentApproved_LeaveRequest_Special() {
        leaveRequest.setLeaveType(LeaveRequest.LeaveType.SPECIAL);
        
        approvalWorkflowService.onDocumentApproved(document);
        
        verify(annualLeaveService, never()).useAnnualLeave(any());
    }
    
    @Test
    @DisplayName("공가 승인 시 연차가 차감되지 않아야 한다")
    void onDocumentApproved_LeaveRequest_Official() {
        leaveRequest.setLeaveType(LeaveRequest.LeaveType.OTHER);
        
        approvalWorkflowService.onDocumentApproved(document);
        
        verify(annualLeaveService, never()).useAnnualLeave(any());
    }
}