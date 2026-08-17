package com.tavemakers.surf.global.common.moderation;

import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 금칙어·허용표현 Aho-Corasick 트라이 쌍을 담는 불변 스냅숏.
 *
 * 갱신은 이 객체를 통째로 교체하는 방식이므로 내부 상태는 생성 후 변하지 않는다.
 */
public final class DictionarySnapshot {

    private final Trie bannedTrie;
    private final Trie allowedTrie;

    private DictionarySnapshot(Trie bannedTrie, Trie allowedTrie) {
        this.bannedTrie = bannedTrie;
        this.allowedTrie = allowedTrie;
    }

    /** 금칙어·허용표현 목록으로 스냅숏을 만든다 (null·공백 항목은 무시). */
    public static DictionarySnapshot of(Collection<String> bannedWords, Collection<String> allowedPhrases) {
        return new DictionarySnapshot(buildTrie(bannedWords), buildTrie(allowedPhrases));
    }

    /** 사전이 비어 있는 스냅숏 — 기동 직후 초기값. */
    public static DictionarySnapshot empty() {
        return new DictionarySnapshot(null, null);
    }

    /** 금칙어가 한 건이라도 있는지 — 없으면 마스킹 자체를 건너뛴다. */
    boolean hasBannedWord() {
        return bannedTrie != null;
    }

    /** 주어진 문자열에서 금칙어 매치를 모두 찾는다. */
    Collection<Emit> findBanned(String text) {
        return parse(bannedTrie, text);
    }

    /** 주어진 문자열에서 허용표현 매치를 모두 찾는다. */
    Collection<Emit> findAllowed(String text) {
        return parse(allowedTrie, text);
    }

    /** 트라이를 구성한다 — 유효 항목이 없으면 null(=매칭 생략)을 돌려준다. */
    private static Trie buildTrie(Collection<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return null;
        }
        Set<String> distinct = new LinkedHashSet<>();
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank()) {
                distinct.add(keyword.trim());
            }
        }
        if (distinct.isEmpty()) {
            return null;
        }
        return Trie.builder().addKeywords(distinct).build();
    }

    /** 트라이가 없으면 빈 결과 — 라이브러리 호출 없이 단락한다. */
    private static Collection<Emit> parse(Trie trie, String text) {
        if (trie == null || text.isEmpty()) {
            return List.of();
        }
        return trie.parseText(text);
    }
}
