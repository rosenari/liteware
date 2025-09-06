package com.liteware.controller.api;

import com.liteware.model.dto.JwtResponse;
import com.liteware.model.dto.LoginRequest;
import com.liteware.model.dto.SignupRequest;
import com.liteware.service.auth.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController {
    
    private final AuthService authService;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                   HttpServletResponse response) {
        try {
            JwtResponse jwtResponse = authService.authenticate(loginRequest);
            
            // Set JWT cookie
            Cookie jwtCookie = new Cookie("jwt", jwtResponse.getAccessToken());
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(false); // Set to true in production with HTTPS
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(60 * 60); // 1 hour
            response.addCookie(jwtCookie);
            
            // Set refresh token cookie
            if (jwtResponse.getRefreshToken() != null) {
                Cookie refreshCookie = new Cookie("refreshToken", jwtResponse.getRefreshToken());
                refreshCookie.setHttpOnly(true);
                refreshCookie.setSecure(false); // Set to true in production with HTTPS
                refreshCookie.setPath("/");
                refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
                response.addCookie(refreshCookie);
            }
            
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            log.error("Login error", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid credentials"));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Clear JWT cookie
        Cookie jwtCookie = new Cookie("jwt", "");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);
        
        // Clear refresh token cookie
        Cookie refreshCookie = new Cookie("refreshToken", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
        
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
    
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest signupRequest) {
        try {
            authService.signup(signupRequest);
            return ResponseEntity.ok(Map.of("message", "User registered successfully"));
        } catch (Exception e) {
            log.error("Signup error", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(name = "refreshToken", required = false) String refreshToken,
                                          HttpServletResponse response) {
        if (refreshToken == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Refresh token not found"));
        }
        
        try {
            JwtResponse jwtResponse = authService.refreshToken(refreshToken);
            
            // Update JWT cookie
            Cookie jwtCookie = new Cookie("jwt", jwtResponse.getAccessToken());
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(false);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(60 * 60); // 1 hour
            response.addCookie(jwtCookie);
            
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            log.error("Token refresh error", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid refresh token"));
        }
    }
}