package com.liteware.service.board;

import com.liteware.model.dto.CommentDto;
import com.liteware.model.entity.User;
import com.liteware.model.entity.board.Comment;
import com.liteware.model.entity.board.Post;
import com.liteware.repository.UserRepository;
import com.liteware.repository.board.CommentRepository;
import com.liteware.repository.board.PostRepository;
import com.liteware.service.notification.NotificationService;
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
    private final NotificationService notificationService;
    
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
        
        Comment savedComment = commentRepository.save(comment);
        
        // 게시글 작성자에게 댓글 알림 (본인이 작성한 글에 본인이 댓글을 달면 알림하지 않음)
        if (!post.getWriter().getUserId().equals(writer.getUserId())) {
            notificationService.createCommentNotification(
                post.getWriter().getUserId(),
                post.getPostId(),
                post.getTitle(),
                writer.getName()
            );
        }
        
        // 부모 댓글 작성자에게도 알림 (대댓글의 경우)
        if (dto.getParentCommentId() != null && comment.getParentComment() != null) {
            Comment parentComment = comment.getParentComment();
            if (!parentComment.getWriter().getUserId().equals(writer.getUserId())) {
                notificationService.createCommentNotification(
                    parentComment.getWriter().getUserId(),
                    post.getPostId(),
                    post.getTitle(),
                    writer.getName()
                );
            }
        }
        
        return savedComment;
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
        
        // 모든 댓글을 조회한 후 계층 구조로 재구성
        List<Comment> allComments = commentRepository.findByPostWithWriter(post);
        
        // 부모 댓글들만 필터링하고, 각 부모 댓글에 대댓글 연결
        List<Comment> rootComments = new java.util.ArrayList<>();
        java.util.Map<Long, Comment> commentMap = new java.util.HashMap<>();
        
        // 댓글을 맵에 저장
        for (Comment comment : allComments) {
            commentMap.put(comment.getCommentId(), comment);
            if (comment.getParentComment() == null) {
                rootComments.add(comment);
            }
        }
        
        // 대댓글을 부모 댓글의 childComments에 추가
        for (Comment comment : allComments) {
            if (comment.getParentComment() != null) {
                Comment parent = commentMap.get(comment.getParentComment().getCommentId());
                if (parent != null) {
                    parent.getChildComments().add(comment);
                }
            }
        }
        
        return rootComments;
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