package com.liteware.service.approval;

import com.liteware.model.entity.approval.ApprovalDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalWorkflowService {
    
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
        
        // 반려 시 처리 로직
        // 예: 알림 발송, 상태 업데이트 등
    }
    
    private void processLeaveRequest(ApprovalDocument document) {
        log.info("Processing leave request: {}", document.getDocNumber());
        // 휴가 신청 승인 후처리
        // 예: 휴가 일수 차감, 캘린더 업데이트 등
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