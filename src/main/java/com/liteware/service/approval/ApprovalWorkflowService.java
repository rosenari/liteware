package com.liteware.service.approval;

import com.liteware.model.entity.approval.ApprovalDocument;
import com.liteware.model.entity.approval.DocumentStatus;
import com.liteware.model.entity.approval.DocumentType;
import com.liteware.model.entity.approval.LeaveRequest;
import com.liteware.service.leave.AnnualLeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalWorkflowService {
    
    private final AnnualLeaveService annualLeaveService;
    
    public void onDocumentApproved(ApprovalDocument document) {
        log.info("Processing approved document: {}", document.getDocNumber());
        
        switch (document.getDocType()) {
            case LEAVE_REQUEST:
                processLeaveRequest(document);
                break;
            case OVERTIME_REQUEST:
                processOvertimeRequest(document);
                break;
            case EXPENSE_REQUEST:
                processExpenseRequest(document);
                break;
            case PURCHASE_REQUEST:
                processPurchaseRequest(document);
                break;
            default:
                log.info("No specific workflow for document type: {}", document.getDocType());
        }
    }
    
    public void onDocumentRejected(ApprovalDocument document, String reason) {
        log.info("Processing rejected document: {} - Reason: {}", document.getDocNumber(), reason);
        
        // 휴가 신청이 반려된 경우 연차 복원
        if (document.getDocType() == DocumentType.LEAVE_REQUEST) {
            processRejectedLeaveRequest(document, reason);
        }
        
        // 기타 반려 시 처리 로직
        // 예: 알림 발송, 상태 업데이트 등
    }
    
    private void processRejectedLeaveRequest(ApprovalDocument document, String reason) {
        try {
            LeaveRequest leaveRequest = document.getLeaveRequest();
            if (leaveRequest != null && leaveRequest.getLeaveType() == LeaveRequest.LeaveType.ANNUAL) {
                // 연차 복원 (이미 차감했던 경우만)
                if (document.getStatus() == DocumentStatus.REJECTED) {
                    annualLeaveService.restoreAnnualLeave(leaveRequest);
                    log.info("Annual leave restored for rejected leave request: {}", document.getDocNumber());
                }
            }
        } catch (Exception e) {
            log.error("Error processing rejected leave request: {}", document.getDocNumber(), e);
        }
    }
    
    private void processLeaveRequest(ApprovalDocument document) {
        log.info("Processing leave request: {}", document.getDocNumber());
        
        try {
            // LeaveRequest 조회
            LeaveRequest leaveRequest = document.getLeaveRequest();
            if (leaveRequest != null && leaveRequest.getLeaveType() == LeaveRequest.LeaveType.ANNUAL) {
                // 연차 차감
                annualLeaveService.useAnnualLeave(leaveRequest);
                log.info("Annual leave deducted for approved leave request: {}", document.getDocNumber());
            }
        } catch (Exception e) {
            log.error("Error processing leave request: {}", document.getDocNumber(), e);
            throw new RuntimeException("휴가 승인 처리 중 오류가 발생했습니다.", e);
        }
    }
    
    private void processOvertimeRequest(ApprovalDocument document) {
        log.info("Processing overtime request: {}", document.getDocNumber());
        // 연장근무 신청 승인 후처리
        // 예: 근태 기록 업데이트, 수당 계산 등
    }
    
    private void processExpenseRequest(ApprovalDocument document) {
        log.info("Processing expense request: {}", document.getDocNumber());
        // 경비 청구 승인 후처리
        // 예: 경비 지급 처리, 회계 시스템 연동 등
    }
    
    private void processPurchaseRequest(ApprovalDocument document) {
        log.info("Processing purchase request: {}", document.getDocNumber());
        // 구매 요청 승인 후처리
        // 예: 구매 프로세스 시작, 공급업체 연락 등
    }
}