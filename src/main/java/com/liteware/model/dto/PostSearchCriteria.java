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
public class PostSearchCriteria {
    
    // 검색 키워드
    private String keyword;
    
    // 검색 대상 필드
    private SearchField searchField;
    
    // 게시판 ID
    private Long boardId;
    
    // 작성자 ID
    private Long writerId;
    
    // 작성자 이름
    private String writerName;
    
    // 부서 ID
    private Long departmentId;
    
    // 날짜 범위
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    // 공지사항 여부
    private Boolean isNotice;
    
    // 비밀글 여부
    private Boolean isSecret;
    
    // 삭제 여부
    private Boolean isDeleted;
    
    // 첨부파일 포함 여부
    private Boolean hasAttachment;
    
    // 최소 조회수
    private Integer minViewCount;
    
    // 최소 좋아요 수
    private Integer minLikeCount;
    
    // 정렬 기준
    private SortType sortType;
    
    public enum SearchField {
        ALL,        // 제목 + 내용
        TITLE,      // 제목만
        CONTENT,    // 내용만
        WRITER      // 작성자명
    }
    
    public enum SortType {
        LATEST,         // 최신순
        OLDEST,         // 오래된순
        VIEW_COUNT,     // 조회수순
        LIKE_COUNT,     // 좋아요순
        COMMENT_COUNT   // 댓글수순
    }
}