package com.tavemakers.surf.global.common.moderation;

import com.tavemakers.surf.global.common.loader.ModerationSeedLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 오탐 회귀 코퍼스 — 최우선 회귀 테스트.
 *
 * <p>소형 가짜 사전이 아니라 <b>실제 시드 파일</b>(moderation/badwords.txt·allowlist.txt)로 사전을 구성한다.
 * 사전을 고칠 때 이 테스트가 전부 통과해야 한다. 실패하면 사전 수정이 정상 문장을 훼손했다는 뜻이다.
 *
 * <p><b>마스킹이 적용된 쓰기 경로</b> — 새 쓰기 경로가 생기면 마스킹 적용과 함께 이 목록을 갱신한다:
 * <ul>
 *   <li>{@code PostCreateUsecase} — post.title / post.content</li>
 *   <li>{@code PostPatchUsecase} — post.title / post.content (withMaskedText)</li>
 *   <li>{@code CommentUsecase.createComment} — comment.content</li>
 *   <li>{@code LetterUsecase.createLetter} — letter.title / letter.content</li>
 *   <li>{@code MemberUsecase.updateProfile} — member.selfIntroduction</li>
 * </ul>
 */
class ProfanityMaskerCorpusTest {

    private static Set<String> bannedWords;
    private static Set<String> allowedPhrases;
    private static ProfanityMasker masker;

    @BeforeAll
    static void loadRealDictionary() {
        // 시드 로더와 같은 파서를 쓴다 — '#' 주석·공백 줄 처리 규칙이 갈라지면 코퍼스가 실사전을 못 재현한다.
        bannedWords = ModerationSeedLoader.readTerms("moderation/badwords.txt");
        allowedPhrases = ModerationSeedLoader.readTerms("moderation/allowlist.txt");

        masker = new ProfanityMasker(new ModerationProperties(true, "*"));
        masker.replaceSnapshot(DictionarySnapshot.of(bannedWords, allowedPhrases));
    }

    // ── 사전 적재 상태 ─────────────────────────────────────────────────

    @Test
    @DisplayName("실제 시드 파일이 주석·공백 줄 없이 적재된다")
    void 시드_파일이_정상_적재된다() {
        assertThat(bannedWords).hasSizeGreaterThanOrEqualTo(500);
        assertThat(allowedPhrases).hasSizeGreaterThanOrEqualTo(20);
        assertThat(bannedWords).noneMatch(word -> word.isBlank() || word.startsWith("#"));
        assertThat(allowedPhrases).noneMatch(phrase -> phrase.isBlank() || phrase.startsWith("#"));
        assertThat(bannedWords).contains("씨발", "씨발놈", "성폭행", "강간");
        assertThat(allowedPhrases).contains("성폭행 예방", "성폭행 피해", "강간죄");
    }

    // ── 오탐 회귀: 정상 문장은 원문 그대로 (사전 큐레이션 사례 + SURF 실사용 문맥) ──

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            // 원본 사전에서 오탐이 났던 문장들
            "[공지] 9월 정기 세션 안내드립니다",
            "운영자님께 문의드립니다",
            "마스터 브랜치에 머지했습니다",
            "아직 결과를 보지 못했어요",
            "이번 프로젝트의 시발점은 작년 세션이었습니다",
            "성교육 이수 증빙 제출 바랍니다",
            "디자인이 좀 허접해서 다시 만들었습니다",
            // 세미나·공지·개발 문맥 (allowlist 안전망 단어 포함)
            "이번 주 세션 발표 자료를 공유드립니다",
            "데이터 분석 파트 지원자 모집합니다",
            "개발 환경 세팅 가이드를 개선했습니다",
            "개인 프로젝트 저장소를 개설했습니다",
            "해커톤 개최 일정 안내드립니다",
            "이년차 회원 대상 멘토링을 진행합니다",
            "지난 이년간 활동 기록입니다",
            "회식비 정액제 지원 규정을 확인해주세요",
            "면접 결과는 아직 보지 못했습니다",
            "프로젝트 진행이 지지부진해서 회의를 잡았습니다",
            "허리띠를 졸라매고 준비했습니다",
            "API 개선 사항 리뷰 부탁드립니다",
            "마스터 브랜치 머지 후 배포 예정입니다 :)",
            "운영자님, 세션 자료 업로드 부탁드립니다!",
            // 강간·성폭행을 사전에 남긴 대신, 정보성 문맥은 allowlist로 통과해야 한다
            "성폭행 예방 교육을 안내드립니다",
            "성폭행 피해 상담 안내",
            "강간죄 처벌 규정"
    })
    @DisplayName("정상 문장은 실사전으로도 원문 그대로 통과한다 (오탐 0건)")
    void 정상_문장은_마스킹되지_않는다(String sentence) {
        assertThat(masker.mask(sentence)).isEqualTo(sentence);
    }

    // ── 양방향 코퍼스: 공격 문맥은 반드시 마스킹 ────────────────────────

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            "너 성폭행할거야",
            "강간하고 싶다고 하더라",
            "씨발 진짜 짜증나네",
            "개새끼야 꺼져"
    })
    @DisplayName("공격 문맥은 allowlist에 걸리지 않고 마스킹된다")
    void 공격_문맥은_마스킹된다(String sentence) {
        String masked = masker.mask(sentence);

        assertThat(masked).isNotEqualTo(sentence);
        assertThat(masked).contains("*");
        assertThat(masker.maskWithResult(sentence).matchCount()).isPositive();
    }

    @Test
    @DisplayName("같은 단어라도 정보성 문맥은 통과하고 공격 문맥만 가려진다 (양방향 검증)")
    void 성폭행_양방향() {
        assertThat(masker.mask("성폭행 예방 교육 이수 안내")).isEqualTo("성폭행 예방 교육 이수 안내");
        assertThat(masker.mask("성폭행할거야")).isEqualTo("***할거야");
    }

    // ── span 병합: 실사전에 존재하는 접두 쌍으로 검증 ───────────────────

    @Test
    @DisplayName("실사전의 접두 쌍(씨발/씨발놈)은 긴 쪽이 온전히 마스킹된다")
    void 실사전_접두쌍_span_병합() {
        // badwords.txt 316행 '씨발', 318행 '씨발놈' — 둘 다 큐레이션 후에도 남아 있는 실존 항목
        assertThat(bannedWords).contains("씨발", "씨발놈");

        assertThat(masker.mask("씨발놈아")).isEqualTo("***아");
        assertThat(masker.mask("저 씨발놈 때문에")).isEqualTo("저 *** 때문에");
        assertThat(masker.maskWithResult("씨발놈").matched()).contains("씨발", "씨발놈");
    }

    // ── 경계: 문자열 끝 매치 · 초장문 ──────────────────────────────────

    @Test
    @DisplayName("금칙어가 문자열 끝에 걸쳐도 마스킹된다")
    void 문자열_끝_매치() {
        assertThat(masker.mask("정말 어이가 없네 씨발")).isEqualTo("정말 어이가 없네 **");
    }

    @Test
    @DisplayName("초장문(정상 텍스트 반복 + 끝 금칙어)에서도 앞부분은 보존하고 끝만 가린다")
    void 초장문_끝_매치() {
        String body = "이번 주 세션 발표 자료를 공유드립니다. 개발 환경 세팅 가이드도 함께 확인해주세요. ".repeat(500);

        String masked = masker.mask(body + "씨발");

        assertThat(masked).hasSize(body.length() + 2);
        assertThat(masked).startsWith(body);
        assertThat(masked).endsWith("**");
    }

    @Test
    @DisplayName("정상 초장문은 한 글자도 훼손되지 않는다")
    void 초장문_오탐_없음() {
        String body = String.join(" ", List.of(
                "[공지] 9월 정기 세션 안내드립니다",
                "마스터 브랜치에 머지했습니다",
                "아직 결과를 보지 못했어요",
                "성폭행 예방 교육을 안내드립니다")).repeat(300);

        assertThat(masker.mask(body)).isEqualTo(body);
    }
}
