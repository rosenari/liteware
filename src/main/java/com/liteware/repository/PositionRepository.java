package com.liteware.repository;

import com.liteware.model.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    
    Optional<Position> findByPositionCode(String positionCode);
    
    List<Position> findByIsActiveTrue();
    
    List<Position> findByIsActiveTrueOrderBySortOrder();
    
    boolean existsByPositionCode(String positionCode);
    
    @Query("SELECT p FROM Position p WHERE p.positionLevel = :level AND p.isActive = true")
    List<Position> findByPositionLevel(@Param("level") Integer level);
    
    @Query("SELECT p FROM Position p WHERE p.positionName LIKE %:name% AND p.isActive = true")
    List<Position> searchByPositionName(@Param("name") String name);
    
    @Query("SELECT p FROM Position p LEFT JOIN FETCH p.users WHERE p.positionId = :positionId")
    Optional<Position> findByIdWithUsers(@Param("positionId") Long positionId);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.position.positionId = :positionId AND u.isDeleted = false")
    Long countUsersByPositionId(@Param("positionId") Long positionId);
}