package com.tavemakers.surf.application.scrap.usecase;

import com.tavemakers.surf.application.scrap.query.ScrapGetService;
import com.tavemakers.surf.domain.scrap.service.ScrapService;
import com.tavemakers.surf.presentation.post.dto.response.PostResDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * ScrapUsecase 단위 테스트 — 트랜잭션 경계 소유자로서 ScrapService/ScrapGetService에
 * 정확한 인자로 위임하고 결과를 그대로 반환하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ScrapUsecaseTest {

    @Mock
    private ScrapService scrapService;

    @Mock
    private ScrapGetService scrapGetService;

    @InjectMocks
    private ScrapUsecase scrapUsecase;

    @Test
    @DisplayName("addScrap: memberId, postId 그대로 ScrapService.addScrap에 위임한다")
    void addScrap_ScrapService에_위임한다() {
        scrapUsecase.addScrap(1L, 2L);

        then(scrapService).should().addScrap(1L, 2L);
    }

    @Test
    @DisplayName("removeScrap: memberId, postId 그대로 ScrapService.removeScrap에 위임한다")
    void removeScrap_ScrapService에_위임한다() {
        scrapUsecase.removeScrap(1L, 2L);

        then(scrapService).should().removeScrap(1L, 2L);
    }

    @Test
    @DisplayName("getMyScraps: memberId, pageable 그대로 ScrapGetService에 위임하고 결과를 그대로 반환한다")
    void getMyScraps_ScrapGetService에_위임하고_결과를_그대로_반환한다() {
        Pageable pageable = PageRequest.of(0, 10);
        Slice<PostResDTO> expected = new SliceImpl<>(List.of());
        given(scrapGetService.getMyScraps(1L, pageable)).willReturn(expected);

        Slice<PostResDTO> result = scrapUsecase.getMyScraps(1L, pageable);

        assertThat(result).isSameAs(expected);
        then(scrapGetService).should().getMyScraps(1L, pageable);
    }
}
