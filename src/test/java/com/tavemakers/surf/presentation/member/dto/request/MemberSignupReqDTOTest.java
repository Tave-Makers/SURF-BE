package com.tavemakers.surf.presentation.member.dto.request;

import com.tavemakers.surf.domain.member.entity.enums.Part;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MemberSignupReqDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("전화번호가 빈 문자열이면 DTO 검증을 통과한다")
    void validate_blankPhoneNumber_passes() {
        MemberSignupReqDTO request = validRequest("");

        Set<ConstraintViolation<MemberSignupReqDTO>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("전화번호가 숫자 10~11자리가 아니면 DTO 검증에 실패한다")
    void validate_invalidPhoneNumber_fails() {
        MemberSignupReqDTO request = validRequest("abc");

        Set<ConstraintViolation<MemberSignupReqDTO>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("phoneNumber");
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("전화번호 형식이 올바르지 않습니다.");
    }

    private MemberSignupReqDTO validRequest(String phoneNumber) {
        MemberSignupReqDTO request = new MemberSignupReqDTO();
        MemberSignupReqDTO.TrackInfo track = new MemberSignupReqDTO.TrackInfo();
        track.setGeneration(15);
        track.setPart(Part.BACKEND);

        ReflectionTestUtils.setField(request, "name", "홍길동");
        ReflectionTestUtils.setField(request, "tracks", List.of(track));
        ReflectionTestUtils.setField(request, "university", "서울과학기술대학교");
        ReflectionTestUtils.setField(request, "graduateSchool", "서울과학기술대학교 대학원");
        ReflectionTestUtils.setField(request, "email", "honggildong@example.com");
        ReflectionTestUtils.setField(request, "phoneNumber", phoneNumber);
        return request;
    }
}
