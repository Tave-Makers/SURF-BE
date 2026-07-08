package com.tavemakers.surf.domain.board.usecase;

import com.tavemakers.surf.domain.board.dto.request.BoardCategoryCreateReqDTO;
import com.tavemakers.surf.domain.board.dto.response.BoardCategoriesGetResDTO;
import com.tavemakers.surf.domain.board.dto.response.BoardCategoryResDTO;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.exception.CategoryHasPostsException;
import com.tavemakers.surf.domain.board.exception.InvalidCategoryMappingException;
import com.tavemakers.surf.domain.board.service.BoardCategoryGetService;
import com.tavemakers.surf.domain.board.service.BoardCategoryService;
import com.tavemakers.surf.domain.board.service.BoardGetService;
import com.tavemakers.surf.domain.post.application.query.PostGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardCategoryUsecase {
    private final BoardGetService boardGetService;
    private final BoardCategoryService boardCategoryService;
    private final BoardCategoryGetService boardCategoryGetService;
    private final PostGetService postGetService;

    /** 게시판 카테고리를 생성합니다. */
    @Transactional
    public BoardCategoryResDTO createCategory(Long boardId, BoardCategoryCreateReqDTO req) {
        Board board = boardGetService.getBoard(boardId);
        return boardCategoryService.createBoardCategory(board, req);
    }

    /** 전체 카테고리를 게시판(Board)별로 묶어서 조회합니다. */
    @Transactional(readOnly = true)
    public List<BoardCategoriesGetResDTO> getCategoriesGroupedByBoard() {
        return boardCategoryGetService.getCategoriesGroupedByBoard();
    }

    /** 게시판 카테고리를 삭제합니다. (하위 게시글이 없는 경우에만 가능) */
    @Transactional
    public void deleteCategory(Long boardId, Long categoryId) {
        BoardCategory category = boardCategoryGetService.getCategory(categoryId);

        if (!category.getBoard().getId().equals(boardId)) {
            throw new InvalidCategoryMappingException();
        }
        if (postGetService.existsByCategory(categoryId)) {
            throw new CategoryHasPostsException();
        }

        boardCategoryService.deleteBoardCategory(category);
    }
}
