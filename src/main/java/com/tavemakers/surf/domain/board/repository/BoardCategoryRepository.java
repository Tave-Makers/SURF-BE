package com.tavemakers.surf.domain.board.repository;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardCategoryRepository extends JpaRepository<BoardCategory, Long> {

    // 보드 내 모든 카테고리 조회
    List<BoardCategory> findAllByBoardId(Long boardId);

    // 보드 내 슬러그로 조회 (URL 접근용)
    Optional<BoardCategory> findByBoardIdAndSlug(Long boardId, String slug);

    boolean existsByBoardAndSlug(Board board, String slug);
}
