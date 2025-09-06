package com.liteware.controller;

import com.liteware.model.entity.User;
import com.liteware.service.approval.ApprovalService;
import com.liteware.service.board.BoardService;
import com.liteware.service.auth.AuthService;
import com.liteware.repository.UserRepository;
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
    private final UserRepository userRepository;
    
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
        
        // 대시보드 데이터 조회 - 실제 로그인한 사용자 정보 사용
        User currentUser = userRepository.findByLoginId(userDetails.getUsername()).orElse(null);
        Long userId = currentUser != null ? currentUser.getUserId() : 1L;
        
        try {
            // 결재 관련 데이터
            model.addAttribute("pendingApprovals", approvalService.countPendingDocuments(userId));
            model.addAttribute("draftedDocuments", approvalService.getDraftedDocuments(userId).size());
            model.addAttribute("recentDocuments", approvalService.getRecentDocuments(userId, 5));
            
            // 게시판 관련 데이터
            model.addAttribute("recentBoards", boardService.getActiveBoards());
            model.addAttribute("notices", boardService.getRecentNotices(5));
            model.addAttribute("recentPosts", boardService.getRecentPosts(5));
            model.addAttribute("newPostsCount", boardService.countNewPostsToday());
            
            // 사용자 정보
            model.addAttribute("username", userDetails.getUsername());
            model.addAttribute("userId", userId);
            model.addAttribute("totalUsers", userRepository.count());
            
            // 오늘 날짜 (근태 및 일정 관련)
            model.addAttribute("today", java.time.LocalDate.now());
            model.addAttribute("currentTime", java.time.LocalTime.now());
            
            // 연차 관련 데이터 (추후 실제 로직으로 대체)
            model.addAttribute("remainingVacation", 15);
            model.addAttribute("monthlyUsedVacation", 2);
            
            // 시스템 현황 데이터 
            long totalUsers = userRepository.count();
            model.addAttribute("totalPosts", totalUsers > 0 ? 12 : 0); // 임시값
            model.addAttribute("completedApprovals", totalUsers > 0 ? 8 : 0); // 임시값  
            model.addAttribute("activeUsersCount", totalUsers);
            model.addAttribute("totalDepartments", totalUsers > 0 ? 4 : 0); // 임시값
            
        } catch (Exception e) {
            log.error("Dashboard data loading error", e);
            // 에러 발생시 빈 데이터라도 전달
            model.addAttribute("pendingApprovals", 0);
            model.addAttribute("draftedDocuments", 0);
            model.addAttribute("newPostsCount", 0L);
            model.addAttribute("totalUsers", 0);
            model.addAttribute("remainingVacation", 0);
            model.addAttribute("monthlyUsedVacation", 0);
            model.addAttribute("totalPosts", 0);
            model.addAttribute("completedApprovals", 0);
            model.addAttribute("activeUsersCount", 0);
            model.addAttribute("totalDepartments", 0);
        }
        
        return "dashboard/index";
    }
    
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        model.addAttribute("username", userDetails.getUsername());
        return "user/profile";
    }
}