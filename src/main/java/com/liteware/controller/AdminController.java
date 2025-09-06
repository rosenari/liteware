package com.liteware.controller;

import com.liteware.model.entity.User;
import com.liteware.model.entity.UserStatus;
import com.liteware.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final UserService userService;
    
    @GetMapping("/users")
    public String userManagement(Model model) {
        try {
            List<User> users = userService.getAllUsers();
            model.addAttribute("users", users);
            model.addAttribute("totalUsers", users.size());
            
            long activeUsers = users.stream()
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE).count();
            long inactiveUsers = users.stream()
                    .filter(u -> u.getStatus() != UserStatus.ACTIVE).count();
            long adminUsers = users.stream()
                    .filter(u -> u.getRoles().stream()
                            .anyMatch(r -> "ROLE_ADMIN".equals(r.getRoleCode()))).count();
            
            model.addAttribute("activeUsers", activeUsers);
            model.addAttribute("inactiveUsers", inactiveUsers);
            model.addAttribute("adminUsers", adminUsers);
            
            // 부서, 직급 목록 추가
            model.addAttribute("departments", userService.getAllDepartments());
            model.addAttribute("positions", userService.getAllPositions());
        } catch (Exception e) {
            log.error("Error loading users", e);
            model.addAttribute("users", List.of());
            model.addAttribute("totalUsers", 0);
            model.addAttribute("activeUsers", 0);
            model.addAttribute("inactiveUsers", 0);
            model.addAttribute("adminUsers", 0);
            model.addAttribute("departments", List.of());
            model.addAttribute("positions", List.of());
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
            userService.toggleUserStatus(userId);
            return "success";
        } catch (Exception e) {
            log.error("Error toggling user status", e);
            return "error";
        }
    }
    
    @PostMapping("/users")
    @ResponseBody
    public String createUser(@RequestBody com.liteware.model.dto.UserDto userDto) {
        try {
            userService.createUser(userDto);
            return "success";
        } catch (Exception e) {
            log.error("Error creating user", e);
            return "error";
        }
    }
    
    @GetMapping("/users/{userId}")
    @ResponseBody
    public com.liteware.model.dto.UserDto getUser(@PathVariable Long userId) {
        try {
            return userService.getUserDto(userId);
        } catch (Exception e) {
            log.error("Error getting user", e);
            throw new RuntimeException("User not found");
        }
    }
    
    @PutMapping("/users/{userId}")
    @ResponseBody
    public String updateUser(@PathVariable Long userId, @RequestBody com.liteware.model.dto.UserDto userDto) {
        try {
            userService.updateUser(userId, userDto);
            return "success";
        } catch (Exception e) {
            log.error("Error updating user", e);
            return "error";
        }
    }
    
    @DeleteMapping("/users/{userId}")
    @ResponseBody
    public String deleteUser(@PathVariable Long userId) {
        try {
            userService.deleteUser(userId);
            return "success";
        } catch (Exception e) {
            log.error("Error deleting user", e);
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