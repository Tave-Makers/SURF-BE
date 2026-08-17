package com.tavemakers.surf.global.common.moderation;

import org.ahocorasick.trie.Emit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 금칙어 마스킹 엔진.
 *
 * 원문 매칭 + 정규화 매칭(원문 좌표 역매핑) → 허용표현 폐기 → span 병합 → 치환 순으로 처리한다.
 * 사전은 불변 스냅숏을 volatile 참조로 들고 통째로 교체하므로 락이 필요 없다.
 * DB·파일을 알지 못하며, 사전 적재는 외부에서 {@link #replaceSnapshot}으로 주입한다.
 */
@Component
public class ProfanityMasker {

    private final ModerationProperties properties;
    private volatile DictionarySnapshot snapshot = DictionarySnapshot.empty();

    public ProfanityMasker(ModerationProperties properties) {
        this.properties = properties;
    }

    /** 마스킹된 문자열만 돌려주는 편의 메서드 — null·빈 문자열은 그대로 반환한다. */
    public String mask(String text) {
        return maskWithResult(text).masked();
    }

    /** 마스킹 결과를 매치 정보와 함께 돌려준다 — 로깅용. */
    public MaskingResult maskWithResult(String text) {
        DictionarySnapshot current = this.snapshot;
        if (text == null || text.isEmpty() || !properties.isEnabled() || !current.hasBannedWord()) {
            return new MaskingResult(text, 0, List.of());
        }

        TextNormalizer.Normalized normalized = TextNormalizer.normalize(text);
        Set<Span> bannedSpans = toRawSpans(
                current.findBanned(text), current.findBanned(normalized.text()), normalized);
        if (bannedSpans.isEmpty()) {
            return new MaskingResult(text, 0, List.of());
        }

        Set<Span> allowedSpans = toRawSpans(
                current.findAllowed(text), current.findAllowed(normalized.text()), normalized);
        List<Span> survivors = bannedSpans.stream()
                .filter(banned -> !isCovered(banned, allowedSpans))
                .sorted((left, right) -> Integer.compare(left.start(), right.start()))
                .toList();
        if (survivors.isEmpty()) {
            return new MaskingResult(text, 0, List.of());
        }

        String masked = replace(text, merge(survivors));
        Set<String> matched = new LinkedHashSet<>();
        survivors.forEach(span -> matched.add(span.keyword()));
        return new MaskingResult(masked, survivors.size(), List.copyOf(matched));
    }

    /** 사전 스냅숏을 통째로 교체한다 — 진행 중인 요청은 이전 스냅숏으로 일관되게 끝난다. */
    public void replaceSnapshot(DictionarySnapshot snapshot) {
        this.snapshot = (snapshot == null) ? DictionarySnapshot.empty() : snapshot;
    }

    /** 원문 매치와 정규화 매치를 모두 원문 좌표 span으로 환산해 합친다(중복 제거). */
    private Set<Span> toRawSpans(Collection<Emit> rawEmits,
                                 Collection<Emit> normalizedEmits,
                                 TextNormalizer.Normalized normalized) {
        Set<Span> spans = new LinkedHashSet<>();
        for (Emit emit : rawEmits) {
            spans.add(new Span(emit.getStart(), emit.getEnd() + 1, emit.getKeyword()));
        }
        for (Emit emit : normalizedEmits) {
            spans.add(new Span(
                    normalized.rawStart(emit.getStart()),
                    normalized.rawEnd(emit.getEnd() + 1),
                    emit.getKeyword()));
        }
        return spans;
    }

    /** 금칙어 span이 허용표현 span에 완전히 포함되는지 — 양쪽 모두 원문 좌표다. */
    private boolean isCovered(Span banned, Set<Span> allowedSpans) {
        return allowedSpans.stream()
                .anyMatch(allowed -> allowed.start() <= banned.start() && banned.end() <= allowed.end());
    }

    /** 중첩·인접한 span을 union으로 병합한다 (입력은 시작 위치 오름차순). */
    private List<int[]> merge(List<Span> spans) {
        List<int[]> merged = new ArrayList<>();
        for (Span span : spans) {
            if (!merged.isEmpty() && span.start() <= merged.get(merged.size() - 1)[1]) {
                int[] last = merged.get(merged.size() - 1);
                last[1] = Math.max(last[1], span.end());
                continue;
            }
            merged.add(new int[]{span.start(), span.end()});
        }
        return merged;
    }

    /** 원문의 각 구간을 마스킹 문자 반복으로 치환한다. */
    private String replace(String text, List<int[]> spans) {
        StringBuilder result = new StringBuilder(text.length());
        int cursor = 0;
        for (int[] span : spans) {
            result.append(text, cursor, span[0]);
            result.append(properties.getMaskChar().repeat(span[1] - span[0]));
            cursor = span[1];
        }
        result.append(text, cursor, text.length());
        return result.toString();
    }

    /** 원문 좌표 매치 구간 [start, end). */
    private record Span(int start, int end, String keyword) {
    }
}
