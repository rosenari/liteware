package com.liteware.repository;

import com.liteware.model.entity.Attendance;
import com.liteware.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    // 특정 사용자의 특정 날짜 근태 기록 조회
    Optional<Attendance> findByUserAndWorkDate(User user, LocalDate workDate);
    
    // 특정 사용자의 오늘 근태 기록 조회
    @Query("SELECT a FROM Attendance a WHERE a.user = :user AND a.workDate = :today")
    Optional<Attendance> findTodayAttendance(@Param("user") User user, @Param("today") LocalDate today);
    
    // 특정 사용자의 특정 월 근태 기록 조회
    @Query("SELECT a FROM Attendance a WHERE a.user = :user AND YEAR(a.workDate) = :year AND MONTH(a.workDate) = :month ORDER BY a.workDate DESC")
    List<Attendance> findByUserAndYearMonth(@Param("user") User user, @Param("year") int year, @Param("month") int month);
    
    // 특정 사용자의 이번달 총 근무일수 조회
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.user = :user AND YEAR(a.workDate) = :year AND MONTH(a.workDate) = :month AND a.checkInTime IS NOT NULL")
    long countWorkingDaysByUserAndYearMonth(@Param("user") User user, @Param("year") int year, @Param("month") int month);
    
    // 특정 사용자의 이번달 총 근무시간 조회 (체크인과 체크아웃이 모두 있는 경우)
    @Query("SELECT SUM(TIMESTAMPDIFF(MINUTE, a.checkInTime, a.checkOutTime)) FROM Attendance a " +
           "WHERE a.user = :user AND YEAR(a.workDate) = :year AND MONTH(a.workDate) = :month " +
           "AND a.checkInTime IS NOT NULL AND a.checkOutTime IS NOT NULL")
    Long getTotalWorkingMinutesByUserAndYearMonth(@Param("user") User user, @Param("year") int year, @Param("month") int month);
}