package com.liteware.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "login_id"),
           @UniqueConstraint(columnNames = "email")
       })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    
    @NotBlank(message = "로그인 ID는 필수입니다")
    @Size(min = 4, max = 20, message = "로그인 ID는 4자 이상 20자 이하여야 합니다")
    @Column(name = "login_id", nullable = false, unique = true, length = 20)
    private String loginId;
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Column(name = "password", nullable = false)
    private String password;
    
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다")
    @Column(name = "name", nullable = false, length = 50)
    private String name;
    
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Column(name = "email", unique = true)
    private String email;
    
    @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다")
    @Column(name = "phone", length = 20)
    private String phone;
    
    public String getPhoneNumber() {
        return phone;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phone = phoneNumber;
    }
    
    public String getUsername() {
        return loginId;
    }
    
    @Column(name = "profile_image")
    private String profileImage;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;
    
    @Column(name = "hire_date")
    private LocalDate hireDate;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    
    @Column(name = "login_attempts")
    @Builder.Default
    private Integer loginAttempts = 0;
    
    @Column(name = "last_login_at")
    private LocalDate lastLoginAt;
    
    @Column(name = "password_changed_at")
    private LocalDate passwordChangedAt;
    
    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;
    
    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;
    
    public void addRole(Role role) {
        this.roles.add(role);
    }
    
    public void removeRole(Role role) {
        this.roles.remove(role);
    }
    
    public void incrementLoginAttempts() {
        this.loginAttempts++;
    }
    
    public void resetLoginAttempts() {
        this.loginAttempts = 0;
    }
    
    public boolean isAccountLocked() {
        return this.loginAttempts >= 5 || this.status == UserStatus.SUSPENDED;
    }
}