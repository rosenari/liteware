package com.liteware.service.organization;

import com.liteware.model.dto.DepartmentDto;
import com.liteware.model.entity.Department;
import com.liteware.model.entity.User;
import com.liteware.repository.DepartmentRepository;
import com.liteware.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentService {
    
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    
    public Department createDepartment(DepartmentDto dto) {
        if (departmentRepository.existsByDeptCode(dto.getDeptCode())) {
            throw new RuntimeException("이미 사용중인 부서 코드입니다");
        }
        
        Department department = Department.builder()
                .deptName(dto.getDeptName())
                .deptCode(dto.getDeptCode())
                .sortOrder(dto.getSortOrder())
                .description(dto.getDescription())
                .isActive(true)
                .build();
        
        if (dto.getParentDeptId() != null) {
            Department parent = departmentRepository.findById(dto.getParentDeptId())
                    .orElseThrow(() -> new RuntimeException("상위 부서를 찾을 수 없습니다"));
            department.setParentDepartment(parent);
        }
        
        return departmentRepository.save(department);
    }
    
    public Department updateDepartment(Long deptId, DepartmentDto dto) {
        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new RuntimeException("부서를 찾을 수 없습니다"));
        
        if (dto.getDeptName() != null) {
            department.setDeptName(dto.getDeptName());
        }
        
        if (dto.getDescription() != null) {
            department.setDescription(dto.getDescription());
        }
        
        if (dto.getSortOrder() != null) {
            department.setSortOrder(dto.getSortOrder());
        }
        
        if (dto.getIsActive() != null) {
            department.setIsActive(dto.getIsActive());
        }
        
        return departmentRepository.save(department);
    }
    
    public void deleteDepartment(Long deptId) {
        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new RuntimeException("부서를 찾을 수 없습니다"));
        
        Long userCount = userRepository.countActiveUsersByDepartmentId(deptId);
        if (userCount > 0) {
            throw new RuntimeException("소속된 사용자가 있는 부서는 삭제할 수 없습니다");
        }
        
        Long subDeptCount = departmentRepository.countSubDepartments(deptId);
        if (subDeptCount > 0) {
            throw new RuntimeException("하위 부서가 있는 부서는 삭제할 수 없습니다");
        }
        
        department.setIsActive(false);
        departmentRepository.save(department);
        
        log.info("Department deleted (deactivated): {}", department.getDeptCode());
    }
    
    @Transactional(readOnly = true)
    public Department getDepartment(Long deptId) {
        return departmentRepository.findById(deptId)
                .orElseThrow(() -> new RuntimeException("부서를 찾을 수 없습니다"));
    }
    
    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return departmentRepository.findByIsActiveTrue();
    }
    
    @Transactional(readOnly = true)
    public List<Department> getDepartmentTree() {
        return departmentRepository.findByParentDepartmentIsNull();
    }
    
    @Transactional(readOnly = true)
    public List<User> getUsersByDepartment(Long deptId) {
        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new RuntimeException("부서를 찾을 수 없습니다"));
        return userRepository.findByDepartment(department);
    }
    
    @Transactional(readOnly = true)
    public List<Department> searchDepartments(String keyword) {
        return departmentRepository.searchByDeptName(keyword);
    }
    
    public Department assignManager(Long deptId, Long userId) {
        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new RuntimeException("부서를 찾을 수 없습니다"));
        
        User manager = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        if (manager.getDepartment() == null || !manager.getDepartment().getDeptId().equals(deptId)) {
            throw new RuntimeException("해당 부서 소속이 아닌 사용자는 부서장이 될 수 없습니다");
        }
        
        department.setManager(manager);
        return departmentRepository.save(department);
    }
    
    public Department moveDepartment(Long deptId, Long newParentId) {
        Department department = departmentRepository.findById(deptId)
                .orElseThrow(() -> new RuntimeException("부서를 찾을 수 없습니다"));
        
        if (newParentId != null) {
            Department newParent = departmentRepository.findById(newParentId)
                    .orElseThrow(() -> new RuntimeException("새로운 상위 부서를 찾을 수 없습니다"));
            
            if (isCircularReference(deptId, newParentId)) {
                throw new RuntimeException("순환 참조가 발생합니다");
            }
            
            department.setParentDepartment(newParent);
        } else {
            department.setParentDepartment(null);
        }
        
        return departmentRepository.save(department);
    }
    
    private boolean isCircularReference(Long deptId, Long newParentId) {
        if (deptId.equals(newParentId)) {
            return true;
        }
        
        Department current = departmentRepository.findById(newParentId).orElse(null);
        while (current != null && current.getParentDepartment() != null) {
            if (current.getParentDepartment().getDeptId().equals(deptId)) {
                return true;
            }
            current = current.getParentDepartment();
        }
        
        return false;
    }
    
    @Transactional(readOnly = true)
    public DepartmentDto getDepartmentWithDetails(Long deptId) {
        Department department = departmentRepository.findByIdWithUsers(deptId)
                .orElseThrow(() -> new RuntimeException("부서를 찾을 수 없습니다"));
        
        DepartmentDto dto = DepartmentDto.from(department);
        dto.setUserCount((long) department.getUsers().size());
        
        return dto;
    }
}