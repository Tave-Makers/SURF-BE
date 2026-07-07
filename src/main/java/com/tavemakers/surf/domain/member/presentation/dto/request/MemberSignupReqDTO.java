package com.tavemakers.surf.domain.member.presentation.dto.request;

import com.tavemakers.surf.global.logging.LogPropsProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Getter;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

import com.tavemakers.surf.domain.member.domain.entity.enums.Part;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Valid
@Schema(description = "회원가입 요청 DTO")
public class MemberSignupReqDTO implements LogPropsProvider {

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String profileImageUrl;

    @Schema(description = "이름", example = "홍길동")
    @NotBlank(message = "이름은 필수 입력값입니다.")
    private String name;

    // ==== 트랙 정보 (기수 + 파트) ====
    @Schema(description = "트랙 정보 리스트 (기수 + 파트)")
    @NotEmpty(message = "트랙 정보는 최소 1개 이상 필요합니다.")
    @jakarta.validation.Valid
    private List<TrackInfo> tracks;

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "트랙 정보 DTO")
    public static class TrackInfo {

        @Schema(description = "기수", example = "15")
        @NotNull(message = "기수는 필수 입력값입니다.")
        private Integer generation;

        @Schema(description = "파트", example = "BACKEND")
        @NotNull(message = "파트는 필수 입력값입니다.")
        private Part part;
    }

    @Schema(description = "대학교", example = "서울과학기술대학교")
    @NotBlank(message = "대학교는 필수 입력값입니다.")
    private String university;

    @Schema(description = "대학원 (선택)", example = "서울과학기술대학교 대학원")
    private String graduateSchool; // 선택(대학원)

    @Schema(description = "이메일", example = "honggildong@example.com")
    @Email(message = "올바른 이메일 형식을 입력하세요.")
    @NotBlank(message = "이메일은 필수 입력값입니다.")
    private String email;

    @Schema(description = "전화번호", example = "01012345678")
    @NotBlank(message = "전화번호는 필수 입력값입니다.")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "전화번호 형식이 올바르지 않습니다.")
    private String phoneNumber;

    @Override
    public Map<String, Object> buildProps() {
        return Map.of(
                "name_len", name != null ? name.length() : 0,
                "tracks_count", tracks != null ? tracks.size() : 0,
                "university", university
        );
    }
}
