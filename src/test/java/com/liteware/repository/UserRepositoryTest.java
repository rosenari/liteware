package com.liteware.repository;

import com.liteware.model.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private PositionRepository positionRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    private User user;
    private Department department;
    private Position position;
    private Role role;
    
    @BeforeEach
    void setUp() {
        department = Department.builder()
                .deptName("개발부")
                .deptCode("DEV")
                .deptLevel(1)
                .isActive(true)
                .build();
        department = departmentRepository.save(department);
        
        position = Position.builder()
                .positionName("대리")
                .positionCode("ASST_MGR")
                .positionLevel(3)
                .sortOrder(3)
                .build();
        position = positionRepository.save(position);
        
        role = Role.builder()
                .roleName("USER")
                .roleCode("ROLE_USER")
                .description("일반 사용자")
                .build();
        role = roleRepository.save(role);
        
        user = User.builder()
                .loginId("testuser")
                .password("encodedPassword")
                .name("테스트 사용자")
                .email("test@liteware.com")
                .phone("010-1234-5678")
                .status(UserStatus.ACTIVE)
                .department(department)
                .position(position)
                .hireDate(LocalDate.now())
                .build();
        user.addRole(role);
    }
    
    @Test
    @DisplayName("사용자를 저장할 수 있다")
    void saveUser() {
        User savedUser = userRepository.save(user);
        
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getUserId()).isNotNull();
        assertThat(savedUser.getLoginId()).isEqualTo("testuser");
    }
    
    @Test
    @DisplayName("로그인 ID로 사용자를 조회할 수 있다")
    void findByLoginId() {
        userRepository.save(user);
        
        Optional<User> foundUser = userRepository.findByLoginId("testuser");
        
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getLoginId()).isEqualTo("testuser");
    }
    
    @Test
    @DisplayName("이메일로 사용자를 조회할 수 있다")
    void findByEmail() {
        userRepository.save(user);
        
        Optional<User> foundUser = userRepository.findByEmail("test@liteware.com");
        
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@liteware.com");
    }
    
    @Test
    @DisplayName("부서별 사용자 목록을 조회할 수 있다")
    void findByDepartment() {
        userRepository.save(user);
        
        User user2 = User.builder()
                .loginId("testuser2")
                .password("encodedPassword")
                .name("테스트 사용자2")
                .email("test2@liteware.com")
                .department(department)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user2);
        
        List<User> users = userRepository.findByDepartment(department);
        
        assertThat(users).hasSize(2);
    }
    
    @Test
    @DisplayName("직급별 사용자 목록을 조회할 수 있다")
    void findByPosition() {
        userRepository.save(user);
        
        List<User> users = userRepository.findByPosition(position);
        
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getPosition().getPositionName()).isEqualTo("대리");
    }
    
    @Test
    @DisplayName("상태별 사용자 목록을 조회할 수 있다")
    void findByStatus() {
        userRepository.save(user);
        
        User inactiveUser = User.builder()
                .loginId("inactive")
                .password("encodedPassword")
                .name("비활성 사용자")
                .email("inactive@liteware.com")
                .status(UserStatus.INACTIVE)
                .build();
        userRepository.save(inactiveUser);
        
        List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);
        List<User> inactiveUsers = userRepository.findByStatus(UserStatus.INACTIVE);
        
        assertThat(activeUsers).hasSize(1);
        assertThat(inactiveUsers).hasSize(1);
    }
    
    @Test
    @DisplayName("이름으로 사용자를 검색할 수 있다")
    void findByNameContaining() {
        userRepository.save(user);
        
        List<User> users = userRepository.findByNameContaining("테스트");
        
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getName()).contains("테스트");
    }
    
    @Test
    @DisplayName("페이징 처리된 사용자 목록을 조회할 수 있다")
    void findAllWithPaging() {
        for (int i = 1; i <= 5; i++) {
            User u = User.builder()
                    .loginId("user" + i)
                    .password("password")
                    .name("사용자" + i)
                    .email("user" + i + "@liteware.com")
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(u);
        }
        
        Page<User> page = userRepository.findAll(PageRequest.of(0, 3));
        
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("삭제되지 않은 사용자만 조회할 수 있다")
    void findByIsDeletedFalse() {
        userRepository.save(user);
        
        User deletedUser = User.builder()
                .loginId("deleted")
                .password("password")
                .name("삭제된 사용자")
                .email("deleted@liteware.com")
                .status(UserStatus.INACTIVE)
                .isDeleted(true)
                .build();
        userRepository.save(deletedUser);
        
        List<User> activeUsers = userRepository.findByIsDeletedFalse();
        
        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getIsDeleted()).isFalse();
    }
    
    @Test
    @DisplayName("로그인 ID 중복 여부를 확인할 수 있다")
    void existsByLoginId() {
        userRepository.save(user);
        
        boolean exists = userRepository.existsByLoginId("testuser");
        boolean notExists = userRepository.existsByLoginId("nonexistent");
        
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
    
    @Test
    @DisplayName("이메일 중복 여부를 확인할 수 있다")
    void existsByEmail() {
        userRepository.save(user);
        
        boolean exists = userRepository.existsByEmail("test@liteware.com");
        boolean notExists = userRepository.existsByEmail("nonexistent@liteware.com");
        
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}