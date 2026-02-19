package com.tavemakers.surf.domain.member.dto.response;

import com.tavemakers.surf.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Builder
public record MemberRegistrationDetailResDTO(
        Long memberId,
        String username,
        String university,
        String profileImageUrl,
        List<TrackResDTO> trackList,
        String role,
        @Schema(
                description = "가입 상태 (REGISTERING: 가입중, WAITING: 대기, APPROVED: 승인, REJECTED: 거절, WITHDRAWN: 탈퇴)",
                example = "APPROVED",
                allowableValues = {"REGISTERING", "WAITING", "APPROVED", "REJECTED", "WITHDRAWN"}
        )
        String memberStatus,
        boolean isBanned,
        String createdAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yy.MM.dd HH:mm");

    public static MemberRegistrationDetailResDTO from(Member member) {
        return MemberRegistrationDetailResDTO.builder()
                .memberId(member.getId())
                .username(member.getName())
                .university(member.getUniversity())
                .profileImageUrl(member.getProfileImageUrl())
                .trackList(member.getTracks().stream()
                        .map(TrackResDTO::from)
                        .toList())
                .role(member.getRole().name())
                .memberStatus(member.getStatus().name())
                .isBanned(member.isBanned())
                .createdAt(member.getCreatedAt().format(FORMATTER))
                .build();
    }
}
