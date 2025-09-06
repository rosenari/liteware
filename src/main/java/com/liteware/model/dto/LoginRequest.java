package com.liteware.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    private String loginId;
    
    private String username; // Alternative field for login ID
    
    @NotBlank(message = "비밀번호를 입력해주세요")
    private String password;
    
    private boolean rememberMe;
}