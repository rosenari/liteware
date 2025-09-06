package com.liteware.service.board;

import com.liteware.model.dto.BoardDto;
import com.liteware.model.dto.PostDto;
import com.liteware.model.entity.User;
import com.liteware.model.entity.board.Board;
import com.liteware.model.entity.board.Post;
import com.liteware.repository.UserRepository;
import com.liteware.repository.board.BoardRepository;
import com.liteware.repository.board.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {
    
    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    
    public Board createBoard(BoardDto dto) {
        if (boardRepository.existsByBoardCode(dto.getBoardCode())) {
            throw new RuntimeException("이미 존재하는 게시판 코드입니다: " + dto.getBoardCode());
        }
        
        Board board = Board.builder()
                .boardCode(dto.getBoardCode())
                .boardName(dto.getBoardName())
                .boardType(dto.getBoardType())
                .description(dto.getDescription())
                .useYn(dto.getUseYn() != null ? dto.getUseYn() : true)
                .writeAuthLevel(dto.getWriteAuthLevel() != null ? dto.getWriteAuthLevel() : 1)
                .readAuthLevel(dto.getReadAuthLevel() != null ? dto.getReadAuthLevel() : 1)
                .commentAuthLevel(dto.getCommentAuthLevel() != null ? dto.getCommentAuthLevel() : 1)
                .attachmentYn(dto.getAttachmentYn() != null ? dto.getAttachmentYn() : true)
                .secretYn(dto.getSecretYn() != null ? dto.getSecretYn() : false)
                .noticeYn(dto.getNoticeYn() != null ? dto.getNoticeYn() : true)
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .build();
        
        return boardRepository.save(board);
    }
    
    public Post createPost(PostDto dto) {
        Board board = boardRepository.findById(dto.getBoardId())
                .orElseThrow(() -> new RuntimeException("게시판을 찾을 수 없습니다"));
        
        User writer = userRepository.findById(dto.getWriterId())
                .orElseThrow(() -> new RuntimeException("작성자를 찾을 수 없습니다"));
        
        Post post = Post.builder()
                .board(board)
                .title(dto.getTitle())
                .content(dto.getContent())
                .writer(writer)
                .isNotice(dto.getIsNotice() != null ? dto.getIsNotice() : false)
                .isSecret(dto.getIsSecret() != null ? dto.getIsSecret() : false)
                .noticeStartDate(dto.getNoticeStartDate())
                .noticeEndDate(dto.getNoticeEndDate())
                .build();
        
        return postRepository.save(post);
    }
    
    public Post updatePost(Long postId, PostDto dto, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        if (!post.getWriter().getUserId().equals(userId)) {
            throw new RuntimeException("작성자만 수정할 수 있습니다");
        }
        
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        if (dto.getIsNotice() != null) {
            post.setIsNotice(dto.getIsNotice());
        }
        if (dto.getIsSecret() != null) {
            post.setIsSecret(dto.getIsSecret());
        }
        post.setNoticeStartDate(dto.getNoticeStartDate());
        post.setNoticeEndDate(dto.getNoticeEndDate());
        
        return postRepository.save(post);
    }
    
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        if (!post.getWriter().getUserId().equals(userId)) {
            throw new RuntimeException("작성자만 삭제할 수 있습니다");
        }
        
        postRepository.delete(post);
    }
    
    @Transactional
    public Post getPost(Long postId) {
        Post post = postRepository.findByIdWithBoardAndWriter(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));
        
        post.incrementViewCount();
        postRepository.save(post);
        
        return post;
    }
    
    @Transactional
    public Post getPost(Long postId, Long userId) {
        Post post = postRepository.findByIdWithBoardAndWriter(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        if (post.getIsSecret() && !post.getWriter().getUserId().equals(userId)) {
            throw new RuntimeException("비밀글은 작성자만 조회할 수 있습니다");
        }
        
        post.incrementViewCount();
        postRepository.save(post);
        
        return post;
    }
    
    @Transactional(readOnly = true)
    public Page<Post> getPostsByBoard(Long boardId, Pageable pageable) {
        log.info("getPostsByBoard");
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시판을 찾을 수 없습니다"));
        
        return postRepository.findByBoardAndIsDeletedFalse(board, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<Post> searchPosts(String keyword, Pageable pageable) {
        Page<Post> posts = postRepository.searchPosts(keyword, pageable);
        
        // Writer 정보를 명시적으로 로드
        posts.getContent().forEach(post -> {
            if (post.getWriter() != null) {
                post.getWriter().getName(); // Lazy loading 강제 초기화
                if (post.getWriter().getDepartment() != null) {
                    post.getWriter().getDepartment().getDeptName(); // Department도 초기화
                }
            }
        });
        
        return posts;
    }
    
    @Transactional(readOnly = true)
    public List<Post> getNoticePosts(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시판을 찾을 수 없습니다"));
        
        return postRepository.findByBoardAndIsNoticeOrderByCreatedAtDesc(board, true);
    }
    
    @Transactional(readOnly = true)
    public List<Board> getActiveBoards() {
        return boardRepository.findByUseYnOrderBySortOrder(true);
    }
    
    public Board deactivateBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시판을 찾을 수 없습니다"));
        
        board.setUseYn(false);
        
        return boardRepository.save(board);
    }
    
    @Transactional(readOnly = true)
    public Page<Post> getMyPosts(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        return postRepository.findByWriter(user, pageable);
    }
    
    @Transactional(readOnly = true)
    public List<Post> getRecentNotices(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Post> posts = postRepository.findRecentNotices(pageable);
        
        // Writer 정보를 명시적으로 로드
        posts.forEach(post -> {
            if (post.getWriter() != null) {
                post.getWriter().getName(); // Lazy loading 강제 초기화
                if (post.getWriter().getDepartment() != null) {
                    post.getWriter().getDepartment().getDeptName(); // Department도 초기화
                }
            }
        });
        
        return posts;
    }
    
    @Transactional(readOnly = true)
    public List<Post> getRecentPosts(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Post> posts = postRepository.findRecentPosts(pageable);
        
        // Writer 정보를 명시적으로 로드
        posts.forEach(post -> {
            if (post.getWriter() != null) {
                post.getWriter().getName(); // Lazy loading 강제 초기화
                if (post.getWriter().getDepartment() != null) {
                    post.getWriter().getDepartment().getDeptName(); // Department도 초기화
                }
            }
        });
        
        return posts;
    }
    
    @Transactional(readOnly = true)
    public Long countNewPostsToday() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return postRepository.countPostsCreatedAfter(startOfDay);
    }
    
    /**
     * 전체 게시글 수 조회
     */
    @Transactional(readOnly = true)
    public long getTotalPostsCount() {
        return postRepository.count();
    }
}