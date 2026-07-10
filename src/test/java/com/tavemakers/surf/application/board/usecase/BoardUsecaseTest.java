package com.tavemakers.surf.application.board.usecase;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.board.service.BoardService;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.presentation.board.dto.request.BoardCreateReqDTO;
import com.tavemakers.surf.presentation.board.dto.request.BoardUpdateReqDTO;
import com.tavemakers.surf.presentation.board.dto.response.BoardResDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * BoardUsecase는 도메인 서비스(BoardService)를 mock 하여
 * "엔티티 → ResDTO 매핑"과 "위임 인자 전달"만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class BoardUsecaseTest {

    @Mock
    private BoardService boardService;

    @Mock
    private LogEventEmitter logEventEmitter;

    @InjectMocks
    private BoardUsecase boardUsecase;

    private Board board(Long id, String name, BoardType type) {
        Board board = Board.of(name, type);
        ReflectionTestUtils.setField(board, "id", id);
        return board;
    }

    @Test
    @DisplayName("게시판 생성은 서비스에 name/type을 위임하고 결과 엔티티를 ResDTO로 매핑한다")
    void createBoard_delegatesAndMapsToResDTO() {
        BoardCreateReqDTO req = new BoardCreateReqDTO("공지사항", BoardType.NOTICE);
        given(boardService.createBoard("공지사항", BoardType.NOTICE))
                .willReturn(board(1L, "공지사항", BoardType.NOTICE));

        BoardResDTO result = boardUsecase.createBoard(req);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("공지사항");
        assertThat(result.type()).isEqualTo(BoardType.NOTICE);
        then(boardService).should().createBoard("공지사항", BoardType.NOTICE);
    }

    @Test
    @DisplayName("게시판 목록 조회는 엔티티 리스트를 순서를 유지한 채 ResDTO 리스트로 매핑한다")
    void getBoards_mapsListToResDTOList() {
        given(boardService.getBoards()).willReturn(List.of(
                board(1L, "공지사항", BoardType.NOTICE),
                board(2L, "자유게시판", BoardType.GENERAL)
        ));

        List<BoardResDTO> result = boardUsecase.getBoards();

        assertThat(result).extracting(BoardResDTO::id).containsExactly(1L, 2L);
        assertThat(result).extracting(BoardResDTO::name).containsExactly("공지사항", "자유게시판");
    }

    @Test
    @DisplayName("게시판 단건 조회는 서비스가 반환한 엔티티를 ResDTO로 매핑한다")
    void getBoard_mapsSingleEntityToResDTO() {
        given(boardService.getBoard(1L)).willReturn(board(1L, "공지사항", BoardType.NOTICE));

        BoardResDTO result = boardUsecase.getBoard(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("공지사항");
    }

    @Test
    @DisplayName("게시판 수정은 boardId와 요청 필드를 그대로 서비스에 위임하고 결과를 매핑한다")
    void updateBoard_delegatesWithCorrectArgsAndMaps() {
        BoardUpdateReqDTO req = new BoardUpdateReqDTO("새이름", BoardType.GENERAL);
        given(boardService.updateBoard(1L, "새이름", BoardType.GENERAL))
                .willReturn(board(1L, "새이름", BoardType.GENERAL));

        BoardResDTO result = boardUsecase.updateBoard(1L, req);

        assertThat(result.name()).isEqualTo("새이름");
        assertThat(result.type()).isEqualTo(BoardType.GENERAL);
        then(boardService).should().updateBoard(1L, "새이름", BoardType.GENERAL);
    }

    @Test
    @DisplayName("게시판 삭제는 boardId를 그대로 서비스에 위임한다")
    void deleteBoard_delegatesToService() {
        boardUsecase.deleteBoard(1L);

        then(boardService).should().deleteBoard(1L);
    }
}
