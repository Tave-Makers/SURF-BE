package com.tavemakers.surf.domain.member.dto.response;

import com.tavemakers.surf.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Builder
public record MemberInformationResDTO(
        String username,
        String profileImageUrl,
        Boolean phoneNumberPublic,
        String phoneNumber,
        String selfIntroduction,
        String link,
        String email,
        String university,
        String graduateSchool,
        String role,
        BigDecimal activityScore,
        String createdAt,

        @Schema(
                description = "가입 상태 (REGISTERING: 가입중, WAITING: 대기, APPROVED: 승인, REJECTED: 거절, WITHDRAWN: 탈퇴)",
                example = "APPROVED",
                allowableValues = {"REGISTERING", "WAITING", "APPROVED", "REJECTED", "WITHDRAWN"}
        )
        String memberStatus,
        boolean isActive,
        List<TrackResDTO> trackList,
        List<CareerResDTO> careerList
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yy.MM.dd HH:mm");

    public static MemberInformationResDTO of(Member member, List<TrackResDTO> trackList, BigDecimal activityScore, List<CareerResDTO> careerList) {
        return MemberInformationResDTO.builder()
                .username(member.getName())
                .profileImageUrl(member.getProfileImageUrl())
                .phoneNumberPublic(member.getPhoneNumberPublic())
                .phoneNumber(member.getPhoneNumber())
                .selfIntroduction(member.getSelfIntroduction())
                .link(member.getLink())
                .email(member.getEmail())
                .university(member.getUniversity())
                .graduateSchool(member.getGraduateSchool())
                .role(member.getRole().name())
                .activityScore(activityScore)
                .createdAt(member.getCreatedAt().format(FORMATTER))
                .memberStatus(member.getStatus().name())
                .isActive((member.isActivityStatus()))
                .trackList(trackList)
                .careerList(careerList)
                .build();
    }
}
