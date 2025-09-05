package com.liteware.repository;

import com.liteware.model.entity.Department;
import com.liteware.model.entity.Position;
import com.liteware.model.entity.User;
import com.liteware.model.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByLoginId(String loginId);
    
    Optional<User> findByEmail(String email);
    
    List<User> findByDepartment(Department department);
    
    List<User> findByPosition(Position position);
    
    List<User> findByStatus(UserStatus status);
    
    List<User> findByNameContaining(String name);
    
    List<User> findByIsDeletedFalse();
    
    boolean existsByLoginId(String loginId);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.department.deptId = :deptId AND u.isDeleted = false")
    List<User> findActiveUsersByDepartmentId(@Param("deptId") Long deptId);
    
    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.loginId = :loginId")
    Optional<User> findByLoginIdWithRoles(@Param("loginId") String loginId);
    
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.isDeleted = false")
    Page<User> findActiveUsersByStatus(@Param("status") UserStatus status, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.loginId) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND u.isDeleted = false")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.department.deptId = :deptId AND u.isDeleted = false")
    Long countActiveUsersByDepartmentId(@Param("deptId") Long deptId);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.department LEFT JOIN FETCH u.position " +
           "WHERE u.userId = :userId")
    Optional<User> findByIdWithDepartmentAndPosition(@Param("userId") Long userId);
    
    List<User> findByNameContainingOrLoginIdContainingOrEmailContaining(String name, String loginId, String email);
}