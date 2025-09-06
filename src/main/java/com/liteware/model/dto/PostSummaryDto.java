package com.liteware.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostSummaryDto {
    private Long postId;
    private String title;
    private String writerName;
    private String departmentName;
    private Integer viewCount;
    private Integer likeCount;
    private Boolean isNotice;
    private Boolean isSecret;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long commentCount;
}