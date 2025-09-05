package com.liteware.repository.board;

import com.liteware.model.entity.User;
import com.liteware.model.entity.board.Board;
import com.liteware.model.entity.board.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    Page<Post> findByBoard(Board board, Pageable pageable);
    
    Page<Post> findByWriter(User writer, Pageable pageable);
    
    Page<Post> findByBoardAndIsDeletedFalse(Board board, Pageable pageable);
    
    @Query("SELECT p FROM Post p JOIN FETCH p.board JOIN FETCH p.writer WHERE p.postId = :postId")
    Optional<Post> findByIdWithBoardAndWriter(@Param("postId") Long postId);
    
    @Query("SELECT p FROM Post p WHERE p.board = :board AND p.isNotice = :isNotice " +
           "AND p.isDeleted = false ORDER BY p.createdAt DESC")
    List<Post> findByBoardAndIsNoticeOrderByCreatedAtDesc(@Param("board") Board board, 
                                                          @Param("isNotice") Boolean isNotice);
    
    @Query("SELECT p FROM Post p WHERE p.board = :board AND p.isNotice = true " +
           "AND p.noticeStartDate <= :today AND p.noticeEndDate >= :today " +
           "AND p.isDeleted = false ORDER BY p.createdAt DESC")
    List<Post> findActiveNotices(@Param("board") Board board, @Param("today") LocalDate today);
    
    @Query("SELECT p FROM Post p WHERE (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
           "AND p.isDeleted = false")
    Page<Post> searchPosts(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.board = :board " +
           "AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
           "AND p.isDeleted = false")
    Page<Post> searchPostsInBoard(@Param("board") Board board, @Param("keyword") String keyword, 
                                  Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.writer = :writer AND p.isDeleted = false " +
           "ORDER BY p.createdAt DESC")
    List<Post> findMyPosts(@Param("writer") User writer);
    
    @Query("SELECT COUNT(p) FROM Post p WHERE p.board = :board AND p.isDeleted = false")
    Long countByBoard(@Param("board") Board board);
    
    @Query("SELECT p FROM Post p WHERE p.board = :board AND p.isSecret = false " +
           "AND p.isDeleted = false ORDER BY p.viewCount DESC")
    List<Post> findPopularPosts(@Param("board") Board board, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.board = :board AND p.isDeleted = false " +
           "ORDER BY p.likeCount DESC, p.createdAt DESC")
    Page<Post> findByBoardOrderByLikeCount(@Param("board") Board board, Pageable pageable);
}