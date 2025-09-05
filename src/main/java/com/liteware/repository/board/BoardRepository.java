package com.liteware.repository.board;

import com.liteware.model.entity.board.Board;
import com.liteware.model.entity.board.BoardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    
    Optional<Board> findByBoardCode(String boardCode);
    
    List<Board> findByBoardType(BoardType boardType);
    
    List<Board> findByUseYn(Boolean useYn);
    
    List<Board> findByUseYnOrderBySortOrder(Boolean useYn);
    
    @Query("SELECT b FROM Board b WHERE b.useYn = true ORDER BY b.sortOrder, b.boardId")
    List<Board> findActiveBoards();
    
    @Query("SELECT b FROM Board b WHERE b.boardCode = :boardCode AND b.useYn = true")
    Optional<Board> findActiveBoardByCode(@Param("boardCode") String boardCode);
    
    @Query("SELECT COUNT(b) > 0 FROM Board b WHERE b.boardCode = :boardCode")
    boolean existsByBoardCode(@Param("boardCode") String boardCode);
    
    @Query("SELECT b FROM Board b WHERE b.writeAuthLevel <= :authLevel AND b.useYn = true")
    List<Board> findWritableBoards(@Param("authLevel") Integer authLevel);
    
    @Query("SELECT b FROM Board b WHERE b.readAuthLevel <= :authLevel AND b.useYn = true")
    List<Board> findReadableBoards(@Param("authLevel") Integer authLevel);
}