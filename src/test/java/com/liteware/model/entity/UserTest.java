package com.liteware.model.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserTest {

    @Autowired
    private TestEntityManager entityManager;
    
    private Validator validator;
    private User user;
    
    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        user = User.builder()
                .loginId("testuser")
                .password("encodedPassword")
                .name("테스트 사용자")
                .email("test@liteware.com")
                .phone("010-1234-5678")
                .status(UserStatus.ACTIVE)
                .hireDate(LocalDate.now())
                .build();
    }
    
    @Test
    @DisplayName("User 엔티티가 정상적으로 생성되어야 한다")
    void createUser() {
        assertThat(user.getLoginId()).isEqualTo("testuser");
        assertThat(user.getName()).isEqualTo("테스트 사용자");
        assertThat(user.getEmail()).isEqualTo("test@liteware.com");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
    
    @Test
    @DisplayName("User 엔티티가 데이터베이스에 저장되어야 한다")
    void saveUser() {
        User savedUser = entityManager.persistAndFlush(user);
        
        assertThat(savedUser.getUserId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("필수 필드가 없으면 검증에 실패해야 한다")
    void validateRequiredFields() {
        User invalidUser = User.builder().build();
        
        Set<ConstraintViolation<User>> violations = validator.validate(invalidUser);
        
        assertThat(violations).isNotEmpty();
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("로그인 ID는 필수입니다", "이름은 필수입니다", "비밀번호는 필수입니다");
    }
    
    @Test
    @DisplayName("이메일 형식이 올바르지 않으면 검증에 실패해야 한다")
    void validateEmailFormat() {
        user.setEmail("invalid-email");
        
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        
        assertThat(violations).isNotEmpty();
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .contains("올바른 이메일 형식이 아닙니다");
    }
    
    @Test
    @DisplayName("User와 Department가 연관관계를 가져야 한다")
    void userDepartmentRelationship() {
        Department dept = Department.builder()
                .deptName("개발부")
                .deptCode("DEV")
                .deptLevel(1)
                .isActive(true)
                .build();
        
        Department savedDept = entityManager.persistAndFlush(dept);
        
        user.setDepartment(savedDept);
        User savedUser = entityManager.persistAndFlush(user);
        
        entityManager.clear();
        
        User foundUser = entityManager.find(User.class, savedUser.getUserId());
        assertThat(foundUser.getDepartment()).isNotNull();
        assertThat(foundUser.getDepartment().getDeptName()).isEqualTo("개발부");
    }
    
    @Test
    @DisplayName("User와 Position이 연관관계를 가져야 한다")
    void userPositionRelationship() {
        Position position = Position.builder()
                .positionName("대리")
                .positionCode("ASST_MGR")
                .positionLevel(3)
                .sortOrder(3)
                .build();
        
        Position savedPosition = entityManager.persistAndFlush(position);
        
        user.setPosition(savedPosition);
        User savedUser = entityManager.persistAndFlush(user);
        
        entityManager.clear();
        
        User foundUser = entityManager.find(User.class, savedUser.getUserId());
        assertThat(foundUser.getPosition()).isNotNull();
        assertThat(foundUser.getPosition().getPositionName()).isEqualTo("대리");
    }
    
    @Test
    @DisplayName("User 엔티티 업데이트 시 updatedAt이 변경되어야 한다")
    void updateUser() throws InterruptedException {
        User savedUser = entityManager.persistAndFlush(user);
        LocalDateTime originalUpdatedAt = savedUser.getUpdatedAt();
        
        Thread.sleep(100);
        
        savedUser.setName("변경된 이름");
        savedUser.setEmail("changed@liteware.com");
        entityManager.persistAndFlush(savedUser);
        
        assertThat(savedUser.getUpdatedAt()).isAfter(originalUpdatedAt);
    }
    
    @Test
    @DisplayName("User 상태를 변경할 수 있어야 한다")
    void changeUserStatus() {
        User savedUser = entityManager.persistAndFlush(user);
        
        savedUser.setStatus(UserStatus.INACTIVE);
        entityManager.persistAndFlush(savedUser);
        
        User foundUser = entityManager.find(User.class, savedUser.getUserId());
        assertThat(foundUser.getStatus()).isEqualTo(UserStatus.INACTIVE);
    }
}