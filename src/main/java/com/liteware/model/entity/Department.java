package com.liteware.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departments",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "dept_code")
       })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dept_id")
    private Long deptId;
    
    @NotBlank(message = "부서명은 필수입니다")
    @Size(max = 100, message = "부서명은 100자를 초과할 수 없습니다")
    @Column(name = "dept_name", nullable = false, length = 100)
    private String deptName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_dept_id")
    private Department parentDepartment;
    
    @OneToMany(mappedBy = "parentDepartment", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Department> subDepartments = new ArrayList<>();
    
    @Column(name = "dept_level")
    private Integer deptLevel;
    
    @NotBlank(message = "부서 코드는 필수입니다")
    @Size(max = 20, message = "부서 코드는 20자를 초과할 수 없습니다")
    @Column(name = "dept_code", nullable = false, unique = true, length = 20)
    private String deptCode;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;
    
    @Column(name = "sort_order")
    private Integer sortOrder;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    public String getName() {
        return deptName;
    }
    
    public void setName(String name) {
        this.deptName = name;
    }
    
    public String getCode() {
        return deptCode;
    }
    
    public void setCode(String code) {
        this.deptCode = code;
    }
    
    public Department getParentDept() {
        return parentDepartment;
    }
    
    public boolean isActive() {
        return isActive != null && isActive;
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    @Builder.Default
    private List<User> users = new ArrayList<>();
    
    @Column(name = "description")
    private String description;
    
    @PrePersist
    @PreUpdate
    public void calculateDeptLevel() {
        if (parentDepartment == null) {
            this.deptLevel = 1;
        } else {
            this.deptLevel = parentDepartment.getDeptLevel() + 1;
        }
    }
    
    public void addSubDepartment(Department subDepartment) {
        subDepartments.add(subDepartment);
        subDepartment.setParentDepartment(this);
    }
    
    public void removeSubDepartment(Department subDepartment) {
        subDepartments.remove(subDepartment);
        subDepartment.setParentDepartment(null);
    }
    
    public void addUser(User user) {
        users.add(user);
        user.setDepartment(this);
    }
    
    public void removeUser(User user) {
        users.remove(user);
        user.setDepartment(null);
    }
}