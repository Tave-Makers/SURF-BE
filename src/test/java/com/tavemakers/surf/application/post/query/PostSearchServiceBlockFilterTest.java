package com.tavemakers.surf.application.post.query;

import com.tavemakers.surf.application.block.query.BlockGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.repository.PostRepository;
import com.tavemakers.surf.domain.post.service.search.RecentSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * 검색의 차단 필터 배선 (이슈 #370).
 *
 * <p>목록에서 숨긴 글이 검색으로 다시 드러나면 차단이 무의미해진다.
 * 통합 검색·게시판 내 검색 두 경로 모두 필터 쿼리를 타야 한다.
 */
@ExtendWith(MockitoExtension.class)
class PostSearchServiceBlockFilterTest {

    private static final Long VIEWER = 1L;
    private static final Long BOARD_ID = 10L;
    private static final String KEYWORD = "서핑";
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);
    private static final Set<Long> EXCLUDED = Set.of(2L);

    @Mock
    private PostRepository postRepository;
    @Mock
    private RecentSearchService recentSearchService;
    @Mock
    private BlockGetService blockGetService;
    @Mock
    private FlagsMapper flagsMapper;

    @InjectMocks
    private PostSearchService postSearchService;

    @Test
    @DisplayName("통합 검색은 차단 작성자를 제외하는 쿼리를 탄다")
    void 통합_검색도_제외한다() {
        given(blockGetService.getMyBlockedMemberIds(VIEWER)).willReturn(EXCLUDED);
        given(postRepository.searchExcludingAuthors(KEYWORD, EXCLUDED, PAGEABLE))
                .willReturn(new SliceImpl<>(List.<Post>of(), PAGEABLE, false));

        postSearchService.search(VIEWER, KEYWORD, null, null, PAGEABLE);

        then(postRepository).should().searchExcludingAuthors(KEYWORD, EXCLUDED, PAGEABLE);
        then(postRepository).should(never())
                .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("게시판 내 검색도 차단 작성자를 제외하는 쿼리를 탄다")
    void 게시판_내_검색도_제외한다() {
        given(blockGetService.getMyBlockedMemberIds(VIEWER)).willReturn(EXCLUDED);
        given(postRepository.searchInBoardExcludingAuthors(BOARD_ID, KEYWORD, EXCLUDED, PAGEABLE))
                .willReturn(new SliceImpl<>(List.<Post>of(), PAGEABLE, false));

        postSearchService.search(VIEWER, KEYWORD, BOARD_ID, null, PAGEABLE);

        then(postRepository).should().searchInBoardExcludingAuthors(BOARD_ID, KEYWORD, EXCLUDED, PAGEABLE);
        then(postRepository).should(never()).searchInBoard(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("카테고리 지정 검색은 카테고리 필터 쿼리를 타고, 차단 작성자도 제외한다")
    void 카테고리_내_검색도_제외한다() {
        Long categoryId = 100L;
        given(blockGetService.getMyBlockedMemberIds(VIEWER)).willReturn(EXCLUDED);
        given(postRepository.searchInCategoryExcludingAuthors(categoryId, KEYWORD, EXCLUDED, PAGEABLE))
                .willReturn(new SliceImpl<>(List.<Post>of(), PAGEABLE, false));

        postSearchService.search(VIEWER, KEYWORD, BOARD_ID, categoryId, PAGEABLE);

        then(postRepository).should().searchInCategoryExcludingAuthors(categoryId, KEYWORD, EXCLUDED, PAGEABLE);
        then(postRepository).should(never()).searchInBoardExcludingAuthors(anyLong(), anyString(), any(), any());
    }
}
