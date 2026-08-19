package com.tavemakers.surf.presentation.member.dto.response;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.Track;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Comparator;
import java.util.List;

/** 회원 검색 결과 행 */
@Schema(description = "회원 검색 결과")
@Builder
public record MemberSearchDetailResDTO(
        Long memberId,
        String username,
        String university,
        String selfIntroduction,
        String profileImageUrl,
        String role,
        List<TrackResDTO> trackList,

        @Schema(description = "내가 해당 회원을 차단했는지 여부", example = "false")
        boolean blockedByMe
) {
    /** 회원과 차단 여부로 검색 결과 행을 만든다 */
    public static MemberSearchDetailResDTO from(Member member, boolean blockedByMe) {
        List<TrackResDTO> trackDtoList = member.getTracks()
                .stream()
                .sorted(Comparator.comparing(Track::getGeneration).reversed())
                .map(TrackResDTO::from)
                .toList();

        return MemberSearchDetailResDTO.builder()
                .memberId(member.getId())
                .username(member.getName())
                .university(member.getUniversity())
                .selfIntroduction(member.getSelfIntroduction())
                .profileImageUrl(member.getProfileImageUrl())
                .role(member.getRole().name())
                .trackList(trackDtoList)
                .blockedByMe(blockedByMe)
                .build();
    }
}
