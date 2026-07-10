package com.tavemakers.surf.domain.board.service;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.board.exception.BoardNotFoundException;
import com.tavemakers.surf.domain.board.repository.BoardRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @InjectMocks
    private BoardService boardService;

    private Board board(Long id, String name, BoardType type) {
        Board board = Board.of(name, type);
        ReflectionTestUtils.setField(board, "id", id);
        return board;
    }

    @Test
    @DisplayName("게시판 생성은 새 엔티티를 저장하고 반환한다")
    void createBoard_savesAndReturnsBoard() {
        given(boardRepository.save(any(Board.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Board result = boardService.createBoard("공지사항", BoardType.NOTICE);

        assertThat(result.getName()).isEqualTo("공지사항");
        assertThat(result.getType()).isEqualTo(BoardType.NOTICE);
        then(boardRepository).should().save(any(Board.class));
    }

    @Test
    @DisplayName("존재하는 게시판을 조회하면 해당 엔티티를 반환한다")
    void getBoard_found_returnsBoard() {
        Board board = board(1L, "공지사항", BoardType.NOTICE);
        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        Board result = boardService.getBoard(1L);

        assertThat(result).isSameAs(board);
    }

    @Test
    @DisplayName("존재하지 않는 게시판을 조회하면 BoardNotFoundException이 발생한다")
    void getBoard_notFound_throwsBoardNotFoundException() {
        given(boardRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.getBoard(999L))
                .isInstanceOf(BoardNotFoundException.class);
    }

    @Test
    @DisplayName("게시판 수정은 조회된 엔티티의 이름/타입을 변경하고 반환한다")
    void updateBoard_found_updatesNameAndType() {
        Board board = board(1L, "옛이름", BoardType.GENERAL);
        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        Board result = boardService.updateBoard(1L, "새이름", BoardType.NOTICE);

        assertThat(result).isSameAs(board);
        assertThat(result.getName()).isEqualTo("새이름");
        assertThat(result.getType()).isEqualTo(BoardType.NOTICE);
    }

    @Test
    @DisplayName("존재하지 않는 게시판을 수정하면 BoardNotFoundException이 발생한다")
    void updateBoard_notFound_throwsBoardNotFoundException() {
        given(boardRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.updateBoard(999L, "새이름", BoardType.NOTICE))
                .isInstanceOf(BoardNotFoundException.class);
    }

    @Test
    @DisplayName("존재하는 게시판을 삭제하면 repository.deleteById가 호출된다")
    void deleteBoard_exists_deletesById() {
        given(boardRepository.existsById(1L)).willReturn(true);

        boardService.deleteBoard(1L);

        then(boardRepository).should().deleteById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 게시판을 삭제하면 BoardNotFoundException이 발생하고 deleteById는 호출되지 않는다")
    void deleteBoard_notExists_throwsBoardNotFoundException_andNeverDeletes() {
        given(boardRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> boardService.deleteBoard(999L))
                .isInstanceOf(BoardNotFoundException.class);

        then(boardRepository).should(never()).deleteById(any());
    }
}
