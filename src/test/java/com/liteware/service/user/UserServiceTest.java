package com.liteware.service.user;

import com.liteware.model.dto.UserDto;
import com.liteware.model.entity.User;
import com.liteware.model.entity.UserStatus;
import com.liteware.service.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceTest extends BaseServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @BeforeEach
    void setUp() {
        // BaseServiceTest의 baseSetUp()이 자동으로 호출됨
    }
    
    @Test
    @DisplayName("사용자 생성 성공 테스트")
    void createUser_Success() {
        // given
        UserDto userDto = new UserDto();
        userDto.setLoginId("newuser001");
        userDto.setPassword("password123");
        userDto.setName("새사용자");
        userDto.setEmail("newuser@example.com");
        userDto.setPhoneNumber("010-9999-8888");
        userDto.setDepartmentId(department.getDeptId());
        userDto.setPositionId(position.getPositionId());
        
        // when
        User createdUser = userService.createUser(userDto);
        
        // then
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getLoginId()).isEqualTo("newuser001");
        assertThat(createdUser.getName()).isEqualTo("새사용자");
        assertThat(createdUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(createdUser.getUserId()).isNotNull();
        assertThat(createdUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
    
    @Test
    @DisplayName("중복된 로그인 ID로 사용자 생성 시 예외 발생")
    void createUser_DuplicateLoginId_ThrowsException() {
        // given
        UserDto userDto = new UserDto();
        userDto.setLoginId(testUser.getLoginId()); // 이미 존재하는 로그인 ID
        userDto.setPassword("password123");
        userDto.setName("중복사용자");
        userDto.setEmail("duplicate@example.com");
        userDto.setPhoneNumber("010-7777-6666");
        userDto.setDepartmentId(department.getDeptId());
        userDto.setPositionId(position.getPositionId());
        
        // when & then
        assertThatThrownBy(() -> userService.createUser(userDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 사용중인 로그인 ID입니다");
    }
    
    @Test
    @DisplayName("중복된 이메일로 사용자 생성 시 예외 발생")
    void createUser_DuplicateEmail_ThrowsException() {
        // given
        UserDto userDto = new UserDto();
        userDto.setLoginId("uniqueuser001");
        userDto.setPassword("password123");
        userDto.setName("이메일중복");
        userDto.setEmail(testUser.getEmail()); // 이미 존재하는 이메일
        userDto.setPhoneNumber("010-5555-4444");
        userDto.setDepartmentId(department.getDeptId());
        userDto.setPositionId(position.getPositionId());
        
        // when & then
        assertThatThrownBy(() -> userService.createUser(userDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 존재하는 이메일입니다");
    }
    
    @Test
    @DisplayName("사용자 ID로 조회 성공 테스트")
    void getUserById_Success() {
        // when
        User foundUser = userService.getUserById(testUser.getUserId());
        
        // then
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getUserId()).isEqualTo(testUser.getUserId());
        assertThat(foundUser.getLoginId()).isEqualTo(testUser.getLoginId());
        assertThat(foundUser.getName()).isEqualTo(testUser.getName());
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회 시 예외 발생")
    void getUserById_NotFound_ThrowsException() {
        // given
        Long nonExistentId = 99999L;
        
        // when & then
        assertThatThrownBy(() -> userService.getUserById(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
    
    @Test
    @DisplayName("사용자 DTO 조회 성공 테스트")
    void getUserDto_Success() {
        // when
        UserDto userDto = userService.getUserDto(testUser.getUserId());
        
        // then
        assertThat(userDto).isNotNull();
        assertThat(userDto.getUserId()).isEqualTo(testUser.getUserId());
        assertThat(userDto.getLoginId()).isEqualTo(testUser.getLoginId());
        assertThat(userDto.getName()).isEqualTo(testUser.getName());
        assertThat(userDto.getEmail()).isEqualTo(testUser.getEmail());
    }
    
    @Test
    @DisplayName("모든 사용자 조회 성공 테스트")
    void getAllUsers_Success() {
        // when
        List<User> users = userService.getAllUsers();
        
        // then
        assertThat(users).isNotEmpty();
        assertThat(users).hasSizeGreaterThanOrEqualTo(2); // testUser, adminUser
    }
    
    @Test
    @DisplayName("부서별 사용자 조회 성공 테스트")
    void getUsersByDepartment_Success() {
        // given
        User user1 = createUser("dept001", "부서원1", "dept1@example.com", department, position);
        User user2 = createUser("dept002", "부서원2", "dept2@example.com", department, position);
        
        // when
        List<User> users = userService.getUsersByDepartment(department.getDeptId());
        
        // then
        assertThat(users).isNotEmpty();
        assertThat(users).hasSizeGreaterThanOrEqualTo(4); // testUser, adminUser, user1, user2
        assertThat(users).allMatch(user -> 
            user.getDepartment() != null && user.getDepartment().getDeptId().equals(department.getDeptId())
        );
    }
    
    @Test
    @DisplayName("사용자 검색 성공 테스트")
    void searchUsers_Success() {
        // given
        String keyword = "테스트";
        
        // when
        List<User> users = userService.searchUsers(keyword);
        
        // then
        assertThat(users).isNotEmpty();
        assertThat(users).anyMatch(user -> user.getName().contains(keyword));
    }
    
    @Test
    @DisplayName("사용자 정보 수정 성공 테스트")
    void updateUser_Success() {
        // given
        UserDto updateDto = new UserDto();
        updateDto.setName("수정된이름");
        updateDto.setEmail("updated@example.com");
        updateDto.setPhoneNumber("010-1111-2222");
        updateDto.setDepartmentId(department.getDeptId());
        updateDto.setPositionId(position.getPositionId());
        
        // when
        User updatedUser = userService.updateUser(testUser.getUserId(), updateDto);
        
        // then
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getName()).isEqualTo("수정된이름");
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getPhoneNumber()).isEqualTo("010-1111-2222");
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자 정보 수정 시 예외 발생")
    void updateUser_NotFound_ThrowsException() {
        // given
        Long nonExistentId = 99999L;
        UserDto updateDto = new UserDto();
        updateDto.setName("수정시도");
        
        // when & then
        assertThatThrownBy(() -> userService.updateUser(nonExistentId, updateDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
    
    @Test
    @DisplayName("비밀번호 변경 성공 테스트")
    void changePassword_Success() {
        // given
        String oldPassword = "password123";
        String newPassword = "newPassword456";
        
        // 먼저 현재 비밀번호를 암호화하여 설정
        testUser.setPassword(passwordEncoder.encode(oldPassword));
        userRepository.save(testUser);
        
        // when
        userService.changePassword(testUser.getUserId(), oldPassword, newPassword);
        
        // then
        User updatedUser = userRepository.findById(testUser.getUserId()).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(passwordEncoder.matches(newPassword, updatedUser.getPassword())).isTrue();
        assertThat(updatedUser.getPasswordChangedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("잘못된 현재 비밀번호로 변경 시도 시 예외 발생")
    void changePassword_WrongCurrentPassword_ThrowsException() {
        // given
        String wrongPassword = "wrongPassword";
        String newPassword = "newPassword456";
        
        // 현재 비밀번호를 암호화하여 설정
        testUser.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(testUser);
        
        // when & then
        assertThatThrownBy(() -> userService.changePassword(testUser.getUserId(), wrongPassword, newPassword))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("현재 비밀번호가 일치하지 않습니다");
    }
    
    @Test
    @DisplayName("사용자 삭제(비활성화) 성공 테스트")
    void deleteUser_Success() {
        // given
        User userToDelete = createUser("deleteuser", "삭제대상", "delete@example.com", department, position);
        
        // when
        userService.deleteUser(userToDelete.getUserId());
        
        // then
        User deletedUser = userRepository.findById(userToDelete.getUserId()).orElse(null);
        assertThat(deletedUser).isNotNull();
        assertThat(deletedUser.getIsDeleted()).isTrue();
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자 삭제 시 예외 발생")
    void deleteUser_NotFound_ThrowsException() {
        // given
        Long nonExistentId = 99999L;
        
        // when & then
        assertThatThrownBy(() -> userService.deleteUser(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
    
    @Test
    @DisplayName("사용자 상태 토글 성공 테스트")
    void toggleUserStatus_Success() {
        // given
        boolean initialStatus = testUser.isActive();
        
        // when
        userService.toggleUserStatus(testUser.getUserId());
        
        // then
        User updatedUser = userRepository.findById(testUser.getUserId()).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.isActive()).isNotEqualTo(initialStatus);
    }
    
    @Test
    @DisplayName("모든 부서 조회 성공 테스트")
    void getAllDepartments_Success() {
        // when
        var departments = userService.getAllDepartments();
        
        // then
        assertThat(departments).isNotEmpty();
        assertThat(departments).contains(department);
    }
    
    @Test
    @DisplayName("모든 직급 조회 성공 테스트")
    void getAllPositions_Success() {
        // when
        var positions = userService.getAllPositions();
        
        // then
        assertThat(positions).isNotEmpty();
        assertThat(positions).contains(position);
    }
}