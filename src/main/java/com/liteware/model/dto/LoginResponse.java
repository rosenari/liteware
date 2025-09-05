package com.liteware.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private Long userId;
    private String loginId;
    private String name;
    private String email;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private Set<String> roles;
    private String departmentName;
    private String positionName;
}