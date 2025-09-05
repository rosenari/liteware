package com.liteware.model.dto;

import com.liteware.model.entity.board.BoardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardDto {
    private Long boardId;
    private String boardCode;
    private String boardName;
    private BoardType boardType;
    private String description;
    private Boolean useYn;
    private Integer writeAuthLevel;
    private Integer readAuthLevel;
    private Integer commentAuthLevel;
    private Boolean attachmentYn;
    private Boolean secretYn;
    private Boolean noticeYn;
    private Integer sortOrder;
}