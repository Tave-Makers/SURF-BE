package com.tavemakers.surf.presentation.member.dto.response;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.Track;
import lombok.Builder;

import java.util.Comparator;
import java.util.List;

@Builder
public record MemberSearchDetailResDTO(
        Long memberId,
        String username,
        String university,
        String selfIntroduction,
        String profileImageUrl,
        String role,
        List<TrackResDTO> trackList
) {
    public static MemberSearchDetailResDTO from(Member member) {
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
                .build();
    }
}
