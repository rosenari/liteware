package com.liteware.repository;

import com.liteware.model.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
    Optional<Department> findByDeptCode(String deptCode);
    
    List<Department> findByIsActiveTrue();
    
    List<Department> findByParentDepartmentIsNull();
    
    List<Department> findByParentDepartment(Department parentDepartment);
    
    boolean existsByDeptCode(String deptCode);
    
    @Query("SELECT d FROM Department d WHERE d.deptLevel = :level AND d.isActive = true")
    List<Department> findByDeptLevel(@Param("level") Integer level);
    
    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.subDepartments WHERE d.deptId = :deptId")
    Optional<Department> findByIdWithSubDepartments(@Param("deptId") Long deptId);
    
    @Query("SELECT d FROM Department d WHERE d.deptName LIKE %:name% AND d.isActive = true")
    List<Department> searchByDeptName(@Param("name") String name);
    
    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.users WHERE d.deptId = :deptId")
    Optional<Department> findByIdWithUsers(@Param("deptId") Long deptId);
    
    @Query("SELECT COUNT(d) FROM Department d WHERE d.parentDepartment.deptId = :parentId AND d.isActive = true")
    Long countSubDepartments(@Param("parentId") Long parentId);
}