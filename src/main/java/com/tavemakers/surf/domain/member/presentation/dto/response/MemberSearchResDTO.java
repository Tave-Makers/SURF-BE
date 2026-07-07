package com.tavemakers.surf.domain.member.presentation.dto.response;

import com.tavemakers.surf.domain.member.domain.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema; // @Schema 임포트
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "이름 기반 회원 검색 결과 응답 DTO") // 클래스에 대한 설명
public class MemberSearchResDTO {

    @Schema(description = "회원 id", example = "3")
    private final Long memberId;

    @Schema(description = "회원 이름", example = "홍길동")
    private String name;

    @Schema(description = "참여 기수", example = "15")
    private Integer generation;

    @Schema(description = "참여 트랙(파트)", example = "백엔드")
    private String track;

    public static MemberSearchResDTO of(Member member, Integer generation, String track) {
        return MemberSearchResDTO.builder()
                .memberId(member.getId())
                .name(member.getName())
                .generation(generation)
                .track(track)
                .build();
    }
}