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
     * 모든 사용자 조회 (부서, 직급, 권한 정보 포함)
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAllWithFullInfo();
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
    
    /**
     * 새 사용자 생성
     */
    @Transactional
    public User createUser(UserDto userDto) {
        // 중복 체크
        if (userRepository.findByLoginId(userDto.getLoginId()).isPresent()) {
            throw new RuntimeException("이미 존재하는 사용자ID입니다: " + userDto.getLoginId());
        }
        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            throw new RuntimeException("이미 존재하는 이메일입니다: " + userDto.getEmail());
        }
        
        User user = User.builder()
                .loginId(userDto.getLoginId())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .name(userDto.getName())
                .email(userDto.getEmail())
                .phone(userDto.getPhoneNumber())
                .status(com.liteware.model.entity.UserStatus.ACTIVE)
                .build();
        
        // 부서 설정
        if (userDto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(userDto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            user.setDepartment(department);
        }
        
        // 직급 설정
        if (userDto.getPositionId() != null) {
            Position position = positionRepository.findById(userDto.getPositionId())
                    .orElseThrow(() -> new RuntimeException("Position not found"));
            user.setPosition(position);
        }
        
        return userRepository.save(user);
    }
    
    /**
     * 사용자 삭제
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserById(userId);
        userRepository.delete(user);
    }
    
    /**
     * 사용자 상태 토글
     */
    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = getUserById(userId);
        if (user.getStatus() == com.liteware.model.entity.UserStatus.ACTIVE) {
            user.setStatus(com.liteware.model.entity.UserStatus.INACTIVE);
        } else {
            user.setStatus(com.liteware.model.entity.UserStatus.ACTIVE);
        }
        userRepository.save(user);
    }
    
    /**
     * 사용자 정보를 DTO로 반환
     */
    public UserDto getUserDto(Long userId) {
        User user = getUserById(userId);
        UserDto dto = new UserDto();
        dto.setUserId(user.getUserId());
        dto.setLoginId(user.getLoginId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setDepartmentId(user.getDepartment() != null ? user.getDepartment().getDeptId() : null);
        dto.setPositionId(user.getPosition() != null ? user.getPosition().getPositionId() : null);
        dto.setStatus(user.getStatus().name());
        return dto;
    }
}