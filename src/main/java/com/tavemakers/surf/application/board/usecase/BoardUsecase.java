package com.tavemakers.surf.application.board.usecase;

import com.tavemakers.surf.presentation.board.dto.request.BoardCreateReqDTO;
import com.tavemakers.surf.presentation.board.dto.request.BoardUpdateReqDTO;
import com.tavemakers.surf.presentation.board.dto.response.BoardResDTO;
import com.tavemakers.surf.domain.board.service.BoardService;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.global.logging.LogParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 게시판 Usecase — 트랜잭션 경계를 소유하고 도메인 서비스 결과(엔티티)를 표현형(DTO)으로 매핑한다.
 * 도메인 계층은 DTO를 알지 못한다.
 */
@Service
@RequiredArgsConstructor
public class BoardUsecase {

    private final BoardService boardService;
    private final LogEventEmitter logEventEmitter;

    /** 게시판 생성 */
    @Transactional
    @LogEvent(value = "board.create", message = "게시판 생성 성공")
    public BoardResDTO createBoard(
            BoardCreateReqDTO req
    ) {
        return BoardResDTO.from(boardService.createBoard(req.name(), req.type()));
    }

    /** 게시판 목록 조회 */
    @Transactional(readOnly = true)
    public List<BoardResDTO> getBoards() {
        return boardService.getBoards().stream()
                .map(BoardResDTO::from)
                .toList();
    }

    /** 게시판 단건 조회 */
    @Transactional(readOnly = true)
    public BoardResDTO getBoard(Long boardId) {
        return BoardResDTO.from(boardService.getBoard(boardId));
    }

    /** 게시판 수정 */
    @Transactional
    @LogEvent(value = "board.update", message = "게시판 수정 성공")
    public BoardResDTO updateBoard(
            @LogParam("board_id") Long boardId,
            BoardUpdateReqDTO req
    ) {
        return BoardResDTO.from(boardService.updateBoard(boardId, req.name(), req.type()));
    }

    /** 게시판 삭제 */
    @Transactional
    @LogEvent(value = "board.delete", message = "게시판 삭제 성공")
    public void deleteBoard(
            @LogParam("board_id") Long boardId
    ) {
        boardService.deleteBoard(boardId);
    }
}
