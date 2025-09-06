package com.liteware.repository.board;

import com.liteware.model.dto.PostSearchCriteria;
import com.liteware.model.entity.board.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostRepositoryCustom {
    
    /**
     * 고급 검색 기능
     * @param criteria 검색 조건
     * @param pageable 페이징 정보
     * @return 검색 결과
     */
    Page<Post> searchWithCriteria(PostSearchCriteria criteria, Pageable pageable);
    
    /**
     * 통계 정보를 포함한 검색
     * @param criteria 검색 조건
     * @return 검색 결과 및 통계
     */
    SearchResult searchWithStats(PostSearchCriteria criteria, Pageable pageable);
    
    /**
     * 검색 결과 및 통계 정보를 담는 클래스
     */
    class SearchResult {
        private Page<Post> posts;
        private long totalCount;
        private long noticeCount;
        private long secretCount;
        private long attachmentCount;
        
        public SearchResult(Page<Post> posts, long totalCount, long noticeCount, 
                           long secretCount, long attachmentCount) {
            this.posts = posts;
            this.totalCount = totalCount;
            this.noticeCount = noticeCount;
            this.secretCount = secretCount;
            this.attachmentCount = attachmentCount;
        }
        
        // Getters
        public Page<Post> getPosts() { return posts; }
        public long getTotalCount() { return totalCount; }
        public long getNoticeCount() { return noticeCount; }
        public long getSecretCount() { return secretCount; }
        public long getAttachmentCount() { return attachmentCount; }
    }
}