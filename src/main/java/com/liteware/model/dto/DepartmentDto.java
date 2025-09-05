package com.liteware.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDto {
    
    private Long deptId;
    
    @NotBlank(message = "부서명은 필수입니다")
    @Size(max = 100, message = "부서명은 100자를 초과할 수 없습니다")
    private String deptName;
    
    @NotBlank(message = "부서 코드는 필수입니다")
    @Size(max = 20, message = "부서 코드는 20자를 초과할 수 없습니다")
    private String deptCode;
    
    private Long parentDeptId;
    private String parentDeptName;
    private Integer deptLevel;
    private Long managerId;
    private String managerName;
    private Integer sortOrder;
    private String description;
    private Boolean isActive;
    
    @Builder.Default
    private List<DepartmentDto> subDepartments = new ArrayList<>();
    
    private Long userCount;
    
    public static DepartmentDto from(com.liteware.model.entity.Department department) {
        DepartmentDto dto = DepartmentDto.builder()
                .deptId(department.getDeptId())
                .deptName(department.getDeptName())
                .deptCode(department.getDeptCode())
                .deptLevel(department.getDeptLevel())
                .sortOrder(department.getSortOrder())
                .description(department.getDescription())
                .isActive(department.getIsActive())
                .build();
        
        if (department.getParentDepartment() != null) {
            dto.setParentDeptId(department.getParentDepartment().getDeptId());
            dto.setParentDeptName(department.getParentDepartment().getDeptName());
        }
        
        if (department.getManager() != null) {
            dto.setManagerId(department.getManager().getUserId());
            dto.setManagerName(department.getManager().getName());
        }
        
        if (department.getSubDepartments() != null && !department.getSubDepartments().isEmpty()) {
            department.getSubDepartments().forEach(subDept -> 
                dto.getSubDepartments().add(DepartmentDto.from(subDept))
            );
        }
        
        return dto;
    }
}