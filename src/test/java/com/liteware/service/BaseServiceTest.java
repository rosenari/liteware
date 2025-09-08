package com.liteware.service;

import com.liteware.model.entity.Department;
import com.liteware.model.entity.Position;
import com.liteware.model.entity.Role;
import com.liteware.model.entity.User;
import com.liteware.repository.DepartmentRepository;
import com.liteware.repository.PositionRepository;
import com.liteware.repository.RoleRepository;
import com.liteware.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;

/**
 * 서비스 통합 테스트를 위한 기본 클래스
 * H2 인메모리 데이터베이스를 사용하며, @Transactional로 각 테스트 후 자동 롤백
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class BaseServiceTest {
    
    @PersistenceContext
    protected EntityManager entityManager;
    
    @Autowired
    protected UserRepository userRepository;
    
    @Autowired
    protected DepartmentRepository departmentRepository;
    
    @Autowired
    protected PositionRepository positionRepository;
    
    @Autowired
    protected RoleRepository roleRepository;
    
    protected User testUser;
    protected User adminUser;
    protected Department department;
    protected Position position;
    protected Role userRole;
    protected Role adminRole;
    
    @BeforeEach
    protected void baseSetUp() {
        // 부서 생성
        department = createDepartment("DEV", "개발팀");
        
        // 직급 생성
        position = createPosition("STAFF", "사원", 1);
        
        // 역할 생성
        userRole = createRole("ROLE_USER", "일반사용자");
        adminRole = createRole("ROLE_ADMIN", "관리자");
        
        // 테스트 사용자 생성
        testUser = createUser("test001", "테스트사용자", "test@example.com", department, position);
        adminUser = createUser("admin001", "관리자", "admin@example.com", department, position);
    }
    
    protected Department createDepartment(String code, String name) {
        Department dept = new Department();
        dept.setDeptCode(code);
        dept.setDeptName(name);
        dept.setDeptLevel(1);
        dept.setSortOrder(1);
        dept.setIsActive(true);
        return departmentRepository.save(dept);
    }
    
    protected Position createPosition(String code, String name, int level) {
        Position pos = new Position();
        pos.setPositionCode(code);
        pos.setPositionName(name);
        pos.setPositionLevel(level);
        pos.setSortOrder(1);
        return positionRepository.save(pos);
    }
    
    protected Role createRole(String code, String name) {
        return roleRepository.findByRoleCode(code)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setRoleCode(code);
                    role.setRoleName(name);
                    role.setDescription(name);
                    return roleRepository.save(role);
                });
    }
    
    protected User createUser(String loginId, String name, String email, Department dept, Position pos) {
        User user = new User();
        user.setLoginId(loginId);
        user.setPassword("password123");
        user.setName(name);
        user.setEmail(email);
        user.setPhoneNumber("010-1234-5678");
        user.setDepartment(dept);
        user.setPosition(pos);
        user.setHireDate(LocalDate.now().minusYears(1));
        user.setActive(true);
        return userRepository.save(user);
    }
    
    protected User createUserWithHireDate(String loginId, String name, String email, LocalDate hireDate) {
        User user = new User();
        user.setLoginId(loginId);
        user.setPassword("password123");
        user.setName(name);
        user.setEmail(email);
        user.setPhoneNumber("010-1234-5678");
        user.setDepartment(department);
        user.setPosition(position);
        user.setHireDate(hireDate);
        user.setActive(true);
        return userRepository.save(user);
    }
}