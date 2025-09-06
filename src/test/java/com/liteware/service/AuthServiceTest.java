package com.liteware.service;

import com.liteware.model.dto.LoginRequest;
import com.liteware.model.dto.LoginResponse;
import com.liteware.model.dto.SignupRequest;
import com.liteware.model.entity.User;
import com.liteware.model.entity.UserStatus;
import com.liteware.repository.RoleRepository;
import com.liteware.repository.UserRepository;
import com.liteware.security.JwtTokenProvider;
import com.liteware.service.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RoleRepository roleRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @InjectMocks
    private AuthService authService;
    
    private LoginRequest loginRequest;
    private SignupRequest signupRequest;
    private User user;
    
    @BeforeEach
    void setUp() {
        loginRequest = LoginRequest.builder()
                .loginId("testuser")
                .password("password123")
                .build();
        
        signupRequest = SignupRequest.builder()
                .loginId("newuser")
                .password("password123")
                .name("새 사용자")
                .email("new@liteware.com")
                .phone("010-1234-5678")
                .build();
        
        user = User.builder()
                .userId(1L)
                .loginId("testuser")
                .password("encodedPassword")
                .name("테스트 사용자")
                .email("test@liteware.com")
                .status(UserStatus.ACTIVE)
                .build();
    }
    
    @Test
    @DisplayName("로그인 성공시 JWT 토큰을 반환해야 한다")
    void loginSuccess() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByLoginId("testuser")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken(anyString())).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(anyString())).thenReturn("refresh-token");
        
        LoginResponse response = authService.login(loginRequest);
        
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getLoginId()).isEqualTo("testuser");
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider).createAccessToken("testuser");
        verify(jwtTokenProvider).createRefreshToken("testuser");
    }
    
    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시 예외가 발생해야 한다")
    void loginFailWithWrongPassword() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
    }
    
    @Test
    @DisplayName("계정이 잠긴 사용자는 로그인할 수 없어야 한다")
    void loginFailWithLockedAccount() {
        user.setStatus(UserStatus.SUSPENDED);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(userRepository.findByLoginId("testuser")).thenReturn(Optional.of(user));
        
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("계정이 잠겨있습니다");
    }
    
    @Test
    @DisplayName("회원가입이 성공적으로 완료되어야 한다")
    void signupSuccess() {
        signupRequest.setPasswordConfirm("password123"); // Add password confirmation
        
        when(userRepository.existsByLoginId("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@liteware.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(roleRepository.findByRoleCode("ROLE_USER")).thenReturn(Optional.empty());
        when(roleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setUserId(2L);
            return savedUser;
        });
        
        User createdUser = authService.signup(signupRequest);
        
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getUserId()).isEqualTo(2L);
        assertThat(createdUser.getLoginId()).isEqualTo("newuser");
        assertThat(createdUser.getName()).isEqualTo("새 사용자");
        assertThat(createdUser.getEmail()).isEqualTo("new@liteware.com");
        
        verify(userRepository).existsByLoginId("newuser");
        verify(userRepository).existsByEmail("new@liteware.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    @DisplayName("중복된 로그인 ID로 회원가입 시 예외가 발생해야 한다")
    void signupFailWithDuplicateLoginId() {
        signupRequest.setPasswordConfirm("password123");
        when(userRepository.existsByLoginId("newuser")).thenReturn(true);
        
        assertThatThrownBy(() -> authService.signup(signupRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 사용중인 로그인 ID입니다");
    }
    
    @Test
    @DisplayName("중복된 이메일로 회원가입 시 예외가 발생해야 한다")
    void signupFailWithDuplicateEmail() {
        signupRequest.setPasswordConfirm("password123");
        when(userRepository.existsByLoginId("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@liteware.com")).thenReturn(true);
        
        assertThatThrownBy(() -> authService.signup(signupRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 사용중인 이메일입니다");
    }
    
    @Test
    @DisplayName("Refresh Token으로 새로운 Access Token을 발급받을 수 있어야 한다")
    void refreshToken() {
        String refreshToken = "valid-refresh-token";
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(refreshToken)).thenReturn("testuser");
        when(userRepository.findByLoginId("testuser")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken("testuser")).thenReturn("new-access-token");
        
        String newAccessToken = authService.refreshToken(refreshToken).getAccessToken();
        
        assertThat(newAccessToken).isEqualTo("new-access-token");
        
        verify(jwtTokenProvider).validateToken(refreshToken);
        verify(jwtTokenProvider).getUsernameFromToken(refreshToken);
        verify(jwtTokenProvider).createAccessToken("testuser");
    }
    
    @Test
    @DisplayName("유효하지 않은 Refresh Token으로는 새 토큰을 발급받을 수 없어야 한다")
    void refreshTokenFailWithInvalidToken() {
        String invalidRefreshToken = "invalid-refresh-token";
        when(jwtTokenProvider.validateToken(invalidRefreshToken)).thenReturn(false);
        
        assertThatThrownBy(() -> authService.refreshToken(invalidRefreshToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("유효하지 않은 Refresh Token입니다");
    }
    
    @Test
    @DisplayName("비밀번호를 성공적으로 변경할 수 있어야 한다")
    void changePasswordSuccess() {
        String oldPassword = "oldPassword";
        String newPassword = "newPassword";
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldPassword, user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        authService.changePassword(1L, oldPassword, newPassword);
        
        verify(passwordEncoder).matches(oldPassword, "encodedPassword"); // Verify with original password
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(any(User.class));
        assertThat(user.getPassword()).isEqualTo("encodedNewPassword");
    }
    
    @Test
    @DisplayName("잘못된 현재 비밀번호로 변경 시 예외가 발생해야 한다")
    void changePasswordFailWithWrongOldPassword() {
        String wrongOldPassword = "wrongPassword";
        String newPassword = "newPassword";
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(wrongOldPassword, user.getPassword())).thenReturn(false);
        
        assertThatThrownBy(() -> authService.changePassword(1L, wrongOldPassword, newPassword))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("현재 비밀번호가 일치하지 않습니다");
    }
}