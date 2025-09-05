package com.liteware.service;

import com.liteware.model.dto.CommentDto;
import com.liteware.model.entity.User;
import com.liteware.model.entity.board.Board;
import com.liteware.model.entity.board.Comment;
import com.liteware.model.entity.board.Post;
import com.liteware.repository.UserRepository;
import com.liteware.repository.board.CommentRepository;
import com.liteware.repository.board.PostRepository;
import com.liteware.service.board.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    
    @Mock
    private CommentRepository commentRepository;
    
    @Mock
    private PostRepository postRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private CommentService commentService;
    
    private User user1;
    private User user2;
    private Post post;
    private Comment comment;
    private Comment reply;
    
    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .userId(1L)
                .loginId("user1")
                .name("사용자1")
                .build();
        
        user2 = User.builder()
                .userId(2L)
                .loginId("user2")
                .name("사용자2")
                .build();
        
        Board board = Board.builder()
                .boardId(1L)
                .boardCode("FREE")
                .boardName("자유게시판")
                .build();
        
        post = Post.builder()
                .postId(1L)
                .board(board)
                .title("게시글 제목")
                .content("게시글 내용")
                .writer(user1)
                .build();
        
        comment = Comment.builder()
                .commentId(1L)
                .post(post)
                .content("댓글 내용")
                .writer(user1)
                .depth(0)
                .build();
        
        reply = Comment.builder()
                .commentId(2L)
                .post(post)
                .content("대댓글 내용")
                .writer(user2)
                .parentComment(comment)
                .depth(1)
                .build();
    }
    
    @Test
    @DisplayName("댓글을 작성할 수 있어야 한다")
    void createComment() {
        CommentDto commentDto = CommentDto.builder()
                .postId(1L)
                .content("새 댓글")
                .writerId(1L)
                .build();
        
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment c = invocation.getArgument(0);
            c.setCommentId(3L);
            return c;
        });
        
        Comment created = commentService.createComment(commentDto);
        
        assertThat(created).isNotNull();
        assertThat(created.getContent()).isEqualTo("새 댓글");
        assertThat(created.getPost()).isEqualTo(post);
        assertThat(created.getWriter()).isEqualTo(user1);
        assertThat(created.getDepth()).isEqualTo(0);
        
        verify(commentRepository).save(any(Comment.class));
    }
    
    @Test
    @DisplayName("대댓글을 작성할 수 있어야 한다")
    void createReply() {
        CommentDto replyDto = CommentDto.builder()
                .postId(1L)
                .content("새 대댓글")
                .writerId(2L)
                .parentCommentId(1L)
                .build();
        
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment c = invocation.getArgument(0);
            c.setCommentId(4L);
            return c;
        });
        
        Comment created = commentService.createComment(replyDto);
        
        assertThat(created).isNotNull();
        assertThat(created.getContent()).isEqualTo("새 대댓글");
        assertThat(created.getParentComment()).isEqualTo(comment);
        assertThat(created.getDepth()).isEqualTo(1);
        
        verify(commentRepository).save(any(Comment.class));
    }
    
    @Test
    @DisplayName("댓글 깊이는 2를 초과할 수 없어야 한다")
    void commentDepthCannotExceedTwo() {
        reply.setDepth(2);
        
        CommentDto deepReplyDto = CommentDto.builder()
                .postId(1L)
                .content("너무 깊은 대댓글")
                .writerId(1L)
                .parentCommentId(2L)
                .build();
        
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(commentRepository.findById(2L)).thenReturn(Optional.of(reply));
        
        assertThatThrownBy(() -> commentService.createComment(deepReplyDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("대댓글의 대댓글은 작성할 수 없습니다");
    }
    
    @Test
    @DisplayName("댓글을 수정할 수 있어야 한다")
    void updateComment() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        
        Comment updated = commentService.updateComment(1L, "수정된 댓글", 1L);
        
        assertThat(updated.getContent()).isEqualTo("수정된 댓글");
        
        verify(commentRepository).save(comment);
    }
    
    @Test
    @DisplayName("작성자가 아닌 사용자는 댓글을 수정할 수 없어야 한다")
    void cannotUpdateCommentByOtherUser() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        
        assertThatThrownBy(() -> commentService.updateComment(1L, "수정 시도", 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("작성자만 수정할 수 있습니다");
    }
    
    @Test
    @DisplayName("댓글을 삭제할 수 있어야 한다")
    void deleteComment() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(commentRepository.existsByParentComment(comment)).thenReturn(false);
        
        commentService.deleteComment(1L, 1L);
        
        verify(commentRepository).delete(comment);
    }
    
    @Test
    @DisplayName("대댓글이 있는 댓글은 삭제 표시만 해야 한다")
    void softDeleteCommentWithReplies() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(commentRepository.existsByParentComment(comment)).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        
        commentService.deleteComment(1L, 1L);
        
        assertThat(comment.getIsDeleted()).isTrue();
        assertThat(comment.getContent()).isEqualTo("삭제된 댓글입니다");
        
        verify(commentRepository).save(comment);
        verify(commentRepository, never()).delete(comment);
    }
    
    @Test
    @DisplayName("작성자가 아닌 사용자는 댓글을 삭제할 수 없어야 한다")
    void cannotDeleteCommentByOtherUser() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        
        assertThatThrownBy(() -> commentService.deleteComment(1L, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("작성자만 삭제할 수 있습니다");
    }
    
    @Test
    @DisplayName("게시글의 댓글 목록을 조회할 수 있어야 한다")
    void getCommentsByPost() {
        List<Comment> comments = Arrays.asList(comment, reply);
        
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.findByPostOrderByParentCommentAscCreatedAtAsc(post))
                .thenReturn(comments);
        
        List<Comment> result = commentService.getCommentsByPost(1L);
        
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(comment, reply);
    }
    
    @Test
    @DisplayName("댓글 수를 조회할 수 있어야 한다")
    void countCommentsByPost() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.countByPost(post)).thenReturn(5L);
        
        Long count = commentService.countCommentsByPost(1L);
        
        assertThat(count).isEqualTo(5L);
    }
    
    @Test
    @DisplayName("내가 작성한 댓글 목록을 조회할 수 있어야 한다")
    void getMyComments() {
        List<Comment> comments = Arrays.asList(comment);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(commentRepository.findByWriterOrderByCreatedAtDesc(user1)).thenReturn(comments);
        
        List<Comment> result = commentService.getMyComments(1L);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWriter()).isEqualTo(user1);
    }
}