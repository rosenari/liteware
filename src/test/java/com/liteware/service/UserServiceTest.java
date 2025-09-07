package com.liteware.service;

import com.liteware.model.dto.UserDto;
import com.liteware.model.entity.Department;
import com.liteware.model.entity.Position;
import com.liteware.model.entity.User;
import com.liteware.model.entity.Role;
import com.liteware.repository.DepartmentRepository;
import com.liteware.repository.PositionRepository;
import com.liteware.repository.RoleRepository;
import com.liteware.repository.UserRepository;
import com.liteware.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private DepartmentRepository departmentRepository;
    
    @Mock
    private PositionRepository positionRepository;
    
    @Mock
    private RoleRepository roleRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;
    
    private User user;
    private Department department;
    private Position position;
    private Role role;
    private UserDto userDto;
    
    @BeforeEach
    void setUp() {
        department = Department.builder()
                .deptId(1L)
                .deptName("개발팀")
                .deptCode("DEV")
                .build();
        
        position = Position.builder()
                .positionId(1L)
                .positionName("대리")
                .positionCode("STAFF")
                .build();
        
        role = Role.builder()
                .roleId(1L)
                .roleName("사용자")
                .roleCode("ROLE_USER")
                .build();
        
        user = User.builder()
                .userId(1L)
                .loginId("test001")
                .name("홍길동")
                .email("test@example.com")
                .phone("010-1234-5678")
                .department(department)
                .position(position)
                .password("encodedPassword")
                .active(true)
                .build();
        
        userDto = new UserDto();
        userDto.setLoginId("test001");
        userDto.setName("홍길동");
        userDto.setEmail("test@example.com");
        userDto.setPhoneNumber("010-1234-5678");
        userDto.setDepartmentId(1L);
        userDto.setPositionId(1L);
        userDto.setPassword("password123");
    }
    
    @Test
    @DisplayName("모든 사용자를 조회할 수 있어야 한다")
    void getAllUsers() {
        List<User> users = Arrays.asList(user);
        when(userRepository.findAll()).thenReturn(users);
        
        List<User> result = userService.getAllUsers();
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(user);
        verify(userRepository).findAll();
    }
    
    @Test
    @DisplayName("ID로 사용자를 조회할 수 있어야 한다")
    void getUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        User result = userService.getUserById(1L);
        
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("홍길동");
    }
    
    @Test
    @DisplayName("부서별 사용자를 조회할 수 있어야 한다")
    void getUsersByDepartment() {
        List<User> users = Arrays.asList(user);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(userRepository.findByDepartment(department)).thenReturn(users);
        
        List<User> result = userService.getUsersByDepartment(1L);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDepartment()).isEqualTo(department);
    }
    
    @Test
    @DisplayName("키워드로 사용자를 검색할 수 있어야 한다")
    void searchUsers() {
        List<User> users = Arrays.asList(user);
        when(userRepository.findByNameContainingOrLoginIdContainingOrEmailContaining("홍", "홍", "홍"))
                .thenReturn(users);
        
        List<User> result = userService.searchUsers("홍");
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).contains("홍");
    }
    
    @Test
    @DisplayName("사용자 정보를 수정할 수 있어야 한다")
    void updateUser() {
        UserDto updateDto = new UserDto();
        updateDto.setName("김철수");
        updateDto.setEmail("kim@example.com");
        updateDto.setPhoneNumber("010-9876-5432");
        updateDto.setDepartmentId(2L);
        updateDto.setPositionId(2L);
        
        Department newDept = Department.builder()
                .deptId(2L)
                .deptName("기획팀")
                .build();
        
        Position newPos = Position.builder()
                .positionId(2L)
                .positionName("과장")
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(newDept));
        when(positionRepository.findById(2L)).thenReturn(Optional.of(newPos));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        User updated = userService.updateUser(1L, updateDto);
        
        assertThat(updated.getName()).isEqualTo("김철수");
        assertThat(updated.getEmail()).isEqualTo("kim@example.com");
        verify(userRepository).save(user);
    }
    
    @Test
    @DisplayName("비밀번호를 변경할 수 있어야 한다")
    void changePassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("currentPassword", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        userService.changePassword(1L, "currentPassword", "newPassword");
        
        assertThat(user.getPassword()).isEqualTo("encodedNewPassword");
        assertThat(user.getPasswordChangedAt()).isNotNull();
        verify(userRepository).save(user);
    }
    
    @Test
    @DisplayName("잘못된 현재 비밀번호로는 변경할 수 없어야 한다")
    void changePasswordWithWrongCurrentPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", user.getPassword())).thenReturn(false);
        
        assertThatThrownBy(() -> userService.changePassword(1L, "wrongPassword", "newPassword"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("현재 비밀번호가 일치하지 않습니다");
    }
    
    @Test
    @DisplayName("모든 부서를 조회할 수 있어야 한다")
    void getAllDepartments() {
        List<Department> departments = Arrays.asList(department);
        when(departmentRepository.findAll()).thenReturn(departments);
        
        List<Department> result = userService.getAllDepartments();
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(department);
    }
    
    @Test
    @DisplayName("모든 직급을 조회할 수 있어야 한다")
    void getAllPositions() {
        List<Position> positions = Arrays.asList(position);
        when(positionRepository.findAll()).thenReturn(positions);
        
        List<Position> result = userService.getAllPositions();
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(position);
    }
    
    @Test
    @DisplayName("새로운 사용자를 생성할 수 있어야 한다")
    void createUser() {
        when(userRepository.existsByLoginId("test001")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));
        when(roleRepository.findByRoleCode("ROLE_USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setUserId(1L);
            return savedUser;
        });
        
        User created = userService.createUser(userDto);
        
        assertThat(created).isNotNull();
        assertThat(created.getLoginId()).isEqualTo("test001");
        assertThat(created.getName()).isEqualTo("홍길동");
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    @DisplayName("중복된 로그인 ID로는 사용자를 생성할 수 없어야 한다")
    void cannotCreateUserWithDuplicateLoginId() {
        when(userRepository.existsByLoginId("test001")).thenReturn(true);
        
        assertThatThrownBy(() -> userService.createUser(userDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 사용중인 로그인 ID입니다");
    }
    
    @Test
    @DisplayName("사용자를 삭제할 수 있어야 한다")
    void deleteUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        userService.deleteUser(1L);
        
        assertThat(user.getIsDeleted()).isTrue();
        verify(userRepository).save(user);
    }
    
    @Test
    @DisplayName("사용자 상태를 토글할 수 있어야 한다")
    void toggleUserStatus() {
        user.setActive(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        userService.toggleUserStatus(1L);
        
        assertThat(user.isActive()).isFalse();
        verify(userRepository).save(user);
    }
    
    @Test
    @DisplayName("사용자 DTO를 조회할 수 있어야 한다")
    void getUserDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        UserDto result = userService.getUserDto(1L);
        
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("홍길동");
        assertThat(result.getLoginId()).isEqualTo("test001");
    }
}