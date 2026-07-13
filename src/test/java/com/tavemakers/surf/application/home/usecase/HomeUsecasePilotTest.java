package com.tavemakers.surf.application.home.usecase;

import com.tavemakers.surf.application.home.query.HomeGetService;
import com.tavemakers.surf.domain.home.service.HomeBannerService;
import com.tavemakers.surf.domain.home.service.HomeContentService;
import com.tavemakers.surf.presentation.home.dto.request.HomeBannerCreateReqDTO;
import com.tavemakers.surf.presentation.home.dto.request.HomeBannerReorderReqDTO;
import com.tavemakers.surf.presentation.home.dto.request.HomeBannerUpdateReqDTO;
import com.tavemakers.surf.presentation.home.dto.request.HomeContentUpsertReqDTO;
import com.tavemakers.surf.presentation.home.dto.response.HomeBannerResDTO;
import com.tavemakers.surf.presentation.home.dto.response.HomeContentResDTO;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.application.schedule.query.ScheduleGetService;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * B안(진짜 클린 아키텍처) 파일럿 — home 도메인 동작 잠금.
 *
 * <p>usecase 레벨 시그니처(ReqDTO 수신·ResDTO 반환)는 리팩토링 전후로 동일하므로,
 * 이 테스트는 "도메인 서비스가 DTO를 반환"하던 구조를 "도메인 서비스는 엔티티를 반환하고
 * usecase가 DTO 매핑을 담당"하는 구조로 바꾼 뒤에도 그대로 통과해야 한다(행위 보존 증명).
 *
 * <p>getHome 경로는 이 파일럿의 대상이 아니므로 그 협력자(Member/Schedule/Log)는 mock 처리한다.
 */
@DataJpaTest
@Import({
        HomeUsecase.class,
        HomeBannerService.class,
        HomeContentService.class,
        HomeGetService.class,
        HomeUsecasePilotTest.MockConfig.class,
})
class HomeUsecasePilotTest {

    @TestConfiguration
    static class MockConfig {
        @Bean LogEventEmitter logEventEmitter() { return org.mockito.Mockito.mock(LogEventEmitter.class); }
    }

    @Autowired
    private HomeUsecase homeUsecase;

    @MockBean private MemberGetService memberGetService;
    @MockBean private ScheduleGetService scheduleGetService;

    @Test
    @DisplayName("배너 생성 시 displayOrder가 1부터 순차 증가한다")
    void 배너_생성_순서() {
        HomeBannerResDTO first = homeUsecase.createBanner(new HomeBannerCreateReqDTO("A", "img-a", "link-a"));
        HomeBannerResDTO second = homeUsecase.createBanner(new HomeBannerCreateReqDTO("B", "img-b", "link-b"));

        assertThat(first.displayOrder()).isEqualTo(1);
        assertThat(second.displayOrder()).isEqualTo(2);
        assertThat(first.name()).isEqualTo("A");
    }

    @Test
    @DisplayName("순서 변경 시 orderedIds 순서대로 displayOrder가 재부여된다")
    void 배너_순서변경() {
        Long a = homeUsecase.createBanner(new HomeBannerCreateReqDTO("A", "i", "l")).id();
        Long b = homeUsecase.createBanner(new HomeBannerCreateReqDTO("B", "i", "l")).id();
        Long c = homeUsecase.createBanner(new HomeBannerCreateReqDTO("C", "i", "l")).id();

        List<HomeBannerResDTO> reordered = homeUsecase.reorderBanners(
                new HomeBannerReorderReqDTO(List.of(c, a, b)));

        assertThat(reordered).extracting(HomeBannerResDTO::id).containsExactly(c, a, b);
        assertThat(reordered).extracting(HomeBannerResDTO::displayOrder).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("배너 삭제 시 남은 배너의 displayOrder가 1부터 다시 정렬된다")
    void 배너_삭제_후_재정렬() {
        Long a = homeUsecase.createBanner(new HomeBannerCreateReqDTO("A", "i", "l")).id();
        homeUsecase.createBanner(new HomeBannerCreateReqDTO("B", "i", "l"));
        homeUsecase.createBanner(new HomeBannerCreateReqDTO("C", "i", "l"));

        homeUsecase.deleteBanner(a);

        List<HomeBannerResDTO> remain = homeUsecase.getBanners();
        assertThat(remain).extracting(HomeBannerResDTO::name).containsExactly("B", "C");
        assertThat(remain).extracting(HomeBannerResDTO::displayOrder).containsExactly(1, 2);
    }

    @Test
    @DisplayName("배너 수정은 이름/이미지/링크를 갱신한다")
    void 배너_수정() {
        Long a = homeUsecase.createBanner(new HomeBannerCreateReqDTO("A", "i", "l")).id();

        HomeBannerResDTO updated = homeUsecase.updateBanner(a,
                new HomeBannerUpdateReqDTO("A2", "i2", "l2"));

        assertThat(updated.name()).isEqualTo("A2");
        assertThat(updated.imageUrl()).isEqualTo("i2");
    }

    @Test
    @DisplayName("콘텐츠 upsert는 최초 생성 후 같은 행을 수정한다")
    void 콘텐츠_upsert() {
        HomeContentResDTO created = homeUsecase.upsertContent(new HomeContentUpsertReqDTO("m1", "s1"));
        HomeContentResDTO updated = homeUsecase.upsertContent(new HomeContentUpsertReqDTO("m2", "s2"));

        assertThat(created.id()).isEqualTo(updated.id());
        assertThat(updated.message()).isEqualTo("m2");
        assertThat(updated.sender()).isEqualTo("s2");
    }

    @Test
    @DisplayName("콘텐츠가 없으면 빈 기본값을 반환한다")
    void 콘텐츠_없을때_기본값() {
        HomeContentResDTO content = homeUsecase.getContent();

        assertThat(content.message()).isEmpty();
        assertThat(content.sender()).isEmpty();
    }
}
