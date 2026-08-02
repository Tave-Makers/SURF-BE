package com.tavemakers.surf.presentation.member.dto.response;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.Track;
import com.tavemakers.surf.domain.member.entity.enums.Part;

import java.util.Comparator;
import java.util.List;

public record MemberGroupedByPartResDTO(
        String part,
        String partDisplayName,
        List<MemberCardDTO> members
) {
    public static MemberGroupedByPartResDTO of(Part part, List<MemberCardDTO> members) {
        return new MemberGroupedByPartResDTO(
                part.name(),
                part.getDisplayName(),
                members
        );
    }

    public record MemberCardDTO(
            Long memberId,
            String name,
            String profileImageUrl,
            List<TrackResDTO> tracks
    ) {
        public static MemberCardDTO of(Member member, List<Track> tracks) {
            List<TrackResDTO> trackDtos = tracks.stream()
                    .sorted(Comparator.reverseOrder())
                    .map(TrackResDTO::from)
                    .toList();

            return new MemberCardDTO(
                    member.getId(),
                    member.getName(),
                    member.getProfileImageUrl(),
                    trackDtos
            );
        }
    }
}
