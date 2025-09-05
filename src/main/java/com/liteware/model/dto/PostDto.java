package com.liteware.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {
    private Long postId;
    private Long boardId;
    private String title;
    private String content;
    private Long writerId;
    private String writerName;
    private Integer viewCount;
    private Integer likeCount;
    private Boolean isNotice;
    private Boolean isSecret;
    private LocalDate noticeStartDate;
    private LocalDate noticeEndDate;
    private List<Long> attachmentIds;
}