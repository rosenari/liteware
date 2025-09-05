package com.liteware.service.user;

import com.liteware.model.dto.UserDto;
import com.liteware.model.entity.Department;
import com.liteware.model.entity.Position;
import com.liteware.model.entity.User;
import com.liteware.repository.DepartmentRepository;
import com.liteware.repository.PositionRepository;
import com.liteware.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 모든 사용자 조회
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * 사용자 ID로 조회
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }
    
    /**
     * 부서별 사용자 조회
     */
    public List<User> getUsersByDepartment(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found: " + departmentId));
        return userRepository.findByDepartment(department);
    }
    
    /**
     * 사용자 검색
     */
    public List<User> searchUsers(String keyword) {
        return userRepository.findByNameContainingOrLoginIdContainingOrEmailContaining(
                keyword, keyword, keyword);
    }
    
    /**
     * 사용자 정보 수정
     */
    @Transactional
    public User updateUser(Long userId, UserDto userDto) {
        User user = getUserById(userId);
        
        if (userDto.getName() != null) {
            user.setName(userDto.getName());
        }
        if (userDto.getEmail() != null) {
            user.setEmail(userDto.getEmail());
        }
        if (userDto.getPhoneNumber() != null) {
            user.setPhoneNumber(userDto.getPhoneNumber());
        }
        if (userDto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(userDto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            user.setDepartment(department);
        }
        if (userDto.getPositionId() != null) {
            Position position = positionRepository.findById(userDto.getPositionId())
                    .orElseThrow(() -> new RuntimeException("Position not found"));
            user.setPosition(position);
        }
        
        return userRepository.save(user);
    }
    
    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    /**
     * 모든 부서 조회
     */
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }
    
    /**
     * 모든 직급 조회
     */
    public List<Position> getAllPositions() {
        return positionRepository.findAll();
    }
}