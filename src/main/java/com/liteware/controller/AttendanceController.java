package com.liteware.controller;

import com.liteware.model.entity.Attendance;
import com.liteware.model.entity.User;
import com.liteware.repository.UserRepository;
import com.liteware.service.attendance.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    
    private final AttendanceService attendanceService;
    private final UserRepository userRepository;
    
    @GetMapping
    public String attendance(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        
        User currentUser = userRepository.findByLoginId(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        // 오늘의 근태 기록 조회
        Attendance todayAttendance = attendanceService.getTodayAttendance(currentUser.getUserId());
        
        model.addAttribute("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        model.addAttribute("todayDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
        
        if (todayAttendance != null) {
            model.addAttribute("checkInTime", 
                    todayAttendance.getCheckInTime() != null ? 
                    todayAttendance.getCheckInTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "-");
            model.addAttribute("checkOutTime", 
                    todayAttendance.getCheckOutTime() != null ? 
                    todayAttendance.getCheckOutTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "-");
            model.addAttribute("workingHours", todayAttendance.getFormattedWorkingHours());
            model.addAttribute("isCheckedIn", todayAttendance.isCheckedIn());
            model.addAttribute("isCheckedOut", todayAttendance.isCheckedOut());
        } else {
            model.addAttribute("checkInTime", "-");
            model.addAttribute("checkOutTime", "-");
            model.addAttribute("workingHours", "0시간 0분");
            model.addAttribute("isCheckedIn", false);
            model.addAttribute("isCheckedOut", false);
        }
        
        // 주간 근태 데이터
        List<Attendance> weeklyAttendance = attendanceService.getWeeklyAttendance(currentUser.getUserId());
        model.addAttribute("weeklyAttendance", weeklyAttendance);
        
        // 월간 근무 통계
        long monthlyAttendanceDays = attendanceService.getMonthlyAttendanceDays(currentUser.getUserId());
        
        model.addAttribute("monthlyWorkingDays", monthlyAttendanceDays);
        model.addAttribute("monthlyWorkingHours", attendanceService.getFormattedMonthlyWorkingHours(currentUser.getUserId()));
        model.addAttribute("monthlyAttendanceDays", monthlyAttendanceDays);
        
        // 연차 정보 (추후 실제 데이터로 교체)
        model.addAttribute("remainingLeave", 15);
        model.addAttribute("usedLeave", 2);
        
        return "attendance/index";
    }
    
    @PostMapping("/checkin")
    public String checkIn(@AuthenticationPrincipal UserDetails userDetails, 
                         RedirectAttributes redirectAttributes) {
        try {
            if (userDetails == null) {
                return "redirect:/login";
            }
            
            User currentUser = userRepository.findByLoginId(userDetails.getUsername()).orElse(null);
            if (currentUser == null) {
                return "redirect:/login";
            }
            
            attendanceService.checkIn(currentUser.getUserId());
            redirectAttributes.addFlashAttribute("success", "출근 처리되었습니다.");
        } catch (Exception e) {
            log.error("Check-in error", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/attendance";
    }
    
    @PostMapping("/checkout")
    public String checkOut(@AuthenticationPrincipal UserDetails userDetails,
                          RedirectAttributes redirectAttributes) {
        try {
            if (userDetails == null) {
                return "redirect:/login";
            }
            
            User currentUser = userRepository.findByLoginId(userDetails.getUsername()).orElse(null);
            if (currentUser == null) {
                return "redirect:/login";
            }
            
            attendanceService.checkOut(currentUser.getUserId());
            redirectAttributes.addFlashAttribute("success", "퇴근 처리되었습니다.");
        } catch (Exception e) {
            log.error("Check-out error", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/attendance";
    }
}