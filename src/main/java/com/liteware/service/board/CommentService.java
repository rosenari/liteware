package com.liteware.service.board;

import com.liteware.model.dto.CommentDto;
import com.liteware.model.entity.User;
import com.liteware.model.entity.board.Comment;
import com.liteware.model.entity.board.Post;
import com.liteware.repository.UserRepository;
import com.liteware.repository.board.CommentRepository;
import com.liteware.repository.board.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    
    public Comment createComment(CommentDto dto) {
        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));
        
        User writer = userRepository.findById(dto.getWriterId())
                .orElseThrow(() -> new RuntimeException("작성자를 찾을 수 없습니다"));
        
        Comment comment = Comment.builder()
                .post(post)
                .content(dto.getContent())
                .writer(writer)
                .depth(0)
                .build();
        
        if (dto.getParentCommentId() != null) {
            Comment parentComment = commentRepository.findById(dto.getParentCommentId())
                    .orElseThrow(() -> new RuntimeException("부모 댓글을 찾을 수 없습니다"));
            
            if (parentComment.getDepth() >= 2) {
                throw new RuntimeException("대댓글의 대댓글은 작성할 수 없습니다");
            }
            
            comment.setParentComment(parentComment);
            comment.setDepth(parentComment.getDepth() + 1);
        }
        
        return commentRepository.save(comment);
    }
    
    public Comment updateComment(Long commentId, String content, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        if (!comment.getWriter().getUserId().equals(userId)) {
            throw new RuntimeException("작성자만 수정할 수 있습니다");
        }
        
        comment.setContent(content);
        
        return commentRepository.save(comment);
    }
    
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        if (!comment.getWriter().getUserId().equals(userId)) {
            throw new RuntimeException("작성자만 삭제할 수 있습니다");
        }
        
        if (commentRepository.existsByParentComment(comment)) {
            comment.markAsDeleted();
            commentRepository.save(comment);
        } else {
            commentRepository.delete(comment);
        }
    }
    
    @Transactional(readOnly = true)
    public List<Comment> getCommentsByPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));
        
        return commentRepository.findByPostOrderByParentCommentAscCreatedAtAsc(post);
    }
    
    @Transactional(readOnly = true)
    public Long countCommentsByPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다"));
        
        return commentRepository.countByPost(post);
    }
    
    @Transactional(readOnly = true)
    public List<Comment> getMyComments(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        return commentRepository.findByWriterOrderByCreatedAtDesc(user);
    }
}