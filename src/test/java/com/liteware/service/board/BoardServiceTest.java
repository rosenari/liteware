package com.liteware.service.board;

import com.liteware.model.dto.BoardDto;
import com.liteware.model.dto.PostDto;
import com.liteware.model.dto.PostSearchCriteria;
import com.liteware.model.entity.User;
import com.liteware.model.entity.board.Board;
import com.liteware.model.entity.board.BoardType;
import com.liteware.model.entity.board.Post;
import com.liteware.service.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardServiceTest extends BaseServiceTest {
    
    @Autowired
    private BoardService boardService;
    
    private User writer;
    private User otherUser;
    private Board board;
    
    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        writer = createUser("writer", "작성자", "writer@example.com", department, position);
        otherUser = createUser("other", "다른사용자", "other@example.com", department, position);
        
        writer.addRole(userRole);
        otherUser.addRole(userRole);
        
        userRepository.save(writer);
        userRepository.save(otherUser);
    }
    
    @Test
    @DisplayName("게시판 생성 성공")
    void createBoard_Success() {
        // given
        BoardDto dto = new BoardDto();
        dto.setBoardCode("TEST");
        dto.setBoardName("테스트 게시판");
        dto.setBoardType(BoardType.GENERAL);
        dto.setDescription("테스트용 게시판입니다");
        dto.setUseYn(true);
        dto.setWriteAuthLevel(1);
        dto.setReadAuthLevel(1);
        dto.setCommentAuthLevel(1);
        dto.setAttachmentYn(true);
        dto.setSecretYn(false);
        dto.setNoticeYn(true);
        dto.setSortOrder(1);
        
        // when
        Board board = boardService.createBoard(dto);
        
        // then
        assertThat(board).isNotNull();
        assertThat(board.getBoardCode()).isEqualTo("TEST");
        assertThat(board.getBoardName()).isEqualTo("테스트 게시판");
        assertThat(board.getBoardType()).isEqualTo(BoardType.GENERAL);
        assertThat(board.getDescription()).isEqualTo("테스트용 게시판입니다");
        assertThat(board.getUseYn()).isTrue();
    }
    
    @Test
    @DisplayName("중복된 게시판 코드로 생성 시 예외 발생")
    void createBoard_DuplicateCode_ThrowsException() {
        // given
        BoardDto dto1 = new BoardDto();
        dto1.setBoardCode("DUPLICATE");
        dto1.setBoardName("첫번째 게시판");
        dto1.setBoardType(BoardType.GENERAL);
        boardService.createBoard(dto1);
        
        BoardDto dto2 = new BoardDto();
        dto2.setBoardCode("DUPLICATE");
        dto2.setBoardName("두번째 게시판");
        dto2.setBoardType(BoardType.GENERAL);
        
        // when & then
        assertThatThrownBy(() -> boardService.createBoard(dto2))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 존재하는 게시판 코드입니다");
    }
    
    @Test
    @DisplayName("게시글 작성 성공")
    void createPost_Success() {
        // given
        board = createTestBoard();
        
        PostDto dto = new PostDto();
        dto.setBoardId(board.getBoardId());
        dto.setTitle("테스트 게시글");
        dto.setContent("테스트 내용입니다");
        dto.setWriterId(writer.getUserId());
        dto.setIsNotice(false);
        dto.setIsSecret(false);
        
        // when
        Post post = boardService.createPost(dto);
        
        // then
        assertThat(post).isNotNull();
        assertThat(post.getTitle()).isEqualTo("테스트 게시글");
        assertThat(post.getContent()).isEqualTo("테스트 내용입니다");
        assertThat(post.getWriter().getUserId()).isEqualTo(writer.getUserId());
        assertThat(post.getBoard().getBoardId()).isEqualTo(board.getBoardId());
        assertThat(post.getIsNotice()).isFalse();
        assertThat(post.getIsSecret()).isFalse();
    }
    
    @Test
    @DisplayName("공지사항 게시글 작성 성공")
    void createPost_Notice_Success() {
        // given
        board = createTestBoard();
        
        PostDto dto = new PostDto();
        dto.setBoardId(board.getBoardId());
        dto.setTitle("공지사항");
        dto.setContent("중요 공지사항입니다");
        dto.setWriterId(writer.getUserId());
        dto.setIsNotice(true);
        dto.setNoticeStartDate(LocalDate.now());
        dto.setNoticeEndDate(LocalDate.now().plusDays(7));
        
        // when
        Post post = boardService.createPost(dto);
        
        // then
        assertThat(post.getIsNotice()).isTrue();
        assertThat(post.getNoticeStartDate()).isEqualTo(LocalDate.now());
        assertThat(post.getNoticeEndDate()).isEqualTo(LocalDate.now().plusDays(7));
    }
    
    @Test
    @DisplayName("비밀글 작성 성공")
    void createPost_Secret_Success() {
        // given
        board = createTestBoard();
        
        PostDto dto = new PostDto();
        dto.setBoardId(board.getBoardId());
        dto.setTitle("비밀글");
        dto.setContent("비밀 내용입니다");
        dto.setWriterId(writer.getUserId());
        dto.setIsSecret(true);
        
        // when
        Post post = boardService.createPost(dto);
        
        // then
        assertThat(post.getIsSecret()).isTrue();
    }
    
    @Test
    @DisplayName("존재하지 않는 게시판에 게시글 작성 시 예외 발생")
    void createPost_NonExistentBoard_ThrowsException() {
        // given
        PostDto dto = new PostDto();
        dto.setBoardId(999999L);
        dto.setTitle("테스트 게시글");
        dto.setContent("테스트 내용");
        dto.setWriterId(writer.getUserId());
        
        // when & then
        assertThatThrownBy(() -> boardService.createPost(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("게시판을 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("게시글 수정 성공")
    void updatePost_Success() {
        // given
        Post post = createTestPost();
        
        PostDto dto = new PostDto();
        dto.setTitle("수정된 제목");
        dto.setContent("수정된 내용");
        dto.setIsNotice(true);
        
        // when
        Post updatedPost = boardService.updatePost(post.getPostId(), dto, writer.getUserId());
        
        // then
        assertThat(updatedPost.getTitle()).isEqualTo("수정된 제목");
        assertThat(updatedPost.getContent()).isEqualTo("수정된 내용");
        assertThat(updatedPost.getIsNotice()).isTrue();
    }
    
    @Test
    @DisplayName("작성자가 아닌 사용자가 게시글 수정 시 예외 발생")
    void updatePost_NotWriter_ThrowsException() {
        // given
        Post post = createTestPost();
        
        PostDto dto = new PostDto();
        dto.setTitle("수정된 제목");
        dto.setContent("수정된 내용");
        
        // when & then
        assertThatThrownBy(() -> boardService.updatePost(post.getPostId(), dto, otherUser.getUserId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("작성자만 수정할 수 있습니다");
    }
    
    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_Success() {
        // given
        Post post = createTestPost();
        Long postId = post.getPostId();
        
        // when
        boardService.deletePost(postId, writer.getUserId());
        
        // then
        assertThatThrownBy(() -> boardService.getPost(postId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("게시글을 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("작성자가 아닌 사용자가 게시글 삭제 시 예외 발생")
    void deletePost_NotWriter_ThrowsException() {
        // given
        Post post = createTestPost();
        
        // when & then
        assertThatThrownBy(() -> boardService.deletePost(post.getPostId(), otherUser.getUserId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("작성자만 삭제할 수 있습니다");
    }
    
    @Test
    @DisplayName("게시글 조회 및 조회수 증가")
    void getPost_IncrementViewCount() {
        // given
        Post post = createTestPost();
        Integer initialViewCount = post.getViewCount();
        
        // when
        Post viewedPost = boardService.getPost(post.getPostId());
        
        // then
        assertThat(viewedPost.getViewCount()).isEqualTo(initialViewCount + 1);
    }
    
    @Test
    @DisplayName("비밀글 조회 - 작성자는 조회 가능")
    void getPost_SecretPost_WriterCanView() {
        // given
        board = createTestBoard();
        PostDto dto = new PostDto();
        dto.setBoardId(board.getBoardId());
        dto.setTitle("비밀글");
        dto.setContent("비밀 내용");
        dto.setWriterId(writer.getUserId());
        dto.setIsSecret(true);
        Post secretPost = boardService.createPost(dto);
        
        // when
        Post viewedPost = boardService.getPost(secretPost.getPostId(), writer.getUserId());
        
        // then
        assertThat(viewedPost).isNotNull();
        assertThat(viewedPost.getIsSecret()).isTrue();
    }
    
    @Test
    @DisplayName("비밀글 조회 - 작성자가 아닌 사용자는 조회 불가")
    void getPost_SecretPost_OtherUserCannotView() {
        // given
        board = createTestBoard();
        PostDto dto = new PostDto();
        dto.setBoardId(board.getBoardId());
        dto.setTitle("비밀글");
        dto.setContent("비밀 내용");
        dto.setWriterId(writer.getUserId());
        dto.setIsSecret(true);
        Post secretPost = boardService.createPost(dto);
        
        // when & then
        assertThatThrownBy(() -> boardService.getPost(secretPost.getPostId(), otherUser.getUserId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("비밀글은 작성자만 조회할 수 있습니다");
    }
    
    @Test
    @DisplayName("게시판별 게시글 목록 조회")
    void getPostsByBoard_Success() {
        // given
        board = createTestBoard();
        createTestPost();
        createTestPost();
        createTestPost();
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // when
        Page<Post> posts = boardService.getPostsByBoard(board.getBoardId(), pageable);
        
        // then
        assertThat(posts.getTotalElements()).isEqualTo(3);
        assertThat(posts.getContent()).hasSize(3);
    }
    
    @Test
    @DisplayName("게시글 검색")
    void searchPosts_Success() {
        // given
        board = createTestBoard();
        PostDto dto = new PostDto();
        dto.setBoardId(board.getBoardId());
        dto.setTitle("검색 테스트 게시글");
        dto.setContent("검색어가 포함된 내용");
        dto.setWriterId(writer.getUserId());
        boardService.createPost(dto);
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // when
        Page<Post> results = boardService.searchPosts("검색", pageable);
        
        // then
        assertThat(results.getTotalElements()).isGreaterThan(0);
        assertThat(results.getContent()).anyMatch(post -> post.getTitle().contains("검색"));
    }
    
    @Test
    @DisplayName("공지사항 목록 조회")
    void getNoticePosts_Success() {
        // given
        board = createTestBoard();
        
        // 일반 게시글
        PostDto normalDto = new PostDto();
        normalDto.setBoardId(board.getBoardId());
        normalDto.setTitle("일반 게시글");
        normalDto.setContent("일반 내용");
        normalDto.setWriterId(writer.getUserId());
        normalDto.setIsNotice(false);
        boardService.createPost(normalDto);
        
        // 공지사항
        PostDto noticeDto = new PostDto();
        noticeDto.setBoardId(board.getBoardId());
        noticeDto.setTitle("공지사항");
        noticeDto.setContent("공지 내용");
        noticeDto.setWriterId(writer.getUserId());
        noticeDto.setIsNotice(true);
        boardService.createPost(noticeDto);
        
        // when
        List<Post> notices = boardService.getNoticePosts(board.getBoardId());
        
        // then
        assertThat(notices).hasSize(1);
        assertThat(notices.get(0).getIsNotice()).isTrue();
    }
    
    @Test
    @DisplayName("활성화된 게시판 목록 조회")
    void getActiveBoards_Success() {
        // given
        BoardDto activeDto = new BoardDto();
        activeDto.setBoardCode("ACTIVE");
        activeDto.setBoardName("활성 게시판");
        activeDto.setBoardType(BoardType.GENERAL);
        activeDto.setUseYn(true);
        boardService.createBoard(activeDto);
        
        BoardDto inactiveDto = new BoardDto();
        inactiveDto.setBoardCode("INACTIVE");
        inactiveDto.setBoardName("비활성 게시판");
        inactiveDto.setBoardType(BoardType.GENERAL);
        inactiveDto.setUseYn(false);
        boardService.createBoard(inactiveDto);
        
        // when
        List<Board> activeBoards = boardService.getActiveBoards();
        
        // then
        assertThat(activeBoards).allMatch(Board::getUseYn);
        assertThat(activeBoards).noneMatch(b -> b.getBoardCode().equals("INACTIVE"));
    }
    
    @Test
    @DisplayName("게시판 비활성화")
    void deactivateBoard_Success() {
        // given
        board = createTestBoard();
        
        // when
        Board deactivatedBoard = boardService.deactivateBoard(board.getBoardId());
        
        // then
        assertThat(deactivatedBoard.getUseYn()).isFalse();
    }
    
    @Test
    @DisplayName("내가 작성한 게시글 목록 조회")
    void getMyPosts_Success() {
        // given
        board = createTestBoard();
        createTestPost();
        createTestPost();
        
        // 다른 사용자의 게시글
        PostDto otherDto = new PostDto();
        otherDto.setBoardId(board.getBoardId());
        otherDto.setTitle("다른 사용자 게시글");
        otherDto.setContent("다른 내용");
        otherDto.setWriterId(otherUser.getUserId());
        boardService.createPost(otherDto);
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // when
        Page<Post> myPosts = boardService.getMyPosts(writer.getUserId(), pageable);
        
        // then
        assertThat(myPosts.getTotalElements()).isEqualTo(2);
        assertThat(myPosts.getContent()).allMatch(post -> post.getWriter().getUserId().equals(writer.getUserId()));
    }
    
    @Test
    @DisplayName("최근 공지사항 조회")
    void getRecentNotices_Success() {
        // given
        board = createTestBoard();
        for (int i = 0; i < 5; i++) {
            PostDto dto = new PostDto();
            dto.setBoardId(board.getBoardId());
            dto.setTitle("공지사항 " + i);
            dto.setContent("공지 내용 " + i);
            dto.setWriterId(writer.getUserId());
            dto.setIsNotice(true);
            boardService.createPost(dto);
        }
        
        // when
        List<Post> recentNotices = boardService.getRecentNotices(3);
        
        // then
        assertThat(recentNotices).hasSize(3);
        assertThat(recentNotices).allMatch(Post::getIsNotice);
    }
    
    @Test
    @DisplayName("최근 게시글 조회")
    void getRecentPosts_Success() {
        // given
        board = createTestBoard();
        for (int i = 0; i < 5; i++) {
            createTestPost();
        }
        
        // when
        List<Post> recentPosts = boardService.getRecentPosts(3);
        
        // then
        assertThat(recentPosts).hasSize(3);
    }
    
    @Test
    @DisplayName("오늘 작성된 게시글 수 조회")
    void countNewPostsToday_Success() {
        // given
        board = createTestBoard();
        createTestPost();
        createTestPost();
        
        // when
        Long count = boardService.countNewPostsToday();
        
        // then
        assertThat(count).isGreaterThanOrEqualTo(2);
    }
    
    @Test
    @DisplayName("전체 게시글 수 조회")
    void getTotalPostsCount_Success() {
        // given
        board = createTestBoard();
        createTestPost();
        createTestPost();
        createTestPost();
        
        // when
        long count = boardService.getTotalPostsCount();
        
        // then
        assertThat(count).isGreaterThanOrEqualTo(3);
    }
    
    // Helper methods
    private Board createTestBoard() {
        BoardDto dto = new BoardDto();
        dto.setBoardCode("TEST_" + System.currentTimeMillis());
        dto.setBoardName("테스트 게시판");
        dto.setBoardType(BoardType.GENERAL);
        dto.setDescription("테스트용 게시판");
        dto.setUseYn(true);
        return boardService.createBoard(dto);
    }
    
    private Post createTestPost() {
        if (board == null) {
            board = createTestBoard();
        }
        
        PostDto dto = new PostDto();
        dto.setBoardId(board.getBoardId());
        dto.setTitle("테스트 게시글 " + System.currentTimeMillis());
        dto.setContent("테스트 내용입니다");
        dto.setWriterId(writer.getUserId());
        dto.setIsNotice(false);
        dto.setIsSecret(false);
        
        return boardService.createPost(dto);
    }
}