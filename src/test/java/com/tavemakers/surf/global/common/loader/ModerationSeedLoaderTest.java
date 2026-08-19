package com.tavemakers.surf.global.common.loader;

import com.tavemakers.surf.application.moderation.service.DictionaryReloader;
import com.tavemakers.surf.application.moderation.usecase.ModerationTermUsecase;
import com.tavemakers.surf.domain.moderation.exception.ModerationDictionaryEmptyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * ModerationSeedLoader 단위 테스트 — 시드 멱등성과 사전이 빈 채로 기동하지 않는지 검증한다.
 * 멱등 판정 자체는 usecase의 seedIfEmpty가 트랜잭션 안에서 수행하므로,
 * 여기서는 로더가 파일을 올바르게 파싱해 넘기고 리빌드 결과로 기동 여부를 가르는지를 본다.
 */
@ExtendWith(MockitoExtension.class)
class ModerationSeedLoaderTest {

    @Mock
    private ModerationTermUsecase moderationTermUsecase;

    @Mock
    private DictionaryReloader dictionaryReloader;

    @InjectMocks
    private ModerationSeedLoader moderationSeedLoader;

    @Captor
    private ArgumentCaptor<Collection<String>> bannedCaptor;

    @Captor
    private ArgumentCaptor<Collection<String>> allowedCaptor;

    @Test
    @DisplayName("시드 파일을 읽어 주석·공백 줄을 걸러낸 항목만 적재 요청하고, 스냅숏을 리빌드한다")
    void 기동시_시드파일을_파싱해_적재하고_스냅숏을_리빌드한다() {
        given(moderationTermUsecase.seedIfEmpty(any(), any())).willReturn(613);
        given(dictionaryReloader.reload()).willReturn(588);

        moderationSeedLoader.onApplicationEvent(null);

        then(moderationTermUsecase).should().seedIfEmpty(bannedCaptor.capture(), allowedCaptor.capture());

        Collection<String> banned = bannedCaptor.getValue();
        Collection<String> allowed = allowedCaptor.getValue();

        // '#'로 시작하는 주석 줄과 빈 줄은 제외된다
        assertThat(banned).isNotEmpty().doesNotContain("").noneMatch(word -> word.startsWith("#"));
        assertThat(allowed).isNotEmpty().doesNotContain("").noneMatch(word -> word.startsWith("#"));

        // 인라인 주석('정액제  # 요금제 표현')도 표현만 남는다
        assertThat(allowed).contains("시발점", "정액제", "성폭행 예방")
                .noneMatch(phrase -> phrase.contains("#"));

        then(dictionaryReloader).should().reload();
    }

    @Test
    @DisplayName("이미 적재된 사전이 있으면(seedIfEmpty가 0건) 중복 적재 없이 기동이 계속된다")
    void 재기동시_이미_적재된_사전이_있으면_중복_적재하지_않는다() {
        given(moderationTermUsecase.seedIfEmpty(any(), any())).willReturn(0);
        given(dictionaryReloader.reload()).willReturn(588);

        assertThatCode(() -> moderationSeedLoader.onApplicationEvent(null))
                .doesNotThrowAnyException();

        then(dictionaryReloader).should().reload();
    }

    @Test
    @DisplayName("시드 이후에도 금칙어가 0건이면 예외를 던져 기동을 실패시킨다")
    void 사전이_비어있으면_기동에_실패한다() {
        given(moderationTermUsecase.seedIfEmpty(any(), any())).willReturn(0);
        given(dictionaryReloader.reload()).willReturn(0);

        assertThatThrownBy(() -> moderationSeedLoader.onApplicationEvent(null))
                .isInstanceOf(ModerationDictionaryEmptyException.class);
    }

}
