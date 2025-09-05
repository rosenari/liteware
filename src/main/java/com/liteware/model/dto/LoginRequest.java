package com.liteware.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    @NotBlank(message = "로그인 ID를 입력해주세요")
    private String loginId;
    
    @NotBlank(message = "비밀번호를 입력해주세요")
    private String password;
    
    private boolean rememberMe;
}