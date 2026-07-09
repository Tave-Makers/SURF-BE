package com.tavemakers.surf.domain.board.service;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.board.exception.BoardNotFoundException;
import com.tavemakers.surf.domain.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 게시판 도메인 로직. DTO를 알지 못하며 엔티티만 다룬다.
 * 트랜잭션 경계는 호출자(BoardUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;

    /** 새 게시판 생성 */
    public Board createBoard(String name, BoardType type) {
        Board board = Board.of(name, type);
        return boardRepository.save(board);
    }

    /** 전체 게시판 목록 조회 */
    public List<Board> getBoards() {
        return boardRepository.findAll();
    }

    /** 게시판 단건 조회 */
    public Board getBoard(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(BoardNotFoundException::new);
    }

    /** 게시판 정보 수정 */
    public Board updateBoard(Long id, String name, BoardType type) {
        Board board = boardRepository.findById(id)
                .orElseThrow(BoardNotFoundException::new);
        board.update(name, type);
        return board;
    }

    /** 게시판 삭제 */
    public void deleteBoard(Long id) {
        if (!boardRepository.existsById(id)) throw new BoardNotFoundException();
        boardRepository.deleteById(id);
    }
}
