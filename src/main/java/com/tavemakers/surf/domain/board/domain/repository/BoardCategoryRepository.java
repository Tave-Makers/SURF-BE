package com.tavemakers.surf.domain.board.domain.repository;

import com.tavemakers.surf.domain.board.domain.entity.Board;
import com.tavemakers.surf.domain.board.domain.entity.BoardCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BoardCategoryRepository extends JpaRepository<BoardCategory, Long> {

    // 보드 내 모든 카테고리 조회
    List<BoardCategory> findAllByBoardId(Long boardId);

    // 전체 카테고리를 게시판과 함께 조회 (N+1 방지, 게시판/카테고리 ID 순 정렬)
    @Query("select bc from BoardCategory bc join fetch bc.board order by bc.board.id asc, bc.id asc")
    List<BoardCategory> findAllWithBoard();

    // 보드 내 슬러그로 조회 (URL 접근용)
    Optional<BoardCategory> findByBoardIdAndSlug(Long boardId, String slug);

    boolean existsByBoardAndSlug(Board board, String slug);
}
