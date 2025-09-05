package com.liteware.service.department;

import com.liteware.model.entity.Department;
import com.liteware.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {
    
    private final DepartmentRepository departmentRepository;
    
    /**
     * 모든 부서 조회
     */
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }
    
    /**
     * 활성 부서 조회
     */
    public List<Department> getActiveDepartments() {
        return departmentRepository.findByIsActiveTrue();
    }
    
    /**
     * 부서 ID로 조회
     */
    public Department getDepartmentById(Long deptId) {
        return departmentRepository.findById(deptId)
                .orElseThrow(() -> new RuntimeException("Department not found: " + deptId));
    }
    
    /**
     * 부서 생성
     */
    @Transactional
    public Department createDepartment(String name, String code, Department parentDept) {
        Department department = Department.builder()
                .deptName(name)
                .deptCode(code)
                .parentDepartment(parentDept)
                .isActive(true)
                .build();
        
        return departmentRepository.save(department);
    }
    
    /**
     * 부서 수정
     */
    @Transactional
    public Department updateDepartment(Long deptId, String name, String code) {
        Department department = getDepartmentById(deptId);
        
        if (name != null) {
            department.setName(name);
        }
        if (code != null) {
            department.setCode(code);
        }
        
        return departmentRepository.save(department);
    }
    
    /**
     * 부서 비활성화
     */
    @Transactional
    public void deactivateDepartment(Long deptId) {
        Department department = getDepartmentById(deptId);
        department.setActive(false);
        departmentRepository.save(department);
    }
    
    /**
     * 하위 부서 조회
     */
    public List<Department> getSubDepartments(Long parentDeptId) {
        Department parentDept = getDepartmentById(parentDeptId);
        return departmentRepository.findByParentDepartment(parentDept);
    }
}