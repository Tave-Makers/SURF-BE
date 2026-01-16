package com.tavemakers.surf.domain.board.facade;

import com.tavemakers.surf.domain.board.dto.request.BoardCreateReqDTO;
import com.tavemakers.surf.domain.board.dto.request.BoardUpdateReqDTO;
import com.tavemakers.surf.domain.board.dto.response.BoardResDTO;
import com.tavemakers.surf.domain.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardFacade {
    private final BoardService boardService;

    public BoardResDTO createBoard(BoardCreateReqDTO req) {
        return boardService.createBoard(req);
    }

    public List<BoardResDTO> getBoards() {
        return boardService.getBoards();
    }

    public BoardResDTO getBoard(Long id) {
        return boardService.getBoard(id);
    }

    public BoardResDTO updateBoard(Long id, BoardUpdateReqDTO req) {
        return boardService.updateBoard(id, req);
    }

    public void deleteBoard(Long id) {
        boardService.deleteBoard(id);
    }
}
