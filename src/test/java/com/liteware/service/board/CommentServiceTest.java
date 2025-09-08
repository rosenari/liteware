package com.liteware.service.board;

import com.liteware.model.dto.BoardDto;
import com.liteware.model.dto.CommentDto;
import com.liteware.model.dto.PostDto;
import com.liteware.model.entity.User;
import com.liteware.model.entity.board.Board;
import com.liteware.model.entity.board.BoardType;
import com.liteware.model.entity.board.Comment;
import com.liteware.model.entity.board.Post;
import com.liteware.service.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentServiceTest extends BaseServiceTest {
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private BoardService boardService;
    
    private User writer;
    private User commenter;
    private User otherUser;
    private Board board;
    private Post post;
    
    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        writer = createUser("writer", "작성자", "writer@example.com", department, position);
        commenter = createUser("commenter", "댓글작성자", "commenter@example.com", department, position);
        otherUser = createUser("other", "다른사용자", "other@example.com", department, position);
        
        writer.addRole(userRole);
        commenter.addRole(userRole);
        otherUser.addRole(userRole);
        
        userRepository.save(writer);
        userRepository.save(commenter);
        userRepository.save(otherUser);
        
        // 테스트 게시판 및 게시글 생성
        board = createTestBoard();
        post = createTestPost();
    }
    
    @Test
    @DisplayName("댓글 작성 성공")
    void createComment_Success() {
        // given
        CommentDto dto = new CommentDto();
        dto.setPostId(post.getPostId());
        dto.setContent("테스트 댓글입니다");
        dto.setWriterId(commenter.getUserId());
        
        // when
        Comment comment = commentService.createComment(dto);
        
        // then
        assertThat(comment).isNotNull();
        assertThat(comment.getContent()).isEqualTo("테스트 댓글입니다");
        assertThat(comment.getWriter().getUserId()).isEqualTo(commenter.getUserId());
        assertThat(comment.getPost().getPostId()).isEqualTo(post.getPostId());
        assertThat(comment.getDepth()).isEqualTo(0);
        assertThat(comment.getParentComment()).isNull();
    }
    
    @Test
    @DisplayName("대댓글 작성 성공")
    void createReplyComment_Success() {
        // given
        // 부모 댓글 생성
        CommentDto parentDto = new CommentDto();
        parentDto.setPostId(post.getPostId());
        parentDto.setContent("부모 댓글");
        parentDto.setWriterId(commenter.getUserId());
        Comment parentComment = commentService.createComment(parentDto);
        
        // 대댓글 DTO
        CommentDto replyDto = new CommentDto();
        replyDto.setPostId(post.getPostId());
        replyDto.setContent("대댓글입니다");
        replyDto.setWriterId(otherUser.getUserId());
        replyDto.setParentCommentId(parentComment.getCommentId());
        
        // when
        Comment replyComment = commentService.createComment(replyDto);
        
        // then
        assertThat(replyComment).isNotNull();
        assertThat(replyComment.getContent()).isEqualTo("대댓글입니다");
        assertThat(replyComment.getParentComment().getCommentId()).isEqualTo(parentComment.getCommentId());
        assertThat(replyComment.getDepth()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("대댓글의 대댓글 작성 시 예외 발생")
    void createReplyToReply_ThrowsException() {
        // given
        // 부모 댓글
        CommentDto parentDto = new CommentDto();
        parentDto.setPostId(post.getPostId());
        parentDto.setContent("부모 댓글");
        parentDto.setWriterId(commenter.getUserId());
        Comment parentComment = commentService.createComment(parentDto);
        
        // 대댓글
        CommentDto replyDto = new CommentDto();
        replyDto.setPostId(post.getPostId());
        replyDto.setContent("대댓글");
        replyDto.setWriterId(otherUser.getUserId());
        replyDto.setParentCommentId(parentComment.getCommentId());
        Comment replyComment = commentService.createComment(replyDto);
        
        // 대댓글의 대댓글 시도
        CommentDto replyToReplyDto = new CommentDto();
        replyToReplyDto.setPostId(post.getPostId());
        replyToReplyDto.setContent("대댓글의 대댓글");
        replyToReplyDto.setWriterId(writer.getUserId());
        replyToReplyDto.setParentCommentId(replyComment.getCommentId());
        
        // when & then
        assertThatThrownBy(() -> commentService.createComment(replyToReplyDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("대댓글의 대댓글은 작성할 수 없습니다");
    }
    
    @Test
    @DisplayName("존재하지 않는 게시글에 댓글 작성 시 예외 발생")
    void createComment_NonExistentPost_ThrowsException() {
        // given
        CommentDto dto = new CommentDto();
        dto.setPostId(999999L);
        dto.setContent("테스트 댓글");
        dto.setWriterId(commenter.getUserId());
        
        // when & then
        assertThatThrownBy(() -> commentService.createComment(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("게시글을 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("댓글 수정 성공")
    void updateComment_Success() {
        // given
        Comment comment = createTestComment();
        String newContent = "수정된 댓글 내용";
        
        // when
        Comment updatedComment = commentService.updateComment(
                comment.getCommentId(), 
                newContent, 
                commenter.getUserId()
        );
        
        // then
        assertThat(updatedComment.getContent()).isEqualTo(newContent);
    }
    
    @Test
    @DisplayName("작성자가 아닌 사용자가 댓글 수정 시 예외 발생")
    void updateComment_NotWriter_ThrowsException() {
        // given
        Comment comment = createTestComment();
        
        // when & then
        assertThatThrownBy(() -> commentService.updateComment(
                comment.getCommentId(), 
                "수정 시도", 
                otherUser.getUserId()
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("작성자만 수정할 수 있습니다");
    }
    
    @Test
    @DisplayName("댓글 삭제 성공 - 대댓글이 없는 경우")
    void deleteComment_NoReplies_Success() {
        // given
        Comment comment = createTestComment();
        Long commentId = comment.getCommentId();
        
        // when
        commentService.deleteComment(commentId, commenter.getUserId());
        
        // then
        List<Comment> comments = commentService.getCommentsByPost(post.getPostId());
        assertThat(comments).isEmpty();
    }
    
    
    @Test
    @DisplayName("작성자가 아닌 사용자가 댓글 삭제 시 예외 발생")
    void deleteComment_NotWriter_ThrowsException() {
        // given
        Comment comment = createTestComment();
        
        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(
                comment.getCommentId(), 
                otherUser.getUserId()
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("작성자만 삭제할 수 있습니다");
    }
    
    @Test
    @DisplayName("게시글의 댓글 목록 조회")
    void getCommentsByPost_Success() {
        // given
        // 부모 댓글들 생성
        createTestComment();
        createTestComment();
        Comment parentComment = createTestComment();
        
        // 대댓글 생성
        CommentDto replyDto = new CommentDto();
        replyDto.setPostId(post.getPostId());
        replyDto.setContent("대댓글");
        replyDto.setWriterId(otherUser.getUserId());
        replyDto.setParentCommentId(parentComment.getCommentId());
        commentService.createComment(replyDto);
        
        // when
        List<Comment> comments = commentService.getCommentsByPost(post.getPostId());
        
        // then
        assertThat(comments).hasSize(3); // 부모 댓글 3개
        Comment commentWithReply = comments.stream()
                .filter(c -> c.getCommentId().equals(parentComment.getCommentId()))
                .findFirst()
                .orElse(null);
        assertThat(commentWithReply).isNotNull();
        assertThat(commentWithReply.getChildComments()).hasSize(1);
    }
    
    @Test
    @DisplayName("게시글의 댓글 수 조회")
    void countCommentsByPost_Success() {
        // given
        createTestComment();
        createTestComment();
        createTestComment();
        
        // when
        Long count = commentService.countCommentsByPost(post.getPostId());
        
        // then
        assertThat(count).isEqualTo(3L);
    }
    
    @Test
    @DisplayName("내가 작성한 댓글 목록 조회")
    void getMyComments_Success() {
        // given
        // commenter가 작성한 댓글들
        createTestComment();
        createTestComment();
        
        // 다른 사용자가 작성한 댓글
        CommentDto otherDto = new CommentDto();
        otherDto.setPostId(post.getPostId());
        otherDto.setContent("다른 사용자 댓글");
        otherDto.setWriterId(otherUser.getUserId());
        commentService.createComment(otherDto);
        
        // when
        List<Comment> myComments = commentService.getMyComments(commenter.getUserId());
        
        // then
        assertThat(myComments).hasSize(2);
        assertThat(myComments).allMatch(comment -> 
                comment.getWriter().getUserId().equals(commenter.getUserId()));
    }
    
    @Test
    @DisplayName("존재하지 않는 댓글 수정 시 예외 발생")
    void updateComment_NonExistent_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> commentService.updateComment(
                999999L, 
                "수정 내용", 
                commenter.getUserId()
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("댓글을 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("존재하지 않는 부모 댓글에 대댓글 작성 시 예외 발생")
    void createReplyComment_NonExistentParent_ThrowsException() {
        // given
        CommentDto dto = new CommentDto();
        dto.setPostId(post.getPostId());
        dto.setContent("대댓글");
        dto.setWriterId(commenter.getUserId());
        dto.setParentCommentId(999999L);
        
        // when & then
        assertThatThrownBy(() -> commentService.createComment(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("부모 댓글을 찾을 수 없습니다");
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
        PostDto dto = new PostDto();
        dto.setBoardId(board.getBoardId());
        dto.setTitle("테스트 게시글");
        dto.setContent("테스트 내용입니다");
        dto.setWriterId(writer.getUserId());
        dto.setIsNotice(false);
        dto.setIsSecret(false);
        return boardService.createPost(dto);
    }
    
    private Comment createTestComment() {
        CommentDto dto = new CommentDto();
        dto.setPostId(post.getPostId());
        dto.setContent("테스트 댓글 " + System.currentTimeMillis());
        dto.setWriterId(commenter.getUserId());
        return commentService.createComment(dto);
    }
}