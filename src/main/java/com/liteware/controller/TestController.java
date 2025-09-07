package com.liteware.controller;

import com.liteware.model.entity.User;
import com.liteware.model.entity.Department;
import com.liteware.model.entity.Position;
import com.liteware.model.entity.Role;
import com.liteware.model.entity.UserStatus;
import com.liteware.repository.UserRepository;
import com.liteware.repository.DepartmentRepository;
import com.liteware.repository.PositionRepository;
import com.liteware.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class TestController {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    
    @GetMapping("/api/test/create-test-user")
    @ResponseBody
    public String createTestUser() {
        // Check if test user already exists
        if (userRepository.findByLoginId("test").isPresent()) {
            return "Test user already exists";
        }
        
        // Get or create role
        Role userRole = roleRepository.findByRoleCode("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("User role not found"));
            
        // Get department and position
        Department dept = departmentRepository.findByDeptCode("DEV")
            .orElseThrow(() -> new RuntimeException("Development department not found"));
            
        Position position = positionRepository.findByPositionCode("STAFF")
            .orElseThrow(() -> new RuntimeException("Staff position not found"));
        
        // Create test user
        User testUser = new User();
        testUser.setLoginId("test");
        testUser.setPassword(passwordEncoder.encode("test123"));
        testUser.setName("테스트 사용자");
        testUser.setEmail("test@liteware.com");
        testUser.setPhone("010-0000-0000");
        testUser.setDepartment(dept);
        testUser.setPosition(position);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setHireDate(LocalDate.now());
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        testUser.setRoles(roles);
        
        userRepository.save(testUser);
        
        return "Test user created successfully - Login with test/test123";
    }
}