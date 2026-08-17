package com.tavemakers.surf.global.common.moderation;

import java.util.List;

/**
 * 마스킹 결과 — 마스킹된 문자열과 로깅용 매치 정보.
 *
 * `matched`에는 매칭된 금칙어만 담는다. 본문 전체는 남기지 않는다.
 */
public record MaskingResult(String masked, int matchCount, List<String> matched) {
}
