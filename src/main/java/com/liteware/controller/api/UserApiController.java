package com.liteware.controller.api;

import com.liteware.model.dto.UserDto;
import com.liteware.model.entity.User;
import com.liteware.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserApiController {
    
    private final UserService userService;
    
    /**
     * 사용자 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<User>> getUsers(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String keyword) {
        
        List<User> users;
        
        if (departmentId != null) {
            users = userService.getUsersByDepartment(departmentId);
        } else if (keyword != null && !keyword.isEmpty()) {
            users = userService.searchUsers(keyword);
        } else {
            users = userService.getAllUsers();
        }
        
        return ResponseEntity.ok(users);
    }
    
    /**
     * 사용자 상세 조회
     */
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable Long userId) {
        try {
            User user = userService.getUserById(userId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("User not found: {}", userId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 사용자 정보 수정
     */
    @PutMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long userId,
            @RequestBody UserDto userDto) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User updatedUser = userService.updateUser(userId, userDto);
            response.put("success", true);
            response.put("message", "사용자 정보가 수정되었습니다.");
            response.put("user", updatedUser);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to update user", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 비밀번호 변경
     */
    @PostMapping("/{userId}/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @PathVariable Long userId,
            @RequestBody Map<String, String> passwordData) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String currentPassword = passwordData.get("currentPassword");
            String newPassword = passwordData.get("newPassword");
            
            userService.changePassword(userId, currentPassword, newPassword);
            
            response.put("success", true);
            response.put("message", "비밀번호가 변경되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to change password", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * 프로필 이미지 업로드
     */
    @PostMapping("/{userId}/profile-image")
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            @PathVariable Long userId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // TODO: 프로필 이미지 업로드 구현
            String imageUrl = "/uploads/profiles/" + file.getOriginalFilename();
            
            response.put("success", true);
            response.put("message", "프로필 이미지가 업로드되었습니다.");
            response.put("imageUrl", imageUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to upload profile image", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 사용자 활성화/비활성화
     */
    @PostMapping("/{userId}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleUserStatus(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userService.getUserById(userId);
            user.setActive(!user.isActive());
            userService.updateUser(userId, convertToDto(user));
            
            String status = user.isActive() ? "활성화" : "비활성화";
            response.put("success", true);
            response.put("message", "사용자가 " + status + "되었습니다.");
            response.put("isActive", user.isActive());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to toggle user status", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 조직도 데이터 조회
     */
    @GetMapping("/organization")
    public ResponseEntity<Map<String, Object>> getOrganizationData() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("users", userService.getAllUsers());
            response.put("departments", userService.getAllDepartments());
            response.put("positions", userService.getAllPositions());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get organization data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setDepartmentId(user.getDepartment() != null ? user.getDepartment().getDeptId() : null);
        dto.setPositionId(user.getPosition() != null ? user.getPosition().getPositionId() : null);
        return dto;
    }
}