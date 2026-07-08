package com.tavemakers.surf.domain.board.domain.service;

import com.tavemakers.surf.domain.board.presentation.dto.request.BoardCategoryCreateReqDTO;
import com.tavemakers.surf.domain.board.presentation.dto.response.BoardCategoryResDTO;
import com.tavemakers.surf.domain.board.domain.entity.Board;
import com.tavemakers.surf.domain.board.domain.entity.BoardCategory;
import com.tavemakers.surf.domain.board.domain.exception.BoardCategoryAlreadyExistsException;
import com.tavemakers.surf.domain.board.domain.repository.BoardCategoryRepository;
import com.tavemakers.surf.global.logging.LogEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardCategoryService {
    private final BoardCategoryRepository boardCategoryRepository;

    /** 새 게시판 카테고리 생성 */
    @Transactional
    @LogEvent(value = "board.category.create", message = "게시판 카테고리 생성 성공")
    public BoardCategoryResDTO createBoardCategory(Board board, BoardCategoryCreateReqDTO req) {
        if (boardCategoryRepository.existsByBoardAndSlug(board, req.slug())) {
            throw new BoardCategoryAlreadyExistsException();
        }

        BoardCategory boardCategory = BoardCategory.of(board, req);

        try {
            BoardCategory saved = boardCategoryRepository.saveAndFlush(boardCategory);
            return BoardCategoryResDTO.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BoardCategoryAlreadyExistsException();
        }
    }

    /** 게시판 카테고리 삭제 */
    @Transactional
    @LogEvent(value = "board.category.delete", message = "게시판 카테고리 삭제 성공")
    public void deleteBoardCategory(BoardCategory category) {
        boardCategoryRepository.delete(category);
    }
}
