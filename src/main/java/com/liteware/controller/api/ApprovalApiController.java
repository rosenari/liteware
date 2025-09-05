package com.liteware.controller.api;

import com.liteware.model.dto.ApprovalDocumentDto;
import com.liteware.model.entity.approval.ApprovalDocument;
import com.liteware.model.entity.approval.ApprovalLine;
import com.liteware.model.entity.approval.ApprovalStatus;
import com.liteware.model.entity.approval.DocumentStatus;
import com.liteware.service.approval.ApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/approval")
@RequiredArgsConstructor
public class ApprovalApiController {
    
    private final ApprovalService approvalService;
    
    /**
     * 결재 문서 생성
     */
    @PostMapping
    public ResponseEntity<ApprovalDocument> createDocument(@RequestBody ApprovalDocumentDto dto) {
        try {
            ApprovalDocument document = approvalService.createDocument(dto);
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            log.error("Failed to create approval document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 결재 문서 목록 조회
     */
    @GetMapping
    public ResponseEntity<Page<ApprovalDocument>> getDocuments(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String type,
            Pageable pageable) {
        
        Page<ApprovalDocument> documents;
        
        if ("pending".equals(type) && userId != null) {
            documents = approvalService.getPendingDocuments(userId, pageable);
        } else if ("drafted".equals(type) && userId != null) {
            documents = approvalService.getDraftedDocuments(userId, pageable);
        } else {
            documents = approvalService.getAllDocuments(pageable);
        }
        
        return ResponseEntity.ok(documents);
    }
    
    /**
     * 결재 문서 상세 조회
     */
    @GetMapping("/{docId}")
    public ResponseEntity<ApprovalDocument> getDocument(@PathVariable Long docId) {
        try {
            ApprovalDocument document = approvalService.getDocument(docId);
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            log.error("Document not found: {}", docId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 결재 처리 (승인/반려)
     */
    @PostMapping("/{docId}/process")
    public ResponseEntity<Map<String, Object>> processApproval(
            @PathVariable Long docId,
            @RequestParam Long approverId,
            @RequestParam ApprovalStatus status,
            @RequestParam(required = false) String comment) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (status == ApprovalStatus.APPROVED) {
                approvalService.approveDocument(docId, approverId, comment);
                response.put("success", true);
                response.put("message", "문서가 승인되었습니다.");
            } else if (status == ApprovalStatus.REJECTED) {
                approvalService.rejectDocument(docId, approverId, comment);
                response.put("success", true);
                response.put("message", "문서가 반려되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "잘못된 상태값입니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to process approval", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 결재 문서 취소
     */
    @PostMapping("/{docId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelDocument(
            @PathVariable Long docId,
            @RequestParam Long userId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            approvalService.cancelDocument(docId, userId);
            response.put("success", true);
            response.put("message", "문서가 취소되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to cancel document", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 결재선 조회
     */
    @GetMapping("/{docId}/lines")
    public ResponseEntity<?> getApprovalLines(@PathVariable Long docId) {
        try {
            ApprovalDocument document = approvalService.getDocument(docId);
            return ResponseEntity.ok(document.getApprovalLines());
        } catch (Exception e) {
            log.error("Failed to get approval lines", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 통계 정보 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(@RequestParam Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            stats.put("pendingCount", approvalService.getPendingDocuments(userId, Pageable.unpaged()).getTotalElements());
            stats.put("draftedCount", approvalService.getDraftedDocuments(userId, Pageable.unpaged()).getTotalElements());
            stats.put("approvedCount", approvalService.getApprovedCount(userId));
            stats.put("rejectedCount", approvalService.getRejectedCount(userId));
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}