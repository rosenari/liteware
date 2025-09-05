package com.liteware.service.auth;

import com.liteware.model.dto.LoginRequest;
import com.liteware.model.dto.LoginResponse;
import com.liteware.model.dto.SignupRequest;
import com.liteware.model.entity.Role;
import com.liteware.model.entity.User;
import com.liteware.model.entity.UserStatus;
import com.liteware.repository.RoleRepository;
import com.liteware.repository.UserRepository;
import com.liteware.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    
    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getLoginId(),
                            request.getPassword()
                    )
            );
            
            User user = userRepository.findByLoginId(request.getLoginId())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            if (user.getStatus() == UserStatus.SUSPENDED || user.isAccountLocked()) {
                throw new RuntimeException("계정이 잠겨있습니다. 관리자에게 문의하세요.");
            }
            
            user.resetLoginAttempts();
            user.setLastLoginAt(LocalDate.now());
            userRepository.save(user);
            
            String accessToken = jwtTokenProvider.createAccessToken(authentication.getName());
            String refreshToken = jwtTokenProvider.createRefreshToken(authentication.getName());
            
            return LoginResponse.builder()
                    .userId(user.getUserId())
                    .loginId(user.getLoginId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .roles(user.getRoles().stream()
                            .map(Role::getRoleName)
                            .collect(Collectors.toSet()))
                    .departmentName(user.getDepartment() != null ? user.getDepartment().getDeptName() : null)
                    .positionName(user.getPosition() != null ? user.getPosition().getPositionName() : null)
                    .build();
                    
        } catch (AuthenticationException e) {
            User user = userRepository.findByLoginId(request.getLoginId()).orElse(null);
            if (user != null) {
                user.incrementLoginAttempts();
                userRepository.save(user);
            }
            throw new BadCredentialsException("Invalid credentials", e);
        }
    }
    
    public User signup(SignupRequest request) {
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다");
        }
        
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new RuntimeException("이미 사용중인 로그인 ID입니다");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 사용중인 이메일입니다");
        }
        
        User user = User.builder()
                .loginId(request.getLoginId())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .status(UserStatus.PENDING)
                .build();
        
        Role defaultRole = roleRepository.findByRoleCode("ROLE_USER")
                .orElseGet(() -> {
                    Role role = Role.builder()
                            .roleName("USER")
                            .roleCode("ROLE_USER")
                            .description("일반 사용자")
                            .isActive(true)
                            .build();
                    return roleRepository.save(role);
                });
        
        user.addRole(defaultRole);
        user.setPasswordChangedAt(LocalDate.now());
        
        return userRepository.save(user);
    }
    
    public String refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 Refresh Token입니다");
        }
        
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByLoginId(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("비활성화된 계정입니다");
        }
        
        return jwtTokenProvider.createAccessToken(username);
    }
    
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDate.now());
        userRepository.save(user);
        
        log.info("Password changed for user: {}", user.getLoginId());
    }
    
    public void resetPassword(String email, String temporaryPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 이메일로 등록된 사용자를 찾을 수 없습니다"));
        
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setPasswordChangedAt(LocalDate.now());
        userRepository.save(user);
        
        log.info("Password reset for user: {}", user.getLoginId());
    }
    
    public void logout(String token) {
        log.info("User logged out with token: {}", token.substring(0, 10) + "...");
    }
}