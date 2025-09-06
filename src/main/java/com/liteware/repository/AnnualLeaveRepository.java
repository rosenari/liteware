package com.liteware.repository;

import com.liteware.model.entity.AnnualLeave;
import com.liteware.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnnualLeaveRepository extends JpaRepository<AnnualLeave, Long> {
    
    /**
     * 특정 사용자의 특정 연도 연차 정보 조회
     */
    Optional<AnnualLeave> findByUserAndYear(User user, Integer year);
    
    /**
     * 특정 사용자의 연차 정보 조회 (연도별 정렬)
     */
    List<AnnualLeave> findByUserOrderByYearDesc(User user);
    
    /**
     * 특정 연도의 모든 연차 정보 조회
     */
    List<AnnualLeave> findByYear(Integer year);
    
    /**
     * 만료 예정인 연차 조회 (30일 이내)
     */
    @Query("SELECT a FROM AnnualLeave a WHERE a.expiryDate <= :targetDate AND a.remainingHours > 0")
    List<AnnualLeave> findExpiringLeaves(@Param("targetDate") LocalDate targetDate);
    
    /**
     * 연차 사용률이 높은 사용자 조회
     */
    @Query("SELECT a FROM AnnualLeave a WHERE a.year = :year AND (a.usedHours / (a.totalHours + a.carriedOverHours)) > :usageRate")
    List<AnnualLeave> findHighUsageLeaves(@Param("year") Integer year, @Param("usageRate") Double usageRate);
    
    /**
     * 부서별 연차 사용 통계
     */
    @Query("SELECT a.user.department.deptName as departmentName, " +
           "AVG(a.usedHours) as avgUsedHours, " +
           "AVG(a.remainingHours) as avgRemainingHours " +
           "FROM AnnualLeave a " +
           "WHERE a.year = :year " +
           "GROUP BY a.user.department.deptName")
    List<Object[]> getDepartmentLeaveStatistics(@Param("year") Integer year);
    
    /**
     * 연차 미사용자 조회
     */
    @Query("SELECT a FROM AnnualLeave a WHERE a.year = :year AND a.usedHours = 0")
    List<AnnualLeave> findUnusedLeaves(@Param("year") Integer year);
    
    /**
     * 사용자 ID로 현재 연도 연차 정보 조회
     */
    @Query("SELECT a FROM AnnualLeave a WHERE a.user.userId = :userId AND a.year = :year")
    Optional<AnnualLeave> findByUserIdAndYear(@Param("userId") Long userId, @Param("year") Integer year);
}