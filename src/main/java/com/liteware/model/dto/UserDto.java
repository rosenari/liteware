package com.liteware.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long userId;
    private String loginId;
    private String username;
    private String password;
    private String email;
    private String name;
    private String phoneNumber;
    private Long departmentId;
    private Long positionId;
    private String status;
}