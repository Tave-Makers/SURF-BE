package com.tavemakers.surf.presentation.home.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record HomeResDTO(

        @Schema(description = "홈 문구", example = "TAVE 신규 회원을 환영합니다.")
        String message,

        @Schema(description = "홈 문구 작성자", example = "TAVE 운영진")
        String sender,

        @Schema(description = "홈 배너 목록")
        List<HomeBannerResDTO> banners,

        @Schema(description = "회원 이름", example = "홍길동")
        String memberName,

        @Schema(description = "회원 기수", example = "17")
        Integer memberGeneration,

        @Schema(description = "회원 파트", example = "프론트엔드")
        String memberPart,

        @Schema(description = "다음 일정 이름", example = "만남의장")
        String nextScheduleTitle,

        @Schema(description = "다음 일정 날짜", example = "01.01")
        String nextScheduleDate,

        @Schema(description = "다음 일정 deeplink", example = "/board/{boardId}/post/{postId}")
        String nextScheduleDeepLink
) {
}