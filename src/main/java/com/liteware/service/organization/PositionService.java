package com.liteware.service.organization;

import com.liteware.model.dto.PositionDto;
import com.liteware.model.entity.Position;
import com.liteware.model.entity.User;
import com.liteware.repository.PositionRepository;
import com.liteware.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PositionService {
    
    private final PositionRepository positionRepository;
    private final UserRepository userRepository;
    
    public Position createPosition(PositionDto dto) {
        if (positionRepository.existsByPositionCode(dto.getPositionCode())) {
            throw new RuntimeException("이미 사용중인 직급 코드입니다");
        }
        
        Position position = Position.builder()
                .positionName(dto.getPositionName())
                .positionCode(dto.getPositionCode())
                .positionLevel(dto.getPositionLevel())
                .sortOrder(dto.getSortOrder())
                .description(dto.getDescription())
                .isActive(true)
                .build();
        
        return positionRepository.save(position);
    }
    
    public Position updatePosition(Long positionId, PositionDto dto) {
        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new RuntimeException("직급을 찾을 수 없습니다"));
        
        if (dto.getPositionName() != null) {
            position.setPositionName(dto.getPositionName());
        }
        
        if (dto.getPositionLevel() != null) {
            position.setPositionLevel(dto.getPositionLevel());
        }
        
        if (dto.getDescription() != null) {
            position.setDescription(dto.getDescription());
        }
        
        if (dto.getSortOrder() != null) {
            position.setSortOrder(dto.getSortOrder());
        }
        
        if (dto.getIsActive() != null) {
            position.setIsActive(dto.getIsActive());
        }
        
        return positionRepository.save(position);
    }
    
    public void deletePosition(Long positionId) {
        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new RuntimeException("직급을 찾을 수 없습니다"));
        
        Long userCount = positionRepository.countUsersByPositionId(positionId);
        if (userCount > 0) {
            throw new RuntimeException("해당 직급을 가진 사용자가 있어 삭제할 수 없습니다");
        }
        
        position.setIsActive(false);
        positionRepository.save(position);
        
        log.info("Position deleted (deactivated): {}", position.getPositionCode());
    }
    
    @Transactional(readOnly = true)
    public Position getPosition(Long positionId) {
        return positionRepository.findById(positionId)
                .orElseThrow(() -> new RuntimeException("직급을 찾을 수 없습니다"));
    }
    
    @Transactional(readOnly = true)
    public List<Position> getAllActivePositions() {
        return positionRepository.findByIsActiveTrueOrderBySortOrder();
    }
    
    @Transactional(readOnly = true)
    public List<Position> getPositionsByLevel(Integer level) {
        return positionRepository.findByPositionLevel(level);
    }
    
    @Transactional(readOnly = true)
    public Long getUserCountByPosition(Long positionId) {
        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new RuntimeException("직급을 찾을 수 없습니다"));
        return positionRepository.countUsersByPositionId(positionId);
    }
    
    @Transactional(readOnly = true)
    public List<User> getUsersByPosition(Long positionId) {
        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new RuntimeException("직급을 찾을 수 없습니다"));
        return userRepository.findByPosition(position);
    }
    
    @Transactional(readOnly = true)
    public List<Position> searchPositions(String keyword) {
        return positionRepository.searchByPositionName(keyword);
    }
    
    public void updatePositionOrder(List<Long> positionIds) {
        List<Position> positions = new ArrayList<>();
        
        for (int i = 0; i < positionIds.size(); i++) {
            Long positionId = positionIds.get(i);
            Position position = positionRepository.findById(positionId)
                    .orElseThrow(() -> new RuntimeException("직급을 찾을 수 없습니다: " + positionId));
            position.setSortOrder(i + 1);
            positions.add(position);
        }
        
        positionRepository.saveAll(positions);
        log.info("Position order updated for {} positions", positions.size());
    }
    
    @Transactional(readOnly = true)
    public List<PositionDto> getAllPositionsWithUserCount() {
        List<Position> positions = positionRepository.findByIsActiveTrue();
        
        return positions.stream().map(position -> {
            PositionDto dto = PositionDto.from(position);
            dto.setUserCount(positionRepository.countUsersByPositionId(position.getPositionId()));
            return dto;
        }).collect(Collectors.toList());
    }
    
    public Position changePositionStatus(Long positionId, boolean isActive) {
        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new RuntimeException("직급을 찾을 수 없습니다"));
        
        if (!isActive && positionRepository.countUsersByPositionId(positionId) > 0) {
            throw new RuntimeException("사용자가 배정된 직급은 비활성화할 수 없습니다");
        }
        
        position.setIsActive(isActive);
        return positionRepository.save(position);
    }
}