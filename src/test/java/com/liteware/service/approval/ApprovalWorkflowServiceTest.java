package com.liteware.service.approval;

import com.liteware.model.dto.ApprovalDocumentDto;
import com.liteware.model.entity.User;
import com.liteware.model.entity.approval.*;
import com.liteware.service.BaseServiceTest;
import com.liteware.service.leave.AnnualLeaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApprovalWorkflowServiceTest extends BaseServiceTest {
    
    @Autowired
    private ApprovalWorkflowService approvalWorkflowService;
    
    @Autowired
    private ApprovalService approvalService;
    
    @MockBean
    private AnnualLeaveService annualLeaveService;
    
    private User drafter;
    private ApprovalDocument document;
    
    @BeforeEach
    void setUp() {
        drafter = createUser("drafter", "기안자", "drafter@example.com", department, position);
        drafter.addRole(userRole);
        userRepository.save(drafter);
    }
    
    @Test
    @DisplayName("연차 휴가 신청 승인 시 연차 차감")
    void onDocumentApproved_LeaveRequest_Annual() {
        // given
        document = createLeaveRequestDocument(LeaveRequest.LeaveType.ANNUAL);
        
        // when
        approvalWorkflowService.onDocumentApproved(document);
        
        // then
        verify(annualLeaveService, times(1)).useAnnualLeave(any(LeaveRequest.class));
    }
    
    @Test
    @DisplayName("병가 휴가 신청 승인 시 연차 차감하지 않음")
    void onDocumentApproved_LeaveRequest_Sick() {
        // given
        document = createLeaveRequestDocument(LeaveRequest.LeaveType.SICK);
        
        // when
        approvalWorkflowService.onDocumentApproved(document);
        
        // then
        verify(annualLeaveService, never()).useAnnualLeave(any(LeaveRequest.class));
    }
    
    @Test
    @DisplayName("연차 휴가 신청 반려 시 연차 복원")
    void onDocumentRejected_LeaveRequest_Annual() {
        // given
        document = createLeaveRequestDocument(LeaveRequest.LeaveType.ANNUAL);
        document.setStatus(DocumentStatus.REJECTED);
        
        // when
        approvalWorkflowService.onDocumentRejected(document, "일정 중복");
        
        // then
        verify(annualLeaveService, times(1)).restoreAnnualLeave(any(LeaveRequest.class));
    }
    
    @Test
    @DisplayName("연장근무 신청 승인 처리")
    void onDocumentApproved_OvertimeRequest() {
        // given
        document = createTestDocument(DocumentType.OVERTIME_REQUEST);
        
        // when
        approvalWorkflowService.onDocumentApproved(document);
        
        // then - 에러 없이 처리 완료
        verify(annualLeaveService, never()).useAnnualLeave(any());
    }
    
    @Test
    @DisplayName("경비 청구 승인 처리")
    void onDocumentApproved_ExpenseRequest() {
        // given
        document = createTestDocument(DocumentType.EXPENSE_REQUEST);
        
        // when
        approvalWorkflowService.onDocumentApproved(document);
        
        // then - 에러 없이 처리 완료
        verify(annualLeaveService, never()).useAnnualLeave(any());
    }
    
    @Test
    @DisplayName("구매 요청 승인 처리")
    void onDocumentApproved_PurchaseRequest() {
        // given
        document = createTestDocument(DocumentType.PURCHASE_REQUEST);
        
        // when
        approvalWorkflowService.onDocumentApproved(document);
        
        // then - 에러 없이 처리 완료
        verify(annualLeaveService, never()).useAnnualLeave(any());
    }
    
    @Test
    @DisplayName("일반 문서 승인 처리")
    void onDocumentApproved_GeneralDocument() {
        // given
        document = createTestDocument(DocumentType.GENERAL_APPROVAL);
        
        // when
        approvalWorkflowService.onDocumentApproved(document);
        
        // then - 에러 없이 처리 완료
        verify(annualLeaveService, never()).useAnnualLeave(any());
    }
    
    @Test
    @DisplayName("연차 신청 승인 처리 중 예외 발생")
    void onDocumentApproved_LeaveRequest_Exception() {
        // given
        document = createLeaveRequestDocument(LeaveRequest.LeaveType.ANNUAL);
        doThrow(new RuntimeException("연차 부족")).when(annualLeaveService).useAnnualLeave(any());
        
        // when & then
        try {
            approvalWorkflowService.onDocumentApproved(document);
        } catch (RuntimeException e) {
            verify(annualLeaveService, times(1)).useAnnualLeave(any(LeaveRequest.class));
        }
    }
    
    @Test
    @DisplayName("연차 신청 반려 처리 중 예외 발생 시 로그만 기록")
    void onDocumentRejected_LeaveRequest_Exception() {
        // given
        document = createLeaveRequestDocument(LeaveRequest.LeaveType.ANNUAL);
        document.setStatus(DocumentStatus.REJECTED);
        doThrow(new RuntimeException("복원 실패")).when(annualLeaveService).restoreAnnualLeave(any());
        
        // when - 예외가 발생해도 메서드는 정상 종료
        approvalWorkflowService.onDocumentRejected(document, "반려 사유");
        
        // then
        verify(annualLeaveService, times(1)).restoreAnnualLeave(any(LeaveRequest.class));
    }
    
    // Helper methods
    private ApprovalDocument createTestDocument(DocumentType type) {
        ApprovalDocumentDto dto = new ApprovalDocumentDto();
        dto.setDocType(type);
        dto.setTitle("테스트 문서");
        dto.setContent("테스트 내용");
        dto.setDrafterId(drafter.getUserId());
        dto.setUrgency(UrgencyType.NORMAL);
        
        return approvalService.draftDocument(dto);
    }
    
    private ApprovalDocument createLeaveRequestDocument(LeaveRequest.LeaveType leaveType) {
        ApprovalDocument doc = createTestDocument(DocumentType.LEAVE_REQUEST);
        
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setDocument(doc);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStartDate(LocalDate.now());
        leaveRequest.setEndDate(LocalDate.now().plusDays(1));
        leaveRequest.setReason("개인 사유");
        leaveRequest.setLeaveDays(2);
        
        doc.setLeaveRequest(leaveRequest);
        
        return doc;
    }
}