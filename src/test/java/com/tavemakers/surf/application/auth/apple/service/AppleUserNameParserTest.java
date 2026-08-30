package com.tavemakers.surf.application.auth.apple.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Apple {@code user} 폼 필드 이름 추출 (이슈 #392) —
 * 이름은 최초 인가 1회만 오는 값이라, 어떤 비정상 입력도 로그인을 실패시키면 안 된다.
 */
class AppleUserNameParserTest {

    private final AppleUserNameParser parser = new AppleUserNameParser(new ObjectMapper());

    @Test
    @DisplayName("정상 페이로드는 한국어 표기 순서(lastName+firstName)로 조합한다")
    void extractsKoreanOrderedName() {
        String payload = "{\"name\":{\"firstName\":\"길동\",\"lastName\":\"홍\"},\"email\":\"user@example.com\"}";

        assertThat(parser.extractName(payload)).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("firstName만 있으면 그 값만 사용한다")
    void extractsPartialName() {
        String payload = "{\"name\":{\"firstName\":\"길동\"}}";

        assertThat(parser.extractName(payload)).isEqualTo("길동");
    }

    @Test
    @DisplayName("name 필드가 없으면 null을 반환한다")
    void returnsNullWhenNameMissing() {
        assertThat(parser.extractName("{\"email\":\"user@example.com\"}")).isNull();
    }

    @Test
    @DisplayName("깨진 JSON이어도 예외 없이 null을 반환한다 — 로그인을 막지 않는다")
    void returnsNullOnMalformedJson() {
        assertThat(parser.extractName("{name: 홍길동")).isNull();
    }

    @Test
    @DisplayName("null/빈 문자열은 null을 반환한다 — 2회차 이후 로그인에는 user가 오지 않는다")
    void returnsNullOnNullOrBlank() {
        assertThat(parser.extractName(null)).isNull();
        assertThat(parser.extractName("  ")).isNull();
    }
}
