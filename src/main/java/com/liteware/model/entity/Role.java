package com.liteware.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "role_name"),
           @UniqueConstraint(columnNames = "role_code")
       })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;
    
    @NotBlank(message = "권한명은 필수입니다")
    @Size(max = 50, message = "권한명은 50자를 초과할 수 없습니다")
    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    private String roleName;
    
    @NotBlank(message = "권한 코드는 필수입니다")
    @Size(max = 20, message = "권한 코드는 20자를 초과할 수 없습니다")
    @Column(name = "role_code", nullable = false, unique = true, length = 20)
    private String roleCode;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "permissions", columnDefinition = "TEXT")
    private String permissions;
    
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> users = new HashSet<>();
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    public void addUser(User user) {
        users.add(user);
        user.getRoles().add(this);
    }
    
    public void removeUser(User user) {
        users.remove(user);
        user.getRoles().remove(this);
    }
}