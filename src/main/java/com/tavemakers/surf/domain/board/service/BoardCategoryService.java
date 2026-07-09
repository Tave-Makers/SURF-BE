package com.tavemakers.surf.domain.board.service;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.exception.BoardCategoryAlreadyExistsException;
import com.tavemakers.surf.domain.board.repository.BoardCategoryRepository;
import com.tavemakers.surf.global.logging.LogEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 게시판 카테고리 도메인 로직. DTO를 알지 못하며 엔티티만 다룬다.
 * 트랜잭션 경계는 호출자(BoardCategoryUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
public class BoardCategoryService {
    private final BoardCategoryRepository boardCategoryRepository;

    /** 새 게시판 카테고리 생성 */
    @LogEvent(value = "board.category.create", message = "게시판 카테고리 생성 성공")
    public BoardCategory createBoardCategory(Board board, String name, String slug) {
        if (boardCategoryRepository.existsByBoardAndSlug(board, slug)) {
            throw new BoardCategoryAlreadyExistsException();
        }

        BoardCategory boardCategory = BoardCategory.of(board, name, slug);

        try {
            return boardCategoryRepository.saveAndFlush(boardCategory);
        } catch (DataIntegrityViolationException e) {
            throw new BoardCategoryAlreadyExistsException();
        }
    }

    /** 게시판 카테고리 삭제 */
    @LogEvent(value = "board.category.delete", message = "게시판 카테고리 삭제 성공")
    public void deleteBoardCategory(BoardCategory category) {
        boardCategoryRepository.delete(category);
    }
}
