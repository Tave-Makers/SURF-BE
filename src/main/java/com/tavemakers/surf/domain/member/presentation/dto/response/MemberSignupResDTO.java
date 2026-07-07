package com.tavemakers.surf.domain.member.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.tavemakers.surf.global.logging.LogPropsProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import com.tavemakers.surf.domain.member.domain.entity.Member;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class MemberSignupResDTO implements LogPropsProvider{

    @Schema(description = "회원 ID", example = "1")
    private Long memberId;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String profileImageUrl;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "트랙 정보 리스트")
    private List<TrackResDTO> tracks;

    @Schema(description = "대학교", example = "서울과학기술대학교")
    private String university;

    @Schema(description = "대학원", example = "서울과학기술대학교 대학원")
    private String graduateSchool;

    @Schema(description = "이메일", example = "honggildong@example.com")
    private String email;

    @Schema(description = "전화번호", example = "01012345678")
    private String phoneNumber;

    // 정적 팩토리 메서드
    public static MemberSignupResDTO from(Member member) {
        List<TrackResDTO> tracks = member.getTracks().stream()
                .map(TrackResDTO::from)
                .toList();

        return new MemberSignupResDTO(
                member.getId(),
                member.getProfileImageUrl(),
                member.getName(),
                tracks,
                member.getUniversity(),
                member.getGraduateSchool(),
                member.getEmail(),
                member.getPhoneNumber()
        );
    }

    @Override
    public Map<String, Object> buildProps() {
        return Map.of(
                "member_id", memberId != null ? memberId : 0,
                "email", email != null ? email : "unknown"
        );
    }
}
