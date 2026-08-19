package com.tavemakers.surf.global.common.loader;

import com.tavemakers.surf.application.moderation.service.DictionaryReloader;
import com.tavemakers.surf.application.moderation.usecase.ModerationTermUsecase;
import com.tavemakers.surf.domain.moderation.exception.ModerationDictionaryEmptyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 금칙어 사전 최초 시드 — 테이블이 비어 있을 때만 vendored 파일을 적재하고
 * 엔진의 초기 스냅숏을 구성한다.
 *
 * <p>시드 이후에도 금칙어가 0건이면 기동을 실패시킨다. 심사 대응 기능이 소리 없이 꺼진 채
 * 배포되는 것이 최악의 시나리오이며, 관리자가 사전을 전부 삭제한 사고도 이 검증이 잡는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModerationSeedLoader implements ApplicationListener<ApplicationReadyEvent> {

    private static final String BANNED_RESOURCE = "moderation/badwords.txt";
    private static final String ALLOWED_RESOURCE = "moderation/allowlist.txt";
    private static final String COMMENT_PREFIX = "#";

    private final ModerationTermUsecase moderationTermUsecase;
    private final DictionaryReloader dictionaryReloader;

    /** 기동 직후 사전 시드를 적재하고 엔진 스냅숏을 초기화한다 — 금칙어가 0건이면 기동을 실패시킨다. */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            int seeded = moderationTermUsecase.seedIfEmpty(
                    readTerms(BANNED_RESOURCE), readTerms(ALLOWED_RESOURCE));
            if (seeded > 0) {
                log.info("[MODERATION] 사전 초기 시드 완료 — {}건 적재", seeded);
            }
        } catch (DataIntegrityViolationException e) {
            // 다중 인스턴스가 동시에 기동하면 (type, text) unique 제약에서 한쪽만 살아남는다.
            // 다른 인스턴스가 이미 시드했다는 뜻이므로 아래 reload() 검증에 판단을 맡기고 진행한다.
            log.warn("[MODERATION] 사전 시드 충돌 — 다른 인스턴스가 동시에 시드한 것으로 보고 건너뜁니다.", e);
        }

        if (dictionaryReloader.reload() == 0) {
            throw new ModerationDictionaryEmptyException();
        }
    }

    /**
     * 시드 파일을 읽는다 — '#' 이후는 주석, 공백 줄은 무시하며 중복은 제거한다.
     * (type, text) unique 제약이 있으므로 파일 내 중복이 남으면 시드 자체가 실패한다.
     *
     * <p>오탐 회귀 코퍼스 테스트가 실사전을 같은 규칙으로 읽도록 static으로 공개한다.
     */
    public static Set<String> readTerms(String resourcePath) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            log.warn("[MODERATION] 시드 파일을 찾을 수 없습니다 — {}", resourcePath);
            return Set.of();
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> lines = reader.lines().toList();
            return new LinkedHashSet<>(lines.stream()
                    .map(ModerationSeedLoader::stripComment)
                    .filter(line -> !line.isEmpty())
                    .toList());
        } catch (IOException e) {
            log.error("[MODERATION] 시드 파일을 읽지 못했습니다 — {}", resourcePath, e);
            return Set.of();
        }
    }

    private static String stripComment(String line) {
        int commentIndex = line.indexOf(COMMENT_PREFIX);
        return (commentIndex >= 0 ? line.substring(0, commentIndex) : line).trim();
    }

}
