package com.tavemakers.surf.domain.board.usecase;

import com.tavemakers.surf.domain.board.dto.request.BoardCategoryCreateReqDTO;
import com.tavemakers.surf.domain.board.dto.response.BoardCategoryResDTO;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.service.BoardGetService;
import com.tavemakers.surf.domain.board.service.BoardCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardCategoryUsecase {
    private final BoardGetService boardGetService;
    private final BoardCategoryService boardCategoryService;

    @Transactional
    public BoardCategoryResDTO createCategory(Long boardId, BoardCategoryCreateReqDTO req) {
        Board board = boardGetService.getBoard(boardId);
        return boardCategoryService.createBoardCategory(board, req);
    }
}
