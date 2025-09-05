package com.liteware.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private Long commentId;
    private Long postId;
    private String content;
    private Long writerId;
    private String writerName;
    private Long parentCommentId;
    private Integer depth;
    private Integer likeCount;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
}