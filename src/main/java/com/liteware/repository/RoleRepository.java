package com.liteware.repository;

import com.liteware.model.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    Optional<Role> findByRoleName(String roleName);
    
    Optional<Role> findByRoleCode(String roleCode);
    
    List<Role> findByIsActiveTrue();
    
    boolean existsByRoleName(String roleName);
    
    boolean existsByRoleCode(String roleCode);
    
    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.users WHERE r.roleId = :roleId")
    Optional<Role> findByIdWithUsers(@Param("roleId") Long roleId);
    
    @Query("SELECT r FROM Role r WHERE r.roleName LIKE %:name% AND r.isActive = true")
    List<Role> searchByRoleName(@Param("name") String name);
    
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.roleId = :roleId")
    Long countUsersByRoleId(@Param("roleId") Long roleId);
}