package com.tavemakers.surf.application.post.query;

import com.tavemakers.surf.application.block.query.BlockGetService;
import com.tavemakers.surf.presentation.post.dto.response.PostResDTO;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.repository.PostRepository;
import com.tavemakers.surf.domain.post.service.search.RecentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostSearchService {

    private final PostRepository postRepository;
    private final RecentSearchService recentSearchService;
    private final BlockGetService blockGetService;
    private final FlagsMapper flagsMapper;

    /** 게시글 제목 및 내용 검색 — categoryId > boardId 순으로 좁혀 검색 (미지정 시 통합 검색) */
    public Slice<PostResDTO> search(Long viewerId, String param, Long boardId, Long categoryId, Pageable pageable) {
        // 0) 차단 작성자는 쿼리에서 제외한다 — 검색 결과에서도 숨김 정책은 동일하다
        Set<Long> excludedAuthorIds = blockGetService.getMyBlockedMemberIds(viewerId);

        // 1) 게시글 검색 — 카테고리는 게시판에 속하므로 categoryId가 가장 좁은 범위다
        Slice<Post> slice;
        if (categoryId != null) {
            slice = postRepository.searchInCategoryExcludingAuthors(categoryId, param, excludedAuthorIds, pageable);
        } else if (boardId != null) {
            slice = postRepository.searchInBoardExcludingAuthors(boardId, param, excludedAuthorIds, pageable);
        } else {
            slice = postRepository.searchExcludingAuthors(param, excludedAuthorIds, pageable);
        }

        // 2) 최근 검색어 저장
        recentSearchService.saveQuery(viewerId, param);

        // 3) viewer 기준 scrapped / liked 플래그 조회
        FlagsMapper.Flags flags = flagsMapper.resolveFlags(viewerId, slice.getContent());

        // 4) Post → PostResDTO 매핑 (이미 쓰던 패턴 그대로)
        return slice.map(p -> flagsMapper.toRes(p, flags));
    }
}
