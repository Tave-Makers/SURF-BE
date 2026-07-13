package com.tavemakers.surf.domain.board.service;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.board.exception.BoardCategoryAlreadyExistsException;
import com.tavemakers.surf.domain.board.repository.BoardCategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class BoardCategoryServiceTest {

    @Mock
    private BoardCategoryRepository boardCategoryRepository;

    @InjectMocks
    private BoardCategoryService boardCategoryService;

    private final Board board = Board.of("공지사항", BoardType.NOTICE);

    @Test
    @DisplayName("이미 존재하는 슬러그면 saveAndFlush를 호출하지 않고 즉시 예외를 던진다(1차 방어)")
    void createBoardCategory_whenSlugAlreadyExists_throwsWithoutSaving() {
        given(boardCategoryRepository.existsByBoardAndSlug(board, "partnership")).willReturn(true);

        assertThatThrownBy(() -> boardCategoryService.createBoardCategory(board, "제휴", "partnership"))
                .isInstanceOf(BoardCategoryAlreadyExistsException.class);

        then(boardCategoryRepository).should(never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("중복이 없으면 카테고리를 저장하고 반환한다")
    void createBoardCategory_whenNoDuplicate_savesAndReturnsCategory() {
        given(boardCategoryRepository.existsByBoardAndSlug(board, "partnership")).willReturn(false);
        given(boardCategoryRepository.saveAndFlush(any(BoardCategory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        BoardCategory result = boardCategoryService.createBoardCategory(board, "제휴", "partnership");

        assertThat(result.getBoard()).isEqualTo(board);
        assertThat(result.getName()).isEqualTo("제휴");
        assertThat(result.getSlug()).isEqualTo("partnership");
    }

    @Test
    @DisplayName("existsBy 통과 후 저장 시점에 유니크 제약 위반이 발생하면(경쟁 조건) 동일한 예외로 변환한다(2차 방어)")
    void createBoardCategory_whenSaveRaceConditionViolatesUniqueConstraint_throwsAlreadyExists() {
        given(boardCategoryRepository.existsByBoardAndSlug(board, "partnership")).willReturn(false);
        given(boardCategoryRepository.saveAndFlush(any(BoardCategory.class)))
                .willThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> boardCategoryService.createBoardCategory(board, "제휴", "partnership"))
                .isInstanceOf(BoardCategoryAlreadyExistsException.class);
    }

    @Test
    @DisplayName("카테고리 삭제는 repository.delete로 위임한다")
    void deleteBoardCategory_delegatesToRepository() {
        BoardCategory category = BoardCategory.of(board, "제휴", "partnership");

        boardCategoryService.deleteBoardCategory(category);

        then(boardCategoryRepository).should().delete(category);
    }
}
