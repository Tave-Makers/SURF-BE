package com.tavemakers.surf.domain.board.usecase;

import com.tavemakers.surf.domain.board.dto.request.BoardCreateReqDTO;
import com.tavemakers.surf.domain.board.dto.request.BoardUpdateReqDTO;
import com.tavemakers.surf.domain.board.dto.response.BoardResDTO;
import com.tavemakers.surf.domain.board.service.BoardService;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.global.logging.LogParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** 게시판 Usecase */
@Service
@RequiredArgsConstructor
public class BoardUsecase {

    private final BoardService boardService;
    private final LogEventEmitter logEventEmitter;

    /** 게시판 생성 */
    @Transactional
    public BoardResDTO createBoard(BoardCreateReqDTO req) {

        BoardResDTO result = boardService.createBoard(req);

        logEventEmitter.emit("board.create", Map.of(
                "board_id", result.id(),
                "title_length", req.name().length()
        ));

        return result;

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
    @LogEvent(value = "board.update", message = "게시판 수정")
    public BoardResDTO updateBoard(
            @LogParam("board_id") Long boardId,
            BoardUpdateReqDTO req
    ) {
        return boardService.updateBoard(boardId, req);
    }

    /** 게시판 삭제 */
    @Transactional
    @LogEvent(value = "board.delete", message = "게시판 삭제")
    public void deleteBoard(
            @LogParam("board_id") Long boardId
    ) {
        boardService.deleteBoard(boardId);
    }
}
