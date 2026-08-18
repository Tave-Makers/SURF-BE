package com.tavemakers.surf.application.moderation.usecase;

import com.tavemakers.surf.domain.moderation.entity.ModerationTerm;
import com.tavemakers.surf.domain.moderation.entity.ModerationTermType;
import com.tavemakers.surf.domain.moderation.event.ModerationDictionaryChangedEvent;
import com.tavemakers.surf.domain.moderation.exception.ModerationTermDuplicateException;
import com.tavemakers.surf.domain.moderation.repository.ModerationTermRepository;
import com.tavemakers.surf.global.logging.RequestLogContext;
import com.tavemakers.surf.presentation.moderation.dto.request.ModerationTermCreateReqDTO;
import com.tavemakers.surf.presentation.moderation.dto.response.ModerationTermResDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * ModerationTermUsecase 단위 테스트 — 시드 멱등성과 중복 등록 차단,
 * 편집 시 사전 변경 이벤트 발행을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ModerationTermUsecaseTest {

    @Mock
    private ModerationTermRepository moderationTermRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ModerationTermUsecase moderationTermUsecase;

    @Captor
    private ArgumentCaptor<List<ModerationTerm>> savedTermsCaptor;

    @Captor
    private ArgumentCaptor<ModerationDictionaryChangedEvent> eventCaptor;

    @Test
    @DisplayName("사전이 비어 있을 때만 시드를 적재한다 — 이미 항목이 있으면 아무것도 저장하지 않는다")
    void 사전에_항목이_있으면_시드를_적재하지_않는다() {
        given(moderationTermRepository.count()).willReturn(588L);

        int seeded = moderationTermUsecase.seedIfEmpty(List.of("씨발"), List.of("시발점"));

        assertThat(seeded).isZero();
        then(moderationTermRepository).should(never()).saveAll(anyList());
    }

    @Test
    @DisplayName("사전이 비어 있으면 금칙어·허용 표현을 종류별로 적재한다")
    void 사전이_비어있으면_시드를_적재한다() {
        given(moderationTermRepository.count()).willReturn(0L);
        given(moderationTermRepository.saveAll(anyList()))
                .willAnswer(invocation -> invocation.<List<ModerationTerm>>getArgument(0));

        int seeded = moderationTermUsecase.seedIfEmpty(List.of("씨발", "병신"), List.of("시발점"));

        assertThat(seeded).isEqualTo(3);

        then(moderationTermRepository).should().saveAll(savedTermsCaptor.capture());
        assertThat(savedTermsCaptor.getValue())
                .extracting(ModerationTerm::getType, ModerationTerm::getText)
                .containsExactly(
                        tuple(ModerationTermType.BANNED, "씨발"),
                        tuple(ModerationTermType.BANNED, "병신"),
                        tuple(ModerationTermType.ALLOWED, "시발점"));
    }

    @Test
    @DisplayName("같은 (종류, 표현)이 이미 있으면 중복 예외를 던지고 저장·이벤트 발행을 하지 않는다")
    void 중복_등록시_예외를_던진다() {
        given(moderationTermRepository.existsByTypeAndText(ModerationTermType.BANNED, "씨발"))
                .willReturn(true);

        assertThatThrownBy(() -> moderationTermUsecase.createTerm(
                new ModerationTermCreateReqDTO(ModerationTermType.BANNED, "씨발")))
                .isInstanceOf(ModerationTermDuplicateException.class);

        then(moderationTermRepository).should(never()).save(any(ModerationTerm.class));
        then(eventPublisher).should(never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("등록에 성공하면 커밋 후 스냅숏 갱신을 위해 사전 변경 이벤트를 발행한다")
    void 등록시_사전_변경_이벤트를_발행한다() {
        given(moderationTermRepository.existsByTypeAndText(ModerationTermType.BANNED, "씨발"))
                .willReturn(false);
        given(moderationTermRepository.save(any(ModerationTerm.class))).willAnswer(invocation -> {
            ModerationTerm term = invocation.getArgument(0);
            ReflectionTestUtils.setField(term, "id", 10L);
            return term;
        });

        // 앞뒤 공백은 제거된 표현으로 저장된다 (공백이 남으면 트라이 매칭이 어긋난다)
        ModerationTermResDTO response = moderationTermUsecase.createTerm(
                new ModerationTermCreateReqDTO(ModerationTermType.BANNED, "  씨발  "));

        assertThat(response.termId()).isEqualTo(10L);
        assertThat(response.text()).isEqualTo("씨발");

        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo(ModerationTermType.BANNED);
        assertThat(eventCaptor.getValue().text()).isEqualTo("씨발");
    }

    @Test
    @DisplayName("삭제 감사 로그에 무엇이 지워졌는지(type·text)를 남긴다 — id만으로는 추적할 수 없다")
    void 삭제시_감사_로그_props를_남긴다() {
        RequestLogContext.clear();
        ModerationTerm term = ModerationTerm.of(ModerationTermType.BANNED, "씨발");
        ReflectionTestUtils.setField(term, "id", 10L);
        given(moderationTermRepository.findById(10L)).willReturn(Optional.of(term));

        moderationTermUsecase.deleteTerm(10L);

        assertThat(RequestLogContext.get().pendingProps)
                .containsEntry("type", "BANNED")
                .containsEntry("text", "씨발");
        RequestLogContext.clear();
    }

    @Test
    @DisplayName("등록 감사 로그 props에 type·text를 담는다 (관리자 id는 flush 시 공통 필드)")
    void 등록_요청_DTO가_감사_로그_props를_만든다() {
        ModerationTermCreateReqDTO req =
                new ModerationTermCreateReqDTO(ModerationTermType.ALLOWED, "  성폭행 예방  ");

        assertThat(req.buildProps())
                .containsEntry("type", "ALLOWED")
                .containsEntry("text", "성폭행 예방");
    }

}
