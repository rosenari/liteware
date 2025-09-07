package com.liteware.controller;

import com.google.common.collect.Sets;
import com.liteware.model.dto.ApprovalDocumentDto;
import com.liteware.model.dto.ApprovalLineDto;
import com.liteware.model.entity.approval.*;
import com.liteware.service.approval.ApprovalService;
import com.liteware.service.organization.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/approval")
@RequiredArgsConstructor
public class ApprovalController {
    
    private final ApprovalService approvalService;
    private final DepartmentService departmentService;
    
    @GetMapping({"", "/list"})
    public String list(@RequestParam(defaultValue = "pending") String type,
                       @AuthenticationPrincipal UserDetails userDetails,
                       Model model) {
        
        Long userId = 1L; // TODO: Get from userDetails
        List<ApprovalDocument> documents;
        
        switch (type) {
            case "pending":
                documents = approvalService.getPendingDocuments(userId);
                model.addAttribute("title", "결재 대기");
                break;
            case "drafted":
                documents = approvalService.getDraftedDocuments(userId);
                model.addAttribute("title", "기안 문서");
                break;
            case "approved":
                documents = approvalService.getApprovedDocumentsByUser(userId);
                model.addAttribute("title", "결재 완료");
                break;
            default:
                documents = approvalService.getPendingDocuments(userId);
                model.addAttribute("title", "결재 대기");
        }
        
        model.addAttribute("documents", documents);
        model.addAttribute("type", type);
        
        return "approval/list";
    }
    
    @GetMapping("/draft")
    public String draftForm(@RequestParam(required = false) Long id,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
        model.addAttribute("documentTypes", DocumentType.values());
        model.addAttribute("urgencyTypes", UrgencyType.values());
        model.addAttribute("departments", departmentService.getAllDepartments());
        
        // 수정 모드인 경우 기존 데이터 로드
        if (id != null) {
            try {
                ApprovalDocument document = approvalService.getDocument(id);
                
                // 권한 확인 (기안자 본인만 수정 가능)
                Long userId = 1L; // TODO: Get from userDetails
                if (!document.getDrafter().getUserId().equals(userId)) {
                    return "redirect:/approval/" + id;
                }
                
                // DRAFT 상태인 경우만 수정 가능
                if (document.getStatus() != com.liteware.model.entity.approval.DocumentStatus.DRAFT) {
                    return "redirect:/approval/" + id;
                }
                
                model.addAttribute("document", document);
                model.addAttribute("isEdit", true);
                
                // JavaScript 직렬화를 위한 간단한 데이터 구조 생성
                // 결재선 데이터를 간단한 맵으로 변환
                List<java.util.Map<String, Object>> approvalLinesData = new ArrayList<>();
                for (ApprovalLine line : document.getApprovalLines()) {
                    java.util.Map<String, Object> lineData = new java.util.HashMap<>();
                    java.util.Map<String, Object> approverData = new java.util.HashMap<>();
                    approverData.put("userId", line.getApprover().getUserId());
                    approverData.put("name", line.getApprover().getName());
                    approverData.put("deptName", line.getApprover().getDepartment() != null ? 
                        line.getApprover().getDepartment().getDeptName() : "");
                    approverData.put("positionName", line.getApprover().getPosition() != null ? 
                        line.getApprover().getPosition().getPositionName() : "");
                    lineData.put("approver", approverData);
                    lineData.put("orderSeq", line.getOrderSeq());
                    approvalLinesData.add(lineData);
                }
                model.addAttribute("approvalLinesJson", approvalLinesData);
                
                // 참조자 데이터를 간단한 맵으로 변환
                List<java.util.Map<String, Object>> referencesData = new ArrayList<>();
                for (ApprovalReference ref : document.getReferences()) {
                    java.util.Map<String, Object> refData = new java.util.HashMap<>();
                    java.util.Map<String, Object> userData = new java.util.HashMap<>();
                    userData.put("userId", ref.getUser().getUserId());
                    userData.put("name", ref.getUser().getName());
                    userData.put("deptName", ref.getUser().getDepartment() != null ? 
                        ref.getUser().getDepartment().getDeptName() : "");
                    userData.put("positionName", ref.getUser().getPosition() != null ? 
                        ref.getUser().getPosition().getPositionName() : "");
                    refData.put("user", userData);
                    referencesData.add(refData);
                }
                model.addAttribute("referencesJson", referencesData);
                
                // 문서 타입별 관련 데이터 로드
                if (document.getDocType() == DocumentType.LEAVE_REQUEST) {
                    // TODO: 휴가신청서 데이터 로드
                    // LeaveRequest leaveRequest = leaveRequestRepository.findByDocument(document);
                    // model.addAttribute("leaveRequest", leaveRequest);
                }
            } catch (Exception e) {
                log.error("Failed to load document for edit: {}", id, e);
                return "redirect:/approval";
            }
        }
        
        return "approval/draft";
    }
    
    @PostMapping("/draft")
    public String draft(@ModelAttribute("documentDto") ApprovalDocumentDto documentDto,
                       @RequestParam(required = false) Long docId,  // 수정 모드일 때 문서 ID
                       @RequestParam(required = false) List<Long> approverIds,
                       @RequestParam(required = false) List<Long> referenceIds,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime,
                       @RequestParam(required = false, defaultValue = "false") Boolean isHourlyLeave,
                       @RequestParam(value = "attachments", required = false) List<org.springframework.web.multipart.MultipartFile> files,
                       @RequestParam(value = "action", defaultValue = "draft") String action,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttributes) {
        try {
            ApprovalDocument document;
            
            if (docId != null) {
                // 수정 모드
                document = approvalService.getDocument(docId);
                
                // 권한 확인
                Long userId = 1L; // TODO: Get from userDetails
                if (!document.getDrafter().getUserId().equals(userId)) {
                    redirectAttributes.addFlashAttribute("error", "문서 수정 권한이 없습니다.");
                    return "redirect:/approval/" + docId;
                }
                
                // 상태 확인
                if (document.getStatus() != com.liteware.model.entity.approval.DocumentStatus.DRAFT) {
                    redirectAttributes.addFlashAttribute("error", "기안 상태의 문서만 수정 가능합니다.");
                    return "redirect:/approval/" + docId;
                }
                
                // 기존 문서 업데이트
                document.setTitle(documentDto.getTitle());
                document.setContent(documentDto.getContent());
                document.setDocType(documentDto.getDocType());
                document.setUrgency(documentDto.getUrgency());
                
                // 휴가 신청서인 경우 추가 데이터 처리
                if (documentDto.getDocType() == DocumentType.LEAVE_REQUEST) {
                    java.util.Map<String, Object> leaveData = new java.util.HashMap<>();
                    leaveData.put("startTime", startTime);
                    leaveData.put("endTime", endTime);
                    leaveData.put("isHourlyLeave", isHourlyLeave);
                    
                    document.setFormData(new com.fasterxml.jackson.databind.ObjectMapper()
                            .writeValueAsString(leaveData));
                }
                
                document = approvalService.updateDocument(document);
                
            } else {
                // 신규 작성 모드
                documentDto.setDrafterId(1L); // TODO: Get from userDetails
                
                // 휴가 신청서인 경우 추가 데이터 처리
                if (documentDto.getDocType() == DocumentType.LEAVE_REQUEST) {
                    java.util.Map<String, Object> leaveData = new java.util.HashMap<>();
                    leaveData.put("startTime", startTime);
                    leaveData.put("endTime", endTime);
                    leaveData.put("isHourlyLeave", isHourlyLeave);
                    
                    // formData에 JSON 형태로 저장
                    documentDto.setFormData(new com.fasterxml.jackson.databind.ObjectMapper()
                            .writeValueAsString(leaveData));
                }
                
                document = approvalService.draftDocument(documentDto);
            }
            
            // 결재선 설정
            if (approverIds != null && !approverIds.isEmpty()) {
                // approverId 중복 제거
                HashSet<Long> setApproverIds = Sets.newHashSet(approverIds);
                approverIds = setApproverIds.stream().toList();
                List<ApprovalLineDto> lines = new java.util.ArrayList<>();
                for (int i = 0; i < approverIds.size(); i++) {
                    lines.add(ApprovalLineDto.builder()
                            .approverId(approverIds.get(i))
                            .approvalType(com.liteware.model.entity.approval.ApprovalType.APPROVAL)
                            .orderSeq(i + 1)
                            .build());
                }
                approvalService.setApprovalLine(document.getDocId(), lines);
            } else {
                // 결재선이 비어있으면 기존 결재선 제거
                approvalService.clearApprovalLine(document.getDocId());
            }
            
            // 참조자 설정
            if (referenceIds != null && !referenceIds.isEmpty()) {
                approvalService.setReferences(document.getDocId(), referenceIds);
            } else {
                // 참조자가 비어있으면 기존 참조자 제거
                approvalService.setReferences(document.getDocId(), new ArrayList<>());
            }
            
            // 첨부파일 처리 (추후 구현)
            if (files != null && !files.isEmpty()) {
                // TODO: 파일 저장 및 연결 로직 구현
                log.info("Attachments received: {} files", files.size());
            }
            
            // action 파라미터에 따라 처리 분기
            if ("submit".equals(action)) {
                // 상신: 상태를 PENDING으로 변경
                approvalService.submitDocument(document.getDocId());
                redirectAttributes.addFlashAttribute("success", "문서가 상신되었습니다.");
            } else {
                // 기안 저장: DRAFT 상태 유지
                redirectAttributes.addFlashAttribute("success", "문서가 기안 저장되었습니다.");
            }
            
            return "redirect:/approval/" + document.getDocId();
            
        } catch (Exception e) {
            log.error("Document draft error", e);
            redirectAttributes.addFlashAttribute("error", "문서 작성 중 오류가 발생했습니다.");
            return "redirect:/approval/draft";
        }
    }
    
    @GetMapping("/{id}")
    public String view(@PathVariable Long id,
                      @AuthenticationPrincipal UserDetails userDetails,
                      Model model) {
        try {
            ApprovalDocument document = approvalService.getDocument(id);
            model.addAttribute("document", document);
            model.addAttribute("canApprove", checkCanApprove(document, userDetails));
            
            return "approval/view";
        } catch (Exception e) {
            log.error("Document view error", e);
            return "redirect:/approval";
        }
    }
    
    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id,
                        RedirectAttributes redirectAttributes) {
        try {
            approvalService.submitDocument(id);
            redirectAttributes.addFlashAttribute("success", "문서가 상신되었습니다.");
        } catch (Exception e) {
            log.error("Document submit error", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/approval/" + id;
    }
    
    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                         @RequestParam(required = false) String comment,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        try {
            Long userId = 1L; // TODO: Get from userDetails
            approvalService.approveDocument(id, userId, comment);
            redirectAttributes.addFlashAttribute("success", "승인되었습니다.");
        } catch (Exception e) {
            log.error("Document approve error", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/approval/" + id;
    }
    
    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                        @RequestParam String reason,
                        @AuthenticationPrincipal UserDetails userDetails,
                        RedirectAttributes redirectAttributes) {
        try {
            Long userId = 1L; // TODO: Get from userDetails
            approvalService.rejectDocument(id, userId, reason);
            redirectAttributes.addFlashAttribute("success", "반려되었습니다.");
        } catch (Exception e) {
            log.error("Document reject error", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/approval/" + id;
    }
    
    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                        @AuthenticationPrincipal UserDetails userDetails,
                        RedirectAttributes redirectAttributes) {
        try {
            Long userId = 1L; // TODO: Get from userDetails
            approvalService.cancelDocument(id, userId);
            redirectAttributes.addFlashAttribute("success", "문서가 회수되었습니다.");
        } catch (Exception e) {
            log.error("Document cancel error", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/approval";
    }
    
    @PostMapping("/{id}/updateApprovalLine")
    @ResponseBody
    public java.util.Map<String, Object> updateApprovalLine(@PathVariable Long id,
                                                            @RequestBody java.util.Map<String, Object> request,
                                                            @AuthenticationPrincipal UserDetails userDetails) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        
        try {
            Long userId = 1L; // TODO: Get from userDetails
            
            // 문서 조회 및 권한 확인
            ApprovalDocument document = approvalService.getDocument(id);
            if (!document.getDrafter().getUserId().equals(userId)) {
                response.put("success", false);
                response.put("message", "문서 수정 권한이 없습니다.");
                return response;
            }
            
            if (document.getStatus() != com.liteware.model.entity.approval.DocumentStatus.DRAFT) {
                response.put("success", false);
                response.put("message", "기안 상태의 문서만 결재선 수정이 가능합니다.");
                return response;
            }
            
            // 결재선 업데이트
            @SuppressWarnings("unchecked")
            List<Object> rawApproverIds = (List<Object>) request.get("approverIds");
            List<Long> approverIds = new ArrayList<>();
            
            if (rawApproverIds != null) {
                for (Object approveId : rawApproverIds) {
                    if (approveId instanceof Number) {
                        approverIds.add(((Number) approveId).longValue());
                    } else if (approveId instanceof String) {
                        approverIds.add(Long.parseLong((String) approveId));
                    }
                }
            }
            
            log.info("Document {} - Received approverIds: {}", id, approverIds);
            
            if (!approverIds.isEmpty()) {
                List<ApprovalLineDto> lines = new java.util.ArrayList<>();
                for (int i = 0; i < approverIds.size(); i++) {
                    lines.add(ApprovalLineDto.builder()
                            .approverId(approverIds.get(i))
                            .approvalType(com.liteware.model.entity.approval.ApprovalType.APPROVAL)
                            .orderSeq(i + 1)
                            .build());
                }
                approvalService.setApprovalLine(id, lines);
            } else {
                // 결재선 제거
                approvalService.clearApprovalLine(id);
            }
            
            response.put("success", true);
            response.put("message", "결재선이 수정되었습니다.");
            log.info("Document {} - Approval line updated successfully", id);
            
        } catch (Exception e) {
            log.error("Update approval line error", e);
            response.put("success", false);
            response.put("message", "결재선 수정 중 오류가 발생했습니다.");
        }
        
        return response;
    }
    
    private boolean checkCanApprove(ApprovalDocument document, UserDetails userDetails) {
        // TODO: 실제 승인 권한 체크 로직 구현
        return document.getCurrentApprover() != null;
    }
}