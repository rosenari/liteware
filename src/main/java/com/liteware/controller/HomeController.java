package com.liteware.controller;

import com.liteware.model.entity.User;
import com.liteware.service.approval.ApprovalService;
import com.liteware.service.board.BoardService;
import com.liteware.service.auth.AuthService;
import com.liteware.service.attendance.AttendanceService;
import com.liteware.repository.UserRepository;
import com.liteware.repository.DepartmentRepository;
import com.liteware.model.entity.Attendance;
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
    private final AttendanceService attendanceService;
    private final DepartmentRepository departmentRepository;
    
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
        
        // 관리자 권한 확인
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        
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
            
            // 오늘 날짜 (근태 및 일정 관련)
            model.addAttribute("today", java.time.LocalDate.now());
            model.addAttribute("currentTime", java.time.LocalTime.now());
            
            // 연차 관련 데이터 (추후 실제 로직으로 대체)
            model.addAttribute("remainingVacation", 15);
            model.addAttribute("monthlyUsedVacation", 2);
            
            // 근태 관련 데이터
            Attendance todayAttendance = attendanceService.getTodayAttendance(userId);
            model.addAttribute("todayAttendance", todayAttendance);
            model.addAttribute("monthlyWorkingDays", attendanceService.getMonthlyWorkingDays(userId));
            model.addAttribute("monthlyWorkingHours", attendanceService.getFormattedMonthlyWorkingHours(userId));
            
            // 출근/퇴근 상태
            if (todayAttendance != null) {
                model.addAttribute("isCheckedIn", todayAttendance.isCheckedIn());
                model.addAttribute("isCheckedOut", todayAttendance.isCheckedOut());
                model.addAttribute("todayWorkingHours", todayAttendance.getFormattedWorkingHours());
            } else {
                model.addAttribute("isCheckedIn", false);
                model.addAttribute("isCheckedOut", false);
                model.addAttribute("todayWorkingHours", "0시간 0분");
            }
            
            // 관리자 전용 데이터
            if (isAdmin) {
                long totalUsers = userRepository.count();
                long totalDepartments = departmentRepository.count();
                
                model.addAttribute("totalUsers", totalUsers);
                model.addAttribute("totalDepartments", totalDepartments);
                
                // 게시글 수 (실제 데이터)
                try {
                    long totalPosts = boardService.getTotalPostsCount();
                    model.addAttribute("totalPosts", totalPosts);
                } catch (Exception e) {
                    log.warn("Failed to get total posts count", e);
                    model.addAttribute("totalPosts", 0L);
                }
                
                // 완료된 결재 수 (실제 데이터)
                try {
                    long completedApprovals = approvalService.getTotalApprovedCount();
                    model.addAttribute("completedApprovals", completedApprovals);
                } catch (Exception e) {
                    log.warn("Failed to get completed approvals count", e);
                    model.addAttribute("completedApprovals", 0L);
                }
                
                // 활성 사용자 수
                try {
                    long activeUsersCount = userRepository.countByStatus(com.liteware.model.entity.UserStatus.ACTIVE);
                    model.addAttribute("activeUsersCount", activeUsersCount);
                } catch (Exception e) {
                    log.warn("Failed to get active users count", e);
                    model.addAttribute("activeUsersCount", totalUsers);
                }
            }
            
        } catch (Exception e) {
            log.error("Dashboard data loading error", e);
            // 에러 발생시 빈 데이터라도 전달
            model.addAttribute("pendingApprovals", 0);
            model.addAttribute("draftedDocuments", 0);
            model.addAttribute("newPostsCount", 0L);
            model.addAttribute("remainingVacation", 0);
            model.addAttribute("monthlyUsedVacation", 0);
            model.addAttribute("monthlyWorkingDays", 0L);
            model.addAttribute("monthlyWorkingHours", "0시간 0분");
            model.addAttribute("isCheckedIn", false);
            model.addAttribute("isCheckedOut", false);
            model.addAttribute("todayWorkingHours", "0시간 0분");
            
            // 관리자 전용 에러 데이터
            if (isAdmin) {
                model.addAttribute("totalUsers", 0);
                model.addAttribute("totalPosts", 0);
                model.addAttribute("completedApprovals", 0);
                model.addAttribute("activeUsersCount", 0);
                model.addAttribute("totalDepartments", 0);
            }
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