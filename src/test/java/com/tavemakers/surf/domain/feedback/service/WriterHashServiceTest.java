package com.tavemakers.surf.domain.feedback.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class WriterHashServiceTest {

    private final WriterHashService writerHashService = new WriterHashService();

    @BeforeEach
    void setUp() {
        // @Value 필드는 Spring 컨텍스트 없이 주입되지 않으므로 리플렉션으로 직접 설정한다.
        ReflectionTestUtils.setField(writerHashService, "secret", "test-secret-key");
    }

    @Test
    @DisplayName("동일 회원·동일 날짜는 항상 동일한 해시를 생성한다 (결정적)")
    void 동일_입력은_동일_해시() {
        LocalDate date = LocalDate.of(2026, 7, 10);

        String hash1 = writerHashService.hashDaily(1L, date);
        String hash2 = writerHashService.hashDaily(1L, date);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("회원이 다르면 같은 날짜라도 다른 해시가 생성된다")
    void 회원이_다르면_다른_해시() {
        LocalDate date = LocalDate.of(2026, 7, 10);

        String hash1 = writerHashService.hashDaily(1L, date);
        String hash2 = writerHashService.hashDaily(2L, date);

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("날짜가 다르면 같은 회원이라도 다른 해시가 생성된다 (일 단위 익명화)")
    void 날짜가_다르면_다른_해시() {
        String hash1 = writerHashService.hashDaily(1L, LocalDate.of(2026, 7, 10));
        String hash2 = writerHashService.hashDaily(1L, LocalDate.of(2026, 7, 11));

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("생성된 해시는 SHA-256 hex 형식(64자 소문자 16진수)을 따른다")
    void 해시_형식_검증() {
        String hash = writerHashService.hashDaily(1L, LocalDate.of(2026, 7, 10));

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("비밀키(secret)가 다르면 동일한 회원·날짜 입력이라도 다른 해시가 생성된다")
    void 비밀키가_다르면_다른_해시() {
        LocalDate date = LocalDate.of(2026, 7, 10);
        String hashWithFirstSecret = writerHashService.hashDaily(1L, date);

        ReflectionTestUtils.setField(writerHashService, "secret", "other-secret-key");
        String hashWithSecondSecret = writerHashService.hashDaily(1L, date);

        assertThat(hashWithFirstSecret).isNotEqualTo(hashWithSecondSecret);
    }
}
