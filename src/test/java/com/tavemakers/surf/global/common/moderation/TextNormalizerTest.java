package com.tavemakers.surf.global.common.moderation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizerTest {

    @Test
    @DisplayName("특수문자는 제거하고 공백은 유지한다 (N1)")
    void 특수문자만_제거한다() {
        TextNormalizer.Normalized normalized = TextNormalizer.normalize("씨*발 어이없네!");

        assertThat(normalized.text()).isEqualTo("씨발 어이없네");
    }

    @Test
    @DisplayName("정규화 좌표를 원문 좌표로 역매핑한다")
    void offset_역매핑() {
        TextNormalizer.Normalized normalized = TextNormalizer.normalize("씨*발");

        assertThat(normalized.text()).isEqualTo("씨발");
        assertThat(normalized.rawIndex()).containsExactly(0, 2);
        assertThat(normalized.rawStart(0)).isZero();
        assertThat(normalized.rawEnd(2)).isEqualTo(3);
    }

    @Test
    @DisplayName("이모지는 code point 단위로 제거되어 서로게이트 쌍이 쪼개지지 않는다")
    void 이모지는_통째로_제거된다() {
        TextNormalizer.Normalized normalized = TextNormalizer.normalize("a😀b");

        assertThat(normalized.text()).isEqualTo("ab");
        assertThat(normalized.rawIndex()).containsExactly(0, 3);
        assertThat(hasUnpairedSurrogate(normalized.text())).isFalse();
    }

    @Test
    @DisplayName("빈 문자열은 빈 결과를 돌려준다")
    void 빈_문자열() {
        TextNormalizer.Normalized normalized = TextNormalizer.normalize("");

        assertThat(normalized.text()).isEmpty();
        assertThat(normalized.rawIndex()).isEmpty();
    }

    /** 짝을 잃은 서로게이트 char가 남아 있는지 검사한다. */
    private boolean hasUnpairedSurrogate(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(i + 1))) {
                    return true;
                }
                i++;
            } else if (Character.isLowSurrogate(c)) {
                return true;
            }
        }
        return false;
    }
}
