package com.liteware.model.dto;

import lombok.*;

import java.util.Set;

@Getter
@Setter
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
    private Set<String> roles;
    private String departmentName;
    private String positionName;
}