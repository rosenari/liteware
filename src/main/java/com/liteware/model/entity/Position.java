package com.liteware.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "positions",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "position_code")
       })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_id")
    private Long positionId;
    
    @NotBlank(message = "직급명은 필수입니다")
    @Size(max = 50, message = "직급명은 50자를 초과할 수 없습니다")
    @Column(name = "position_name", nullable = false, length = 50)
    private String positionName;
    
    @Column(name = "position_level", nullable = false)
    private Integer positionLevel;
    
    @NotBlank(message = "직급 코드는 필수입니다")
    @Size(max = 20, message = "직급 코드는 20자를 초과할 수 없습니다")
    @Column(name = "position_code", nullable = false, unique = true, length = 20)
    private String positionCode;
    
    @Column(name = "sort_order")
    private Integer sortOrder;
    
    @Column(name = "description")
    private String description;
    
    @OneToMany(mappedBy = "position", fetch = FetchType.LAZY)
    @Builder.Default
    private List<User> users = new ArrayList<>();
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    public void addUser(User user) {
        users.add(user);
        user.setPosition(this);
    }
    
    public void removeUser(User user) {
        users.remove(user);
        user.setPosition(null);
    }
}