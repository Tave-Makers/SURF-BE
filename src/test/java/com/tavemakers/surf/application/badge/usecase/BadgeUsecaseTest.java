package com.tavemakers.surf.application.badge.usecase;

import com.tavemakers.surf.application.badge.query.BadgeGetService;
import com.tavemakers.surf.domain.badge.entity.Badge;
import com.tavemakers.surf.domain.badge.service.BadgeCreateService;
import com.tavemakers.surf.domain.badge.service.BadgeDeleteService;
import com.tavemakers.surf.domain.badge.service.BadgeUpdateService;
import com.tavemakers.surf.presentation.badge.dto.request.BadgeCreateReqDTO;
import com.tavemakers.surf.presentation.badge.dto.request.BadgeUpdateReqDTO;
import com.tavemakers.surf.presentation.badge.dto.response.BadgeDetailResDTO;
import com.tavemakers.surf.presentation.badge.dto.response.BadgeResDTO;
import com.tavemakers.surf.presentation.badge.dto.response.BadgeSliceResDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * BadgeUsecase 단위 테스트 — 도메인 서비스는 mock 처리하고, DTO ↔ 엔티티 매핑과
 * usecase의 위임(전달 인자)이 올바른지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class BadgeUsecaseTest {

    @Mock
    private BadgeCreateService badgeCreateService;

    @Mock
    private BadgeUpdateService badgeUpdateService;

    @Mock
    private BadgeDeleteService badgeDeleteService;

    @Mock
    private BadgeGetService badgeGetService;

    @InjectMocks
    private BadgeUsecase badgeUsecase;

    @Test
    @DisplayName("배지 생성 요청은 DTO 값을 그대로 생성 서비스에 전달하고 생성된 id를 반환한다")
    void 배지_생성_요청값을_그대로_전달하고_생성된_id를_반환한다() {
        BadgeCreateReqDTO dto = new BadgeCreateReqDTO();
        ReflectionTestUtils.setField(dto, "name", "이름");
        ReflectionTestUtils.setField(dto, "imageUrl", "url");
        ReflectionTestUtils.setField(dto, "description", "설명");
        ReflectionTestUtils.setField(dto, "requirement", "요건");
        given(badgeCreateService.createBadge("이름", "url", "설명", "요건")).willReturn(10L);

        Long id = badgeUsecase.createBadge(dto);

        assertThat(id).isEqualTo(10L);
        then(badgeCreateService).should().createBadge("이름", "url", "설명", "요건");
    }

    @Test
    @DisplayName("배지 리스트 조회는 id 내림차순 페이지네이션으로 조회하고 엔티티 슬라이스를 DTO로 매핑한다")
    void 배지_리스트조회는_id내림차순으로_조회하고_엔티티를_DTO로_매핑한다() {
        Badge b1 = new Badge("A", "imgA", "descA", "reqA");
        ReflectionTestUtils.setField(b1, "id", 2L);
        Badge b2 = new Badge("B", "imgB", "descB", "reqB");
        ReflectionTestUtils.setField(b2, "id", 1L);
        Slice<Badge> slice = new SliceImpl<>(List.of(b1, b2), PageRequest.of(0, 20), false);
        given(badgeGetService.getBadgeList(any(Pageable.class))).willReturn(slice);

        BadgeSliceResDTO result = badgeUsecase.getBadgeList(20, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        then(badgeGetService).should().getBadgeList(captor.capture());
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "id"));

        assertThat(result.content()).extracting(BadgeResDTO::badgeId).containsExactly(2L, 1L);
        assertThat(result.content()).extracting(BadgeResDTO::name).containsExactly("A", "B");
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("배지 단건 조회는 엔티티를 상세 DTO로 매핑한다")
    void 배지_단건조회는_엔티티를_상세DTO로_매핑한다() {
        Badge badge = new Badge("이름", "url", "설명", "요건");
        ReflectionTestUtils.setField(badge, "id", 5L);
        given(badgeGetService.getBadgeDetail(5L)).willReturn(badge);

        BadgeDetailResDTO result = badgeUsecase.getBadgeSingle(5L);

        assertThat(result.badgeId()).isEqualTo(5L);
        assertThat(result.name()).isEqualTo("이름");
        assertThat(result.imageUrl()).isEqualTo("url");
        assertThat(result.description()).isEqualTo("설명");
        assertThat(result.requirement()).isEqualTo("요건");
    }

    @Test
    @DisplayName("배지 수정 요청은 id와 DTO 값을 그대로 수정 서비스로 위임한다")
    void 배지_수정_요청은_id와_DTO값을_그대로_수정서비스로_위임한다() {
        BadgeUpdateReqDTO dto = new BadgeUpdateReqDTO();
        ReflectionTestUtils.setField(dto, "name", "새이름");
        ReflectionTestUtils.setField(dto, "imageUrl", "새url");
        ReflectionTestUtils.setField(dto, "description", "새설명");
        ReflectionTestUtils.setField(dto, "requirement", "새요건");

        badgeUsecase.updateBadge(1L, dto);

        then(badgeUpdateService).should().updateBadge(1L, "새이름", "새url", "새설명", "새요건");
    }

    @Test
    @DisplayName("배지 삭제 요청은 id를 그대로 삭제 서비스로 위임한다")
    void 배지_삭제_요청은_id를_그대로_삭제서비스로_위임한다() {
        badgeUsecase.deleteBadge(7L);

        then(badgeDeleteService).should().deleteBadge(7L);
    }
}
