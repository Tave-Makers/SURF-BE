package com.tavemakers.surf.domain.board.usecase;

import com.tavemakers.surf.domain.board.dto.request.BoardCreateReqDTO;
import com.tavemakers.surf.domain.board.dto.request.BoardUpdateReqDTO;
import com.tavemakers.surf.domain.board.dto.response.BoardResDTO;
import com.tavemakers.surf.domain.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 게시판 Usecase */
@Service
@RequiredArgsConstructor
public class BoardUsecase {

    private final BoardService boardService;

    /** 게시판 생성 */
    @Transactional
    public BoardResDTO createBoard(BoardCreateReqDTO req) {
        return boardService.createBoard(req);
    }

    /** 게시판 목록 조회 */
    @Transactional(readOnly = true)
    public List<BoardResDTO> getBoards() {
        return boardService.getBoards();
    }

    /** 게시판 단건 조회 */
    @Transactional(readOnly = true)
    public BoardResDTO getBoard(Long boardId) {
        return boardService.getBoard(boardId);
    }

    /** 게시판 수정 */
    @Transactional
    public BoardResDTO updateBoard(Long boardId, BoardUpdateReqDTO req) {
        return boardService.updateBoard(boardId, req);
    }

    /** 게시판 삭제 */
    @Transactional
    public void deleteBoard(Long boardId) {
        boardService.deleteBoard(boardId);
    }
}
