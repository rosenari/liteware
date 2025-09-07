package com.liteware.common;

import com.liteware.service.approval.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {
    
    private final ApprovalService approvalService;
    
    @ModelAttribute("pendingApprovalCount")
    public Long getPendingApprovalCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            // TODO: Get actual user ID from authentication
            // For now, using hardcoded user ID 1 (current logged-in user)
            Long userId = 1L;
            return approvalService.countPendingDocuments(userId);
        }
        return 0L;
    }
}