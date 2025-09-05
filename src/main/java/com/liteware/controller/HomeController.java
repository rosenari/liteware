package com.liteware.controller;

import com.liteware.model.entity.User;
import com.liteware.service.approval.ApprovalService;
import com.liteware.service.board.BoardService;
import com.liteware.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final ApprovalService approvalService;
    private final BoardService boardService;
    
    @GetMapping("/")
    public String index(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        return "redirect:/dashboard";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        
        // 대시보드 데이터 조회
        // TODO: 실제 사용자 ID로 조회하도록 수정 필요
        Long userId = 1L; // getUserIdFromUserDetails(userDetails);
        
        try {
            model.addAttribute("pendingApprovals", approvalService.countPendingDocuments(userId));
            model.addAttribute("draftedDocuments", approvalService.getDraftedDocuments(userId).size());
            model.addAttribute("recentBoards", boardService.getActiveBoards());
            // model.addAttribute("notices", boardService.getRecentNotices());
        } catch (Exception e) {
            log.error("Dashboard data loading error", e);
        }
        
        model.addAttribute("username", userDetails.getUsername());
        
        return "dashboard/index";
    }
    
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("username", userDetails.getUsername());
        return "user/profile";
    }
}