package com.liteware.service;

import com.liteware.model.dto.BoardDto;
import com.liteware.model.dto.PostDto;
import com.liteware.model.entity.User;
import com.liteware.model.entity.board.Board;
import com.liteware.model.entity.board.BoardType;
import com.liteware.model.entity.board.Post;
import com.liteware.repository.UserRepository;
import com.liteware.repository.board.BoardRepository;
import com.liteware.repository.board.PostRepository;
import com.liteware.service.board.BoardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {
    
    @Mock
    private BoardRepository boardRepository;
    
    @Mock
    private PostRepository postRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private BoardService boardService;
    
    private User admin;
    private User user;
    private Board noticeBoard;
    private Board freeBoard;
    private Post post;
    
    @BeforeEach
    void setUp() {
        admin = User.builder()
                .userId(1L)
                .loginId("admin")
                .name("관리자")
                .build();
        
        user = User.builder()
                .userId(2L)
                .loginId("user")
                .name("사용자")
                .build();
        
        noticeBoard = Board.builder()
                .boardId(1L)
                .boardCode("NOTICE")
                .boardName("공지사항")
                .boardType(BoardType.NOTICE)
                .description("회사 공지사항 게시판")
                .useYn(true)
                .writeAuthLevel(3)
                .readAuthLevel(1)
                .build();
        
        freeBoard = Board.builder()
                .boardId(2L)
                .boardCode("FREE")
                .boardName("자유게시판")
                .boardType(BoardType.GENERAL)
                .description("자유로운 소통 공간")
                .useYn(true)
                .writeAuthLevel(1)
                .readAuthLevel(1)
                .build();
        
        post = Post.builder()
                .postId(1L)
                .board(noticeBoard)
                .title("공지사항 제목")
                .content("공지사항 내용")
                .writer(admin)
                .viewCount(0)
                .isNotice(false)
                .isSecret(false)
                .build();
    }
    
    @Test
    @DisplayName("게시판을 생성할 수 있어야 한다")
    void createBoard() {
        BoardDto boardDto = BoardDto.builder()
                .boardCode("QNA")
                .boardName("Q&A")
                .boardType(BoardType.GENERAL)
                .description("질문과 답변 게시판")
                .useYn(true)
                .writeAuthLevel(1)
                .readAuthLevel(1)
                .build();
        
        Board savedBoard = Board.builder()
                .boardId(3L)
                .boardCode("QNA")
                .boardName("Q&A")
                .boardType(BoardType.GENERAL)
                .description("질문과 답변 게시판")
                .useYn(true)
                .writeAuthLevel(1)
                .readAuthLevel(1)
                .build();
        
        when(boardRepository.existsByBoardCode("QNA")).thenReturn(false);
        when(boardRepository.save(any(Board.class))).thenReturn(savedBoard);
        
        Board created = boardService.createBoard(boardDto);
        
        assertThat(created).isNotNull();
        assertThat(created.getBoardCode()).isEqualTo("QNA");
        assertThat(created.getBoardName()).isEqualTo("Q&A");
        
        verify(boardRepository).save(any(Board.class));
    }
    
    @Test
    @DisplayName("중복된 게시판 코드로 생성 시 예외가 발생해야 한다")
    void createBoardWithDuplicateCode() {
        BoardDto boardDto = BoardDto.builder()
                .boardCode("NOTICE")
                .boardName("공지사항2")
                .boardType(BoardType.NOTICE)
                .build();
        
        when(boardRepository.existsByBoardCode("NOTICE")).thenReturn(true);
        
        assertThatThrownBy(() -> boardService.createBoard(boardDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 존재하는 게시판 코드입니다");
    }
    
    @Test
    @DisplayName("게시글을 작성할 수 있어야 한다")
    void createPost() {
        PostDto postDto = PostDto.builder()
                .boardId(1L)
                .title("새 공지사항")
                .content("공지사항 내용입니다")
                .writerId(1L)
                .isNotice(true)
                .isSecret(false)
                .build();
        
        when(boardRepository.findById(1L)).thenReturn(Optional.of(noticeBoard));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            p.setPostId(2L);
            return p;
        });
        
        Post created = boardService.createPost(postDto);
        
        assertThat(created).isNotNull();
        assertThat(created.getTitle()).isEqualTo("새 공지사항");
        assertThat(created.getBoard()).isEqualTo(noticeBoard);
        assertThat(created.getWriter()).isEqualTo(admin);
        assertThat(created.getIsNotice()).isTrue();
        
        verify(postRepository).save(any(Post.class));
    }
    
    @Test
    @DisplayName("게시글을 수정할 수 있어야 한다")
    void updatePost() {
        PostDto updateDto = PostDto.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .isNotice(true)
                .build();
        
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(postRepository.save(any(Post.class))).thenReturn(post);
        
        Post updated = boardService.updatePost(1L, updateDto, 1L);
        
        assertThat(updated.getTitle()).isEqualTo("수정된 제목");
        assertThat(updated.getContent()).isEqualTo("수정된 내용");
        assertThat(updated.getIsNotice()).isTrue();
        
        verify(postRepository).save(post);
    }
    
    @Test
    @DisplayName("작성자가 아닌 사용자는 게시글을 수정할 수 없어야 한다")
    void cannotUpdatePostByOtherUser() {
        PostDto updateDto = PostDto.builder()
                .title("수정 시도")
                .content("수정 내용")
                .build();
        
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        
        assertThatThrownBy(() -> boardService.updatePost(1L, updateDto, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("작성자만 수정할 수 있습니다");
    }
    
    @Test
    @DisplayName("게시글을 삭제할 수 있어야 한다")
    void deletePost() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        
        boardService.deletePost(1L, 1L);
        
        verify(postRepository).delete(post);
    }
    
    @Test
    @DisplayName("작성자가 아닌 사용자는 게시글을 삭제할 수 없어야 한다")
    void cannotDeletePostByOtherUser() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        
        assertThatThrownBy(() -> boardService.deletePost(1L, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("작성자만 삭제할 수 있습니다");
    }
    
    @Test
    @DisplayName("게시글을 조회하면 조회수가 증가해야 한다")
    void viewPostIncreasesViewCount() {
        when(postRepository.findByIdWithBoardAndWriter(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);
        
        int initialViewCount = post.getViewCount();
        Post viewed = boardService.getPost(1L);
        
        assertThat(viewed).isNotNull();
        assertThat(viewed.getViewCount()).isEqualTo(initialViewCount + 1);
        
        verify(postRepository).save(post);
    }
    
    @Test
    @DisplayName("게시판별 게시글 목록을 페이징하여 조회할 수 있어야 한다")
    void getPostsByBoard() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Post> posts = Arrays.asList(post);
        Page<Post> postPage = new PageImpl<>(posts, pageable, 1);
        
        when(boardRepository.findById(1L)).thenReturn(Optional.of(noticeBoard));
        when(postRepository.findByBoard(noticeBoard, pageable)).thenReturn(postPage);
        
        Page<Post> result = boardService.getPostsByBoard(1L, pageable);
        
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(post);
    }
    
    @Test
    @DisplayName("게시글을 검색할 수 있어야 한다")
    void searchPosts() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Post> posts = Arrays.asList(post);
        Page<Post> postPage = new PageImpl<>(posts, pageable, 1);
        
        when(postRepository.searchPosts("공지", pageable)).thenReturn(postPage);
        
        Page<Post> result = boardService.searchPosts("공지", pageable);
        
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).contains("공지");
    }
    
    @Test
    @DisplayName("공지글 목록을 조회할 수 있어야 한다")
    void getNoticePosts() {
        post.setIsNotice(true);
        List<Post> noticePosts = Arrays.asList(post);
        
        when(boardRepository.findById(1L)).thenReturn(Optional.of(noticeBoard));
        when(postRepository.findByBoardAndIsNoticeOrderByCreatedAtDesc(noticeBoard, true))
                .thenReturn(noticePosts);
        
        List<Post> result = boardService.getNoticePosts(1L);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsNotice()).isTrue();
    }
    
    @Test
    @DisplayName("활성화된 게시판 목록을 조회할 수 있어야 한다")
    void getActiveBoards() {
        List<Board> boards = Arrays.asList(noticeBoard, freeBoard);
        
        when(boardRepository.findByUseYnOrderBySortOrder(true)).thenReturn(boards);
        
        List<Board> result = boardService.getActiveBoards();
        
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(noticeBoard, freeBoard);
    }
    
    @Test
    @DisplayName("게시판을 비활성화할 수 있어야 한다")
    void deactivateBoard() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(noticeBoard));
        when(boardRepository.save(any(Board.class))).thenReturn(noticeBoard);
        
        Board deactivated = boardService.deactivateBoard(1L);
        
        assertThat(deactivated.getUseYn()).isFalse();
        
        verify(boardRepository).save(noticeBoard);
    }
    
    @Test
    @DisplayName("비밀글은 작성자만 조회할 수 있어야 한다")
    void secretPostCanOnlyBeViewedByWriter() {
        post.setIsSecret(true);
        
        when(postRepository.findByIdWithBoardAndWriter(1L)).thenReturn(Optional.of(post));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        
        assertThatThrownBy(() -> boardService.getPost(1L, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("비밀글은 작성자만 조회할 수 있습니다");
    }
    
    @Test
    @DisplayName("내가 작성한 게시글 목록을 조회할 수 있어야 한다")
    void getMyPosts() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Post> posts = Arrays.asList(post);
        Page<Post> postPage = new PageImpl<>(posts, pageable, 1);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(postRepository.findByWriter(admin, pageable)).thenReturn(postPage);
        
        Page<Post> result = boardService.getMyPosts(1L, pageable);
        
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getWriter()).isEqualTo(admin);
    }
}