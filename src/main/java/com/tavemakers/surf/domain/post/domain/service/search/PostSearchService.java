package com.tavemakers.surf.domain.post.domain.service.search;

import com.tavemakers.surf.domain.post.presentation.dto.response.PostResDTO;
import com.tavemakers.surf.domain.post.domain.entity.Post;
import com.tavemakers.surf.domain.post.domain.repository.PostRepository;
import com.tavemakers.surf.domain.post.domain.service.support.FlagsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostSearchService {

    private final PostRepository postRepository;
    private final RecentSearchService recentSearchService;
    private final FlagsMapper flagsMapper;

    /** 게시글 제목 및 내용 검색 */
    public Slice<PostResDTO> search(Long viewerId, String param, Pageable pageable) {
        // 1) 게시글 검색
        Slice<Post> slice = postRepository
                .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(param, param, pageable);

        // 2) 최근 검색어 저장
        recentSearchService.saveQuery(viewerId, param);

        // 3) viewer 기준 scrapped / liked 플래그 조회
        FlagsMapper.Flags flags = flagsMapper.resolveFlags(viewerId, slice.getContent());

        // 4) Post → PostResDTO 매핑 (이미 쓰던 패턴 그대로)
        return slice.map(p -> flagsMapper.toRes(p, flags));
    }
}
