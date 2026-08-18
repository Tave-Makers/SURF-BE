package com.tavemakers.surf.global.common.moderation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 순수 엔진 테스트 — 사전은 테스트가 직접 구성한다(엔진은 파일·DB를 모른다). */
class ProfanityMaskerTest {

    private static final List<String> BANNED =
            List.of("씨발", "씨발놈", "시발", "ㅅㅂ", "개새끼", "병신", "보지", "성폭행");
    private static final List<String> ALLOWED =
            List.of("보지 못", "보지 마", "시발점", "성폭행 예방");

    private ProfanityMasker masker;

    @BeforeEach
    void setUp() {
        masker = new ProfanityMasker(new ModerationProperties(true, "*"));
        masker.replaceSnapshot(DictionarySnapshot.of(BANNED, ALLOWED));
    }

    // ── 마스킹 정확도 ───────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "씨발 진짜, ** 진짜",
            "씨*발 어이없네, *** 어이없네",
            "ㅅㅂ 망했다, ** 망했다",
            "개새끼야, ***야",
            "병신같은 소리, **같은 소리",
            "씨 발 뭐야, 씨 발 뭐야"
    })
    @DisplayName("실측 예시대로 마스킹한다")
    void 실측_예시대로_마스킹한다(String raw, String expected) {
        assertThat(masker.mask(raw)).isEqualTo(expected);
    }

    @Test
    @DisplayName("씨*발 → *** : 정규화 매치를 원문 좌표로 역매핑해 3자 전체를 가린다")
    void offset_역매핑으로_원문_전체를_가린다() {
        assertThat(masker.mask("씨*발")).isEqualTo("***");
    }

    @Test
    @DisplayName("중첩 매치는 union으로 병합된다")
    void 중첩_span_병합() {
        assertThat(masker.mask("씨발놈아")).isEqualTo("***아");
    }

    @Test
    @DisplayName("여러 금칙어가 각각 마스킹된다")
    void 다중_매치() {
        assertThat(masker.mask("병신 그리고 개새끼")).isEqualTo("** 그리고 ***");
    }

    // ── 허용 표현 ──────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} (원문 유지)")
    @ValueSource(strings = {
            "아직 결과를 보지 못했어요",
            "그건 보지 마세요",
            "이번 프로젝트의 시발점은 여기다",
            "성폭행 예방 교육을 안내드립니다"
    })
    @DisplayName("허용 표현 구간에 포함되면 마스킹하지 않는다")
    void 허용_표현에_포함되면_마스킹하지_않는다(String raw) {
        assertThat(masker.mask(raw)).isEqualTo(raw);
    }

    @Test
    @DisplayName("허용 표현 밖의 같은 단어는 그대로 마스킹된다")
    void 허용_표현_밖은_마스킹된다() {
        assertThat(masker.mask("시발 뭐야")).isEqualTo("** 뭐야");
        assertThat(masker.mask("성폭행이나 다름없다")).isEqualTo("***이나 다름없다");
    }

    // ── 경계 ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("null은 null로 돌려준다 — PATCH DTO의 '기존값 유지' 계약")
    void null_입력() {
        assertThat(masker.mask(null)).isNull();
        assertThat(masker.maskWithResult(null).masked()).isNull();
    }

    @Test
    @DisplayName("빈 문자열은 빈 문자열로 돌려준다")
    void 빈_문자열_입력() {
        assertThat(masker.mask("")).isEmpty();
    }

    @Test
    @DisplayName("금칙어가 문자열 끝에 걸쳐도 마스킹된다")
    void 문자열_끝_매치() {
        assertThat(masker.mask("진짜 씨발")).isEqualTo("진짜 **");
        assertThat(masker.mask("씨발")).isEqualTo("**");
    }

    @Test
    @DisplayName("이모지가 섞여도 서로게이트 쌍이 깨지지 않는다")
    void 이모지_보존() {
        String raw = "회의 자료 공유합니다 🙂👍";
        assertThat(masker.mask(raw)).isEqualTo(raw);
        assertThat(hasUnpairedSurrogate(masker.mask(raw))).isFalse();

        assertThat(masker.mask("🙂 씨발")).isEqualTo("🙂 **");
        assertThat(masker.mask("🙂 씨발")).contains("🙂");
    }

    @Test
    @DisplayName("이모지가 금칙어 중간에 끼어도 서로게이트 쌍 중간이 잘리지 않는다")
    void 이모지가_금칙어_중간에_끼는_경우() {
        String masked = masker.mask("씨😀발 어이없네");

        assertThat(masked).isEqualTo("**** 어이없네");
        assertThat(hasUnpairedSurrogate(masked)).isFalse();
    }

    @Test
    @DisplayName("대문자로 표기해도 우회되지 않는다 — 트라이 ignoreCase")
    void 대소문자_우회_탐지() {
        ProfanityMasker english = new ProfanityMasker(new ModerationProperties(true, "*"));
        english.replaceSnapshot(DictionarySnapshot.of(List.of("fuck"), List.of("fuck up")));

        assertThat(english.mask("FUCK you")).isEqualTo("**** you");
        assertThat(english.mask("FuCk you")).isEqualTo("**** you");
        assertThat(english.mask("fuck you")).isEqualTo("**** you");
        // 허용 표현도 같은 규칙이라 대문자 표기에서 그대로 살아남는다.
        assertThat(english.mask("Don't FUCK UP")).isEqualTo("Don't FUCK UP");
    }

    @Test
    @DisplayName("사전이 비어 있으면 원문 그대로다")
    void 빈_사전() {
        ProfanityMasker empty = new ProfanityMasker(new ModerationProperties(true, "*"));

        assertThat(empty.mask("씨발 진짜")).isEqualTo("씨발 진짜");
    }

    // ── 설정 ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("enabled=false면 입력을 그대로 반환한다")
    void 킬_스위치() {
        ProfanityMasker disabled = new ProfanityMasker(new ModerationProperties(false, "*"));
        disabled.replaceSnapshot(DictionarySnapshot.of(BANNED, ALLOWED));

        assertThat(disabled.mask("씨발 진짜")).isEqualTo("씨발 진짜");
        assertThat(disabled.mask(null)).isNull();
    }

    @Test
    @DisplayName("maskChar 기본값은 * 이고 설정으로 바꿀 수 있다")
    void 마스킹_문자() {
        ProfanityMasker custom = new ProfanityMasker(new ModerationProperties(null, "#"));
        custom.replaceSnapshot(DictionarySnapshot.of(BANNED, ALLOWED));

        assertThat(custom.mask("씨발 진짜")).isEqualTo("## 진짜");
        assertThat(new ModerationProperties(null, null).isEnabled()).isTrue();
        assertThat(new ModerationProperties(null, null).getMaskChar()).isEqualTo("*");
    }

    @Test
    @DisplayName("maskChar가 두 글자면 기동을 실패시킨다 — 마스킹 결과가 원문보다 길어지는 것을 막는다")
    void 마스킹_문자는_한_글자여야_한다() {
        assertThatThrownBy(() -> new ModerationProperties(true, "**"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moderation.mask-char");
    }

    // ── 스냅숏 교체 ────────────────────────────────────────────────────

    @Test
    @DisplayName("스냅숏을 교체하면 새 사전이 즉시 적용된다")
    void 스냅숏_교체_반영() {
        masker.replaceSnapshot(DictionarySnapshot.of(List.of("잼민이"), List.of()));

        assertThat(masker.mask("잼민이 왔다")).isEqualTo("*** 왔다");
        assertThat(masker.mask("씨발 진짜")).isEqualTo("씨발 진짜");
    }

    @Test
    @DisplayName("empty 스냅숏으로 되돌리면 아무것도 마스킹하지 않는다")
    void empty_스냅숏() {
        masker.replaceSnapshot(DictionarySnapshot.empty());

        assertThat(masker.mask("씨발 진짜")).isEqualTo("씨발 진짜");
    }

    // ── 마스킹 결과 ────────────────────────────────────────────────────

    @Test
    @DisplayName("매치 수와 매칭된 금칙어를 함께 돌려준다")
    void 매치_정보() {
        MaskingResult result = masker.maskWithResult("병신 그리고 개새끼");

        assertThat(result.masked()).isEqualTo("** 그리고 ***");
        assertThat(result.matchCount()).isEqualTo(2);
        assertThat(result.matched()).containsExactlyInAnyOrder("병신", "개새끼");
    }

    @Test
    @DisplayName("원문·정규화 양쪽에서 잡힌 같은 매치는 한 번만 센다")
    void 중복_매치는_한번만_센다() {
        MaskingResult result = masker.maskWithResult("씨발 진짜");

        assertThat(result.matchCount()).isEqualTo(1);
        assertThat(result.matched()).containsExactly("씨발");
    }

    @Test
    @DisplayName("매치가 없으면 빈 결과다")
    void 매치_없음() {
        MaskingResult result = masker.maskWithResult("오늘 세션 안내드립니다");

        assertThat(result.masked()).isEqualTo("오늘 세션 안내드립니다");
        assertThat(result.matchCount()).isZero();
        assertThat(result.matched()).isEmpty();
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
