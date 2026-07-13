package com.tavemakers.surf.application.board.usecase;

import com.tavemakers.surf.application.board.query.BoardCategoryGetService;
import com.tavemakers.surf.application.board.query.BoardGetService;
import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.board.exception.CategoryHasPostsException;
import com.tavemakers.surf.domain.board.exception.InvalidCategoryMappingException;
import com.tavemakers.surf.domain.board.service.BoardCategoryService;
import com.tavemakers.surf.presentation.board.dto.request.BoardCategoryCreateReqDTO;
import com.tavemakers.surf.presentation.board.dto.response.BoardCategoryResDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class BoardCategoryUsecaseTest {

    @Mock
    private BoardGetService boardGetService;

    @Mock
    private BoardCategoryService boardCategoryService;

    @Mock
    private BoardCategoryGetService boardCategoryGetService;

    @Mock
    private PostGetService postGetService;

    @InjectMocks
    private BoardCategoryUsecase boardCategoryUsecase;

    private Board board(Long id) {
        Board board = Board.of("공지사항", BoardType.NOTICE);
        ReflectionTestUtils.setField(board, "id", id);
        return board;
    }

    private BoardCategory category(Long id, Board board) {
        BoardCategory category = BoardCategory.of(board, "제휴", "partnership");
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    @Test
    @DisplayName("카테고리 생성은 boardId로 Board를 조회한 뒤 그 Board로 카테고리를 생성하고 ResDTO로 매핑한다")
    void createCategory_looksUpBoardThenCreatesCategoryAndMaps() {
        Board board = board(1L);
        BoardCategoryCreateReqDTO req = new BoardCategoryCreateReqDTO("제휴", "partnership");
        given(boardGetService.getBoard(1L)).willReturn(board);
        given(boardCategoryService.createBoardCategory(board, "제휴", "partnership"))
                .willReturn(category(10L, board));

        BoardCategoryResDTO result = boardCategoryUsecase.createCategory(1L, req);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.name()).isEqualTo("제휴");
        assertThat(result.slug()).isEqualTo("partnership");
        then(boardGetService).should().getBoard(1L);
        then(boardCategoryService).should().createBoardCategory(board, "제휴", "partnership");
    }

    @Test
    @DisplayName("카테고리가 속한 게시판이 경로의 boardId와 다르면 InvalidCategoryMappingException을 던지고 삭제하지 않는다")
    void deleteCategory_whenBoardMismatch_throwsInvalidCategoryMappingException_withoutDeleting() {
        Board board = board(1L);
        BoardCategory category = category(10L, board);
        given(boardCategoryGetService.getCategory(10L)).willReturn(category);

        assertThatThrownBy(() -> boardCategoryUsecase.deleteCategory(999L, 10L))
                .isInstanceOf(InvalidCategoryMappingException.class);

        then(postGetService).should(never()).existsByCategory(any());
        then(boardCategoryService).should(never()).deleteBoardCategory(any());
    }

    @Test
    @DisplayName("카테고리에 속한 게시글이 있으면 CategoryHasPostsException을 던지고 삭제하지 않는다")
    void deleteCategory_whenCategoryHasPosts_throwsCategoryHasPostsException_withoutDeleting() {
        Board board = board(1L);
        BoardCategory category = category(10L, board);
        given(boardCategoryGetService.getCategory(10L)).willReturn(category);
        given(postGetService.existsByCategory(10L)).willReturn(true);

        assertThatThrownBy(() -> boardCategoryUsecase.deleteCategory(1L, 10L))
                .isInstanceOf(CategoryHasPostsException.class);

        then(boardCategoryService).should(never()).deleteBoardCategory(any());
    }

    @Test
    @DisplayName("게시판이 일치하고 하위 게시글이 없으면 카테고리를 삭제한다")
    void deleteCategory_whenValid_deletesCategory() {
        Board board = board(1L);
        BoardCategory category = category(10L, board);
        given(boardCategoryGetService.getCategory(10L)).willReturn(category);
        given(postGetService.existsByCategory(10L)).willReturn(false);

        boardCategoryUsecase.deleteCategory(1L, 10L);

        then(boardCategoryService).should().deleteBoardCategory(category);
    }
}
