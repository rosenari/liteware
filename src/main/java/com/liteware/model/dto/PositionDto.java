package com.liteware.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionDto {
    
    private Long positionId;
    
    @NotBlank(message = "직급명은 필수입니다")
    @Size(max = 50, message = "직급명은 50자를 초과할 수 없습니다")
    private String positionName;
    
    @NotNull(message = "직급 레벨은 필수입니다")
    private Integer positionLevel;
    
    @NotBlank(message = "직급 코드는 필수입니다")
    @Size(max = 20, message = "직급 코드는 20자를 초과할 수 없습니다")
    private String positionCode;
    
    private Integer sortOrder;
    private String description;
    private Boolean isActive;
    private Long userCount;
    
    public static PositionDto from(com.liteware.model.entity.Position position) {
        return PositionDto.builder()
                .positionId(position.getPositionId())
                .positionName(position.getPositionName())
                .positionLevel(position.getPositionLevel())
                .positionCode(position.getPositionCode())
                .sortOrder(position.getSortOrder())
                .description(position.getDescription())
                .isActive(position.getIsActive())
                .build();
    }
}