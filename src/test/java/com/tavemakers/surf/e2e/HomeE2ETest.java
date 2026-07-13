package com.tavemakers.surf.e2e;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 홈 화면 조립 E2E.
 * 관통 도메인/계층: presentation.home.HomeController → application.home.HomeUsecase(@Transactional readOnly)
 * → application.home.HomeGetService → domain.home(배너/콘텐츠) + application.member(MemberGetService)
 * + application.schedule(ScheduleGetService). 여러 도메인의 read-model 을 하나의 HomeResDTO 로 직렬화하는
 * 경로 전체가 배선/직렬화 관점에서 깨지지 않았는지 검증한다.
 */
class HomeE2ETest extends E2ESupport {

    @Test
    @DisplayName("로그인 회원의 홈 조회 — 200 + HomeResDTO 직렬화(회원명 포함, 배너/일정은 빈 상태 허용)")
    void getHome_assemblesAcrossDomains() throws Exception {
        Member member = persistMember(MemberRole.MEMBER);

        mockMvc.perform(get("/v1/user/home")
                        .header("Authorization", bearer(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                // member 도메인 조립 결과가 직렬화되는지 (회원명)
                .andExpect(jsonPath("$.data.memberName").value(member.getName()))
                // banners 필드가 존재(빈 배열이라도)해야 직렬화 계약이 유지된 것
                .andExpect(jsonPath("$.data.banners").isArray());
    }
}
