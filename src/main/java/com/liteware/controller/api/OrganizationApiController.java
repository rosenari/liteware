package com.liteware.controller.api;

import com.liteware.model.entity.Department;
import com.liteware.model.entity.User;
import com.liteware.service.organization.DepartmentService;
import com.liteware.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/organization")
@RequiredArgsConstructor
public class OrganizationApiController {
    
    private final DepartmentService departmentService;
    private final UserService userService;
    
    /**
     * 부서 목록 조회
     */
    @GetMapping("/departments")
    public ResponseEntity<List<Department>> getDepartments() {
        List<Department> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(departments);
    }
    
    /**
     * 부서별 사용자 조회
     */
    @GetMapping("/departments/{deptId}/users")
    public ResponseEntity<List<User>> getDepartmentUsers(@PathVariable Long deptId) {
        List<User> users = userService.getUsersByDepartment(deptId);
        return ResponseEntity.ok(users);
    }
    
    /**
     * 조직도 전체 데이터 조회 (부서 + 사용자)
     */
    @GetMapping("/tree")
    public ResponseEntity<Map<String, Object>> getOrganizationTree() {
        Map<String, Object> result = new HashMap<>();
        
        List<Department> departments = departmentService.getAllDepartments();
        List<User> users = userService.getAllUsers();
        
        result.put("departments", departments);
        result.put("users", users);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 사용자 검색
     */
    @GetMapping("/users/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String keyword) {
        List<User> users = userService.searchUsers(keyword);
        return ResponseEntity.ok(users);
    }
}