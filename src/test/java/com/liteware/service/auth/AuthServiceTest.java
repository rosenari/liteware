package com.liteware.service.auth;

import com.liteware.model.dto.JwtResponse;
import com.liteware.model.dto.LoginRequest;
import com.liteware.model.dto.LoginResponse;
import com.liteware.model.dto.SignupRequest;
import com.liteware.model.entity.User;
import com.liteware.model.entity.UserStatus;
import com.liteware.security.JwtTokenProvider;
import com.liteware.service.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest extends BaseServiceTest {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private User loginUser;
    
    @BeforeEach
    void setUp() {
        // 로그인 테스트용 사용자 생성
        loginUser = createUser("loginuser", "로그인사용자", "login@example.com", department, position);
        loginUser.setPassword(passwordEncoder.encode("password123"));
        loginUser.setStatus(UserStatus.ACTIVE);
        loginUser.addRole(userRole);
        userRepository.save(loginUser);
    }
    
    @Test
    @DisplayName("로그인 성공 테스트")
    void login_Success() {
        // given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginId("loginuser");
        loginRequest.setPassword("password123");
        
        // when
        LoginResponse response = authService.login(loginRequest);
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getUserId()).isEqualTo(loginUser.getUserId());
        assertThat(response.getLoginId()).isEqualTo("loginuser");
        assertThat(response.getName()).isEqualTo("로그인사용자");
        assertThat(response.getRoles()).contains("일반사용자");
        
        // 로그인 시도 횟수 초기화 확인
        User updatedUser = userRepository.findById(loginUser.getUserId()).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getLoginAttempts()).isEqualTo(0);
        assertThat(updatedUser.getLastLoginAt()).isEqualTo(LocalDate.now());
    }
    
    @Test
    @DisplayName("JWT 인증 성공 테스트")
    void authenticate_Success() {
        // given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("loginuser");
        loginRequest.setPassword("password123");
        
        // when
        JwtResponse response = authService.authenticate(loginRequest);
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getUsername()).isEqualTo("loginuser");
        assertThat(response.getName()).isEqualTo("로그인사용자");
        assertThat(response.getEmail()).isEqualTo("login@example.com");
    }
    
    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시 예외 발생")
    void login_WrongPassword_ThrowsException() {
        // given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginId("loginuser");
        loginRequest.setPassword("wrongPassword");
        
        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
        
        // 로그인 시도 횟수 증가 확인
        User updatedUser = userRepository.findById(loginUser.getUserId()).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getLoginAttempts()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자로 로그인 시 예외 발생")
    void login_NonExistentUser_ThrowsException() {
        // given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginId("nonexistent");
        loginRequest.setPassword("password");
        
        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }
    
    @Test
    @DisplayName("계정이 잠긴 사용자 로그인 시 예외 발생")
    void login_LockedAccount_ThrowsException() {
        // given
        loginUser.setLoginAttempts(5); // 계정 잠금 임계값
        userRepository.save(loginUser);
        
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginId("loginuser");
        loginRequest.setPassword("password123");
        
        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("계정이 잠겨있습니다");
    }
    
    @Test
    @DisplayName("정지된 계정으로 로그인 시 예외 발생")
    void login_SuspendedAccount_ThrowsException() {
        // given
        loginUser.setStatus(UserStatus.SUSPENDED);
        userRepository.save(loginUser);
        
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setLoginId("loginuser");
        loginRequest.setPassword("password123");
        
        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("계정이 잠겨있습니다");
    }
    
    @Test
    @DisplayName("리프레시 토큰으로 액세스 토큰 재발급 성공")
    void refreshToken_Success() throws InterruptedException {
        // given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("loginuser");
        loginRequest.setPassword("password123");
        JwtResponse loginResponse = authService.authenticate(loginRequest);
        
        // JWT 토큰이 다른 시간에 생성되도록 짧은 지연 추가
        Thread.sleep(1000);
        
        // when
        JwtResponse refreshResponse = authService.refreshToken(loginResponse.getRefreshToken());
        
        // then
        assertThat(refreshResponse).isNotNull();
        assertThat(refreshResponse.getAccessToken()).isNotBlank();
        assertThat(refreshResponse.getRefreshToken()).isEqualTo(loginResponse.getRefreshToken());
        assertThat(refreshResponse.getUsername()).isEqualTo("loginuser");
        
        // 새로운 액세스 토큰이 발급되었는지 확인
        assertThat(refreshResponse.getAccessToken()).isNotEqualTo(loginResponse.getAccessToken());
    }
    
    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 재발급 시 예외 발생")
    void refreshToken_InvalidToken_ThrowsException() {
        // given
        String invalidToken = "invalid.token.here";
        
        // when & then
        assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("유효하지 않은 Refresh Token입니다");
    }
    
    @Test
    @DisplayName("회원가입 성공 테스트")
    void signup_Success() {
        // given
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setLoginId("newuser");
        signupRequest.setPassword("newPassword123");
        signupRequest.setPasswordConfirm("newPassword123");
        signupRequest.setName("새사용자");
        signupRequest.setEmail("newuser@example.com");
        signupRequest.setPhone("010-1234-5678");
        
        // when
        User newUser = authService.signup(signupRequest);
        
        // then
        assertThat(newUser).isNotNull();
        assertThat(newUser.getLoginId()).isEqualTo("newuser");
        assertThat(newUser.getName()).isEqualTo("새사용자");
        assertThat(newUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(newUser.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(newUser.getRoles()).isNotEmpty();
        assertThat(passwordEncoder.matches("newPassword123", newUser.getPassword())).isTrue();
    }
    
    @Test
    @DisplayName("비밀번호 불일치로 회원가입 실패")
    void signup_PasswordMismatch_ThrowsException() {
        // given
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setLoginId("newuser");
        signupRequest.setPassword("password123");
        signupRequest.setPasswordConfirm("differentPassword");
        signupRequest.setName("새사용자");
        signupRequest.setEmail("newuser@example.com");
        
        // when & then
        assertThatThrownBy(() -> authService.signup(signupRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("비밀번호가 일치하지 않습니다");
    }
    
    @Test
    @DisplayName("중복된 로그인 ID로 회원가입 실패")
    void signup_DuplicateLoginId_ThrowsException() {
        // given
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setLoginId(loginUser.getLoginId());
        signupRequest.setPassword("password123");
        signupRequest.setPasswordConfirm("password123");
        signupRequest.setName("새사용자");
        signupRequest.setEmail("newuser@example.com");
        
        // when & then
        assertThatThrownBy(() -> authService.signup(signupRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 사용중인 로그인 ID입니다");
    }
    
    @Test
    @DisplayName("중복된 이메일로 회원가입 실패")
    void signup_DuplicateEmail_ThrowsException() {
        // given
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setLoginId("newuser");
        signupRequest.setPassword("password123");
        signupRequest.setPasswordConfirm("password123");
        signupRequest.setName("새사용자");
        signupRequest.setEmail(loginUser.getEmail());
        
        // when & then
        assertThatThrownBy(() -> authService.signup(signupRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 사용중인 이메일입니다");
    }
    
    @Test
    @DisplayName("비밀번호 변경 성공 테스트")
    void changePassword_Success() {
        // given
        String oldPassword = "password123";
        String newPassword = "newPassword456";
        
        // when
        authService.changePassword(loginUser.getUserId(), oldPassword, newPassword);
        
        // then
        User updatedUser = userRepository.findById(loginUser.getUserId()).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(passwordEncoder.matches(newPassword, updatedUser.getPassword())).isTrue();
        assertThat(updatedUser.getPasswordChangedAt()).isEqualTo(LocalDate.now());
    }
    
    @Test
    @DisplayName("잘못된 현재 비밀번호로 변경 시도 시 예외 발생")
    void changePassword_WrongCurrentPassword_ThrowsException() {
        // given
        String wrongPassword = "wrongPassword";
        String newPassword = "newPassword456";
        
        // when & then
        assertThatThrownBy(() -> authService.changePassword(loginUser.getUserId(), wrongPassword, newPassword))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("현재 비밀번호가 일치하지 않습니다");
    }
    
    @Test
    @DisplayName("비밀번호 재설정 성공 테스트")
    void resetPassword_Success() {
        // given
        String temporaryPassword = "temp123456";
        
        // when
        authService.resetPassword(loginUser.getEmail(), temporaryPassword);
        
        // then
        User updatedUser = userRepository.findById(loginUser.getUserId()).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(passwordEncoder.matches(temporaryPassword, updatedUser.getPassword())).isTrue();
        assertThat(updatedUser.getPasswordChangedAt()).isEqualTo(LocalDate.now());
    }
    
    @Test
    @DisplayName("존재하지 않는 이메일로 비밀번호 재설정 시 예외 발생")
    void resetPassword_NonExistentEmail_ThrowsException() {
        // given
        String nonExistentEmail = "nonexistent@example.com";
        String temporaryPassword = "temp123456";
        
        // when & then
        assertThatThrownBy(() -> authService.resetPassword(nonExistentEmail, temporaryPassword))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("해당 이메일로 등록된 사용자를 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("로그아웃 성공 테스트")
    void logout_Success() {
        // given
        String token = "Bearer eyJhbGciOiJIUzI1NiJ9.example.token";
        
        // when & then - 로그아웃은 로그만 남기므로 예외가 발생하지 않으면 성공
        authService.logout(token);
    }
}