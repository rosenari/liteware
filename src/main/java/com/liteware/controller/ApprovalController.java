package com.liteware.controller;

import com.liteware.model.dto.ApprovalDocumentDto;
import com.liteware.model.dto.ApprovalLineDto;
import com.liteware.model.entity.approval.ApprovalDocument;
import com.liteware.model.entity.approval.DocumentType;
import com.liteware.model.entity.approval.UrgencyType;
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

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/approval")
@RequiredArgsConstructor
public class ApprovalController {
    
    private final ApprovalService approvalService;
    private final DepartmentService departmentService;
    
    @GetMapping
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
    public String draftForm(Model model) {
        model.addAttribute("documentTypes", DocumentType.values());
        model.addAttribute("urgencyTypes", UrgencyType.values());
        model.addAttribute("departments", departmentService.getAllDepartments());
        
        return "approval/draft";
    }
    
    @PostMapping("/draft")
    public String draft(@ModelAttribute ApprovalDocumentDto documentDto,
                       @RequestParam(required = false) List<Long> approverIds,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttributes) {
        try {
            documentDto.setDrafterId(1L); // TODO: Get from userDetails
            
            ApprovalDocument document = approvalService.draftDocument(documentDto);
            
            // 결재선 설정
            if (approverIds != null && !approverIds.isEmpty()) {
                List<ApprovalLineDto> lines = new java.util.ArrayList<>();
                for (int i = 0; i < approverIds.size(); i++) {
                    lines.add(ApprovalLineDto.builder()
                            .approverId(approverIds.get(i))
                            .approvalType(com.liteware.model.entity.approval.ApprovalType.APPROVAL)
                            .orderSeq(i + 1)
                            .build());
                }
                approvalService.setApprovalLine(document.getDocId(), lines);
            }
            
            redirectAttributes.addFlashAttribute("success", "문서가 저장되었습니다.");
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
    
    private boolean checkCanApprove(ApprovalDocument document, UserDetails userDetails) {
        // TODO: 실제 승인 권한 체크 로직 구현
        return document.getCurrentApprover() != null;
    }
}