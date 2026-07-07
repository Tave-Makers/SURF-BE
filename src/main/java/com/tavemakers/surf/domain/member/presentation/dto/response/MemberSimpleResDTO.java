package com.tavemakers.surf.domain.member.presentation.dto.response;

import com.tavemakers.surf.domain.member.domain.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberSimpleResDTO {

    @Schema(description = "회원 id", example = "3")
    private final Long memberId;

    @Schema(description = "회원 이름", example = "홍길동")
    private final String name;

    public static MemberSimpleResDTO from(Member member) {
        return MemberSimpleResDTO.builder()
                .memberId(member.getId())
                .name(member.getName())
                .build();
    }
}
