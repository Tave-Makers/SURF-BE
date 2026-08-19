package com.tavemakers.surf.global.common.moderation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;

/**
 * N1 정규화 — 특수문자만 제거하고 공백은 유지한다.
 *
 * 정규화된 좌표에서 찾은 매치를 원문 좌표로 되돌리기 위해 offset 역매핑을 함께 만든다.
 * 제거 판단은 code point 단위로 수행하므로 서로게이트 쌍의 한쪽만 사라지는 일이 없다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TextNormalizer {

    /** 정규화 결과 — 정규화 문자열과 `정규화 char 인덱스 → 원문 char 인덱스` 역매핑. */
    public record Normalized(String text, int[] rawIndex) {

        /** 정규화 구간 [ns, ne) 의 시작을 원문 char 인덱스로 환산한다. */
        public int rawStart(int ns) {
            return rawIndex[ns];
        }

        /** 정규화 구간 [ns, ne) 의 끝(배타)을 원문 char 인덱스로 환산한다. */
        public int rawEnd(int ne) {
            return rawIndex[ne - 1] + 1;
        }
    }

    /** 원문에서 특수문자를 제거하고(공백·문자·숫자는 유지) 원문 좌표 역매핑을 만든다. */
    public static Normalized normalize(String raw) {
        StringBuilder normalized = new StringBuilder(raw.length());
        int[] rawIndex = new int[raw.length()];
        int length = 0;

        int i = 0;
        while (i < raw.length()) {
            int codePoint = raw.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            if (isRetained(codePoint)) {
                // 서로게이트 쌍은 통째로 옮기고 char마다 원문 인덱스를 기록한다.
                for (int offset = 0; offset < charCount; offset++) {
                    normalized.append(raw.charAt(i + offset));
                    rawIndex[length++] = i + offset;
                }
            }
            i += charCount;
        }
        return new Normalized(normalized.toString(), Arrays.copyOf(rawIndex, length));
    }

    /** 유지 대상 문자인지 — 문자·숫자·공백만 남기고 나머지(기호·구두점·이모지)는 제거한다. */
    private static boolean isRetained(int codePoint) {
        return Character.isLetterOrDigit(codePoint) || Character.isWhitespace(codePoint);
    }
}
