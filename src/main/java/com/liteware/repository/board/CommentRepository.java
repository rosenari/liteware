package com.liteware.repository.board;

import com.liteware.model.entity.User;
import com.liteware.model.entity.board.Comment;
import com.liteware.model.entity.board.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    List<Comment> findByPost(Post post);
    
    List<Comment> findByWriter(User writer);
    
    List<Comment> findByParentComment(Comment parentComment);
    
    @Query("SELECT c FROM Comment c WHERE c.post = :post " +
           "ORDER BY COALESCE(c.parentComment.commentId, c.commentId), c.createdAt")
    List<Comment> findByPostOrderByParentCommentAscCreatedAtAsc(@Param("post") Post post);
    
    @Query("SELECT c FROM Comment c WHERE c.writer = :writer " +
           "AND c.isDeleted = false ORDER BY c.createdAt DESC")
    List<Comment> findByWriterOrderByCreatedAtDesc(@Param("writer") User writer);
    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post = :post AND c.isDeleted = false")
    Long countByPost(@Param("post") Post post);
    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.parentComment = :parentComment")
    Long countByParentComment(@Param("parentComment") Comment parentComment);
    
    @Query("SELECT EXISTS(SELECT 1 FROM Comment c WHERE c.parentComment = :parentComment)")
    boolean existsByParentComment(@Param("parentComment") Comment parentComment);
    
    @Query("SELECT c FROM Comment c WHERE c.post = :post AND c.parentComment IS NULL " +
           "ORDER BY c.createdAt DESC")
    List<Comment> findRootCommentsByPost(@Param("post") Post post);
    
    @Query("SELECT c FROM Comment c JOIN FETCH c.writer " +
           "WHERE c.post = :post AND c.isDeleted = false")
    List<Comment> findByPostWithWriter(@Param("post") Post post);
}