package com.liteware.controller.api;

import com.liteware.model.dto.CommentDto;
import com.liteware.model.dto.PostDto;
import com.liteware.model.dto.PostSearchCriteria;
import com.liteware.model.entity.board.Comment;
import com.liteware.model.entity.board.Post;
import com.liteware.service.board.BoardService;
import com.liteware.service.board.CommentService;
import com.liteware.repository.board.PostRepositoryCustom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardApiController {
    
    private final BoardService boardService;
    private final CommentService commentService;
    
    /**
     * 게시글 생성
     */
    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody PostDto postDto) {
        try {
            Post post = boardService.createPost(postDto);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            log.error("Failed to create post", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 게시글 목록 조회
     */
    @GetMapping
    public ResponseEntity<Page<Post>> getPosts(
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        
        Page<Post> posts;
        
        if (keyword != null && !keyword.isEmpty()) {
            posts = boardService.searchPosts(keyword, pageable);
        } else if (boardId != null) {
            posts = boardService.getPostsByBoard(boardId, pageable);
        } else {
            posts = boardService.searchPosts("", pageable);
        }
        
        return ResponseEntity.ok(posts);
    }
    
    /**
     * 게시글 상세 조회
     */
    @GetMapping("/{postId}")
    public ResponseEntity<Post> getPost(@PathVariable Long postId,
                                        @RequestParam(required = false) Long userId) {
        try {
            Post post = userId != null ? 
                       boardService.getPost(postId, userId) : 
                       boardService.getPost(postId);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            log.error("Post not found: {}", postId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 게시글 수정
     */
    @PutMapping("/{postId}")
    public ResponseEntity<Post> updatePost(@PathVariable Long postId,
                                          @RequestBody PostDto postDto,
                                          @RequestParam Long userId) {
        try {
            Post post = boardService.updatePost(postId, postDto, userId);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            log.error("Failed to update post", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 게시글 삭제
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Map<String, Object>> deletePost(@PathVariable Long postId,
                                                         @RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boardService.deletePost(postId, userId);
            response.put("success", true);
            response.put("message", "게시글이 삭제되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to delete post", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 댓글 추가
     */
    @PostMapping("/{postId}/comments")
    public ResponseEntity<Comment> addComment(@PathVariable Long postId,
                                             @RequestBody CommentDto commentDto) {
        try {
            commentDto.setPostId(postId);
            Comment comment = commentService.createComment(commentDto);
            return ResponseEntity.ok(comment);
        } catch (Exception e) {
            log.error("Failed to create comment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 댓글 목록 조회
     */
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<Comment>> getComments(@PathVariable Long postId) {
        try {
            List<Comment> comments = commentService.getCommentsByPost(postId);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            log.error("Failed to get comments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 댓글 수정
     */
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> updateComment(@PathVariable Long commentId,
                                                            @RequestBody CommentDto commentDto,
                                                            @RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            commentService.updateComment(commentId, commentDto.getContent(), userId);
            response.put("success", true);
            response.put("message", "댓글이 수정되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to update comment", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 댓글 삭제
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(@PathVariable Long commentId,
                                                            @RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            commentService.deleteComment(commentId, userId);
            response.put("success", true);
            response.put("message", "댓글이 삭제되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to delete comment", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 임시저장
     */
    @PostMapping("/draft")
    public ResponseEntity<Map<String, Object>> saveDraft(@RequestBody PostDto postDto) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // TODO: 임시저장 기능 구현
            response.put("success", true);
            response.put("message", "임시저장되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to save draft", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 이미지 업로드
     */
    @PostMapping("/upload/image")
    public ResponseEntity<String> uploadImage(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            // TODO: 이미지 업로드 기능 구현
            String imageUrl = "/uploads/images/" + file.getOriginalFilename();
            return ResponseEntity.ok(imageUrl);
        } catch (Exception e) {
            log.error("Failed to upload image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 고급 검색 API
     */
    @GetMapping("/search/advanced")
    public ResponseEntity<Page<Post>> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PostSearchCriteria.SearchField searchField,
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) Long writerId,
            @RequestParam(required = false) String writerName,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Boolean isNotice,
            @RequestParam(required = false) Boolean isSecret,
            @RequestParam(required = false) Boolean hasAttachment,
            @RequestParam(required = false) Integer minViewCount,
            @RequestParam(required = false) Integer minLikeCount,
            @RequestParam(required = false) PostSearchCriteria.SortType sortType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        PostSearchCriteria criteria = PostSearchCriteria.builder()
                .keyword(keyword)
                .searchField(searchField)
                .boardId(boardId)
                .writerId(writerId)
                .writerName(writerName)
                .departmentId(departmentId)
                .startDate(startDate)
                .endDate(endDate)
                .isNotice(isNotice)
                .isSecret(isSecret)
                .hasAttachment(hasAttachment)
                .minViewCount(minViewCount)
                .minLikeCount(minLikeCount)
                .sortType(sortType)
                .build();
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> results = boardService.advancedSearch(criteria, pageable);
        
        log.info("Advanced search executed: {} results found", results.getTotalElements());
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * 고급 검색 API (POST 방식)
     */
    @PostMapping("/search/advanced")
    public ResponseEntity<Page<Post>> advancedSearchPost(
            @RequestBody PostSearchCriteria criteria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> results = boardService.advancedSearch(criteria, pageable);
        
        log.info("Advanced search (POST) executed: {} results found", results.getTotalElements());
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * 통계 정보를 포함한 고급 검색 API
     */
    @PostMapping("/search/advanced/stats")
    public ResponseEntity<Map<String, Object>> advancedSearchWithStats(
            @RequestBody PostSearchCriteria criteria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        PostRepositoryCustom.SearchResult result = boardService.advancedSearchWithStats(criteria, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("posts", result.getPosts());
        response.put("statistics", Map.of(
            "totalCount", result.getTotalCount(),
            "noticeCount", result.getNoticeCount(),
            "secretCount", result.getSecretCount(),
            "attachmentCount", result.getAttachmentCount()
        ));
        
        log.info("Advanced search with stats executed: {} total results", result.getTotalCount());
        
        return ResponseEntity.ok(response);
    }
}