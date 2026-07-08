package com.tavemakers.surf.domain.post.domain.service.support;

import com.tavemakers.surf.domain.post.domain.dto.PostViewUpdateDto;
import com.tavemakers.surf.domain.post.infrastructure.persistence.PostJdbcRepositoryImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostUpdateService {

    private final PostJdbcRepositoryImpl postJdbcRepository;

    /** 게시글 조회수 일괄 업데이트 */
    public void updateViewCount(List<PostViewUpdateDto> updateDtoList) {
        postJdbcRepository.viewCountBulkUpdate(updateDtoList);
    }

    //게시글과 매핑된 스케쥴 아이디 추가

}
