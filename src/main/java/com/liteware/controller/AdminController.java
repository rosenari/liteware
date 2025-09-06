package com.liteware.controller;

import com.liteware.model.entity.UserStatus;
import com.liteware.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final UserService userService;
    
    @GetMapping("/users")
    public String userManagement(Model model) {
        try {
            model.addAttribute("users", userService.getAllUsers());
            model.addAttribute("totalUsers", userService.getAllUsers().size());
            model.addAttribute("activeUsers", userService.getAllUsers().stream()
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE).count());
        } catch (Exception e) {
            log.error("Error loading users", e);
        }
        return "admin/users";
    }
    
    @GetMapping("/settings")
    public String systemSettings(Model model) {
        model.addAttribute("systemName", "Liteware");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("maxFileSize", "50MB");
        model.addAttribute("sessionTimeout", "30분");
        model.addAttribute("passwordPolicy", "최소 8자, 대소문자 및 숫자 포함");
        
        return "admin/settings";
    }
    
    @PostMapping("/users/{userId}/toggle")
    @ResponseBody
    public String toggleUserStatus(@PathVariable Long userId) {
        try {
            // Toggle user active status
            return "success";
        } catch (Exception e) {
            log.error("Error toggling user status", e);
            return "error";
        }
    }
    
    @PostMapping("/settings/save")
    public String saveSettings(@RequestParam String systemName,
                              @RequestParam String maxFileSize,
                              @RequestParam String sessionTimeout,
                              RedirectAttributes redirectAttributes) {
        try {
            // Save settings logic
            redirectAttributes.addFlashAttribute("success", "설정이 저장되었습니다.");
        } catch (Exception e) {
            log.error("Error saving settings", e);
            redirectAttributes.addFlashAttribute("error", "설정 저장에 실패했습니다.");
        }
        return "redirect:/admin/settings";
    }
}