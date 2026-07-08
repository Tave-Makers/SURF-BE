package com.tavemakers.surf.domain.board.application.query;

import com.tavemakers.surf.domain.board.domain.entity.Board;
import com.tavemakers.surf.domain.board.domain.exception.BoardNotFoundException;
import com.tavemakers.surf.domain.board.domain.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardGetService {
    private final BoardRepository boardRepository;

    /** Board 엔티티 조회, 없으면 BoardNotFoundException */
    public Board getBoard(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(BoardNotFoundException::new);
    }

    /** Board 엔티티 조회, 없으면 null 반환 */
    public Board getBoardOrNull(Long id) {
        return boardRepository.findById(id).orElse(null);
    }
}
