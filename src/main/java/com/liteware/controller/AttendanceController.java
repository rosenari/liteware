package com.liteware.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Controller
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    
    @GetMapping
    public String attendance(Model model) {
        model.addAttribute("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        model.addAttribute("todayDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")));
        model.addAttribute("checkInTime", "09:00");
        model.addAttribute("checkOutTime", "-");
        model.addAttribute("workingHours", "8시간 30분");
        model.addAttribute("remainingLeave", 15);
        model.addAttribute("usedLeave", 2);
        
        return "attendance/index";
    }
    
    @PostMapping("/checkin")
    public String checkIn(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("success", "출근 처리되었습니다.");
        return "redirect:/attendance";
    }
    
    @PostMapping("/checkout")
    public String checkOut(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("success", "퇴근 처리되었습니다.");
        return "redirect:/attendance";
    }
}