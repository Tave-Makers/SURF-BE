package com.tavemakers.surf.presentation.member.dto.response;

import com.tavemakers.surf.domain.member.entity.Member;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record MyPageProfileResDTO(
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
        boolean isActive,
        boolean blockedByMe,
        List<TrackResDTO> trackList,
        List<CareerResDTO> careerList
) {
    public static MyPageProfileResDTO of(Member member, List<TrackResDTO> trackList, BigDecimal activityScore, List<CareerResDTO> careerList, boolean blockedByMe) {
        boolean isPhoneNumberVisible = !member.isNotOwner() || member.getPhoneNumberPublic();
        return MyPageProfileResDTO.builder()
                .username(member.getName())
                .profileImageUrl(member.getProfileImageUrl())
                .phoneNumberPublic(member.getPhoneNumberPublic())
                .phoneNumber(isPhoneNumberVisible ? member.getPhoneNumber() : null) // 파라미터로 받은 전화번호 사용
                .selfIntroduction(member.getSelfIntroduction())
                .link(member.getLink())
                .email(member.getEmail())
                .university(member.getUniversity())
                .graduateSchool(member.getGraduateSchool())
                .role(member.getRole().name())
                .activityScore(activityScore)
                .isActive((member.isActivityStatus()))
                .blockedByMe(blockedByMe)
                .trackList(trackList)
                .careerList(careerList)
                .build();
    }
}
