package com.tavemakers.surf.domain.board.service;

import com.tavemakers.surf.domain.board.dto.request.BoardCategoryCreateReqDTO;
import com.tavemakers.surf.domain.board.dto.response.BoardCategoryResDTO;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.repository.BoardCategoryRepository;
import com.tavemakers.surf.global.logging.LogEvent;
import lombok.RequiredArgsConstructor;
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
        BoardCategory boardCategory = BoardCategory.of(board, req);
        BoardCategory saved = boardCategoryRepository.save(boardCategory);
        return BoardCategoryResDTO.from(saved);
    }
}
