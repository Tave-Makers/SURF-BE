package com.tavemakers.surf.application.board.query;

import com.tavemakers.surf.presentation.board.dto.response.BoardCategoriesGetResDTO;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.board.exception.CategoryNotFoundException;
import com.tavemakers.surf.domain.board.repository.BoardCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardCategoryGetService {
    private final BoardCategoryRepository boardCategoryRepository;

    /** BoardCategory 엔티티 조회, 없으면 CategoryNotFoundException */
    public BoardCategory getCategory(Long id) {
        return boardCategoryRepository.findById(id)
                .orElseThrow(CategoryNotFoundException::new);
    }

    /** 전체 카테고리를 게시판(Board)별로 묶어서 조회 */
    public List<BoardCategoriesGetResDTO> getCategoriesGroupedByBoard() {
        Map<Board, List<BoardCategory>> grouped = new LinkedHashMap<>();
        for (BoardCategory category : boardCategoryRepository.findAllWithBoard()) {
            grouped.computeIfAbsent(category.getBoard(), b -> new ArrayList<>())
                    .add(category);
        }
        return grouped.entrySet().stream()
                .map(entry -> BoardCategoriesGetResDTO.of(entry.getKey(), entry.getValue()))
                .toList();
    }

    /** Board ID와 slug로 BoardCategory 엔티티 조회, 없으면 CategoryNotFoundException */
    public BoardCategory getCategoryByBoardAndSlug(Long boardId, String slug) {
        return boardCategoryRepository.findByBoardIdAndSlug(boardId, slug)
                .orElseThrow(CategoryNotFoundException::new);
    }
}
