package com.tavemakers.surf.application.activity.usecase;

import com.tavemakers.surf.application.activity.query.ActiveGenerationGetService;
import com.tavemakers.surf.domain.activity.event.ActiveGenerationChangedEvent;
import com.tavemakers.surf.domain.activity.service.activeGeneration.ActiveGenerationPutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ActiveGenerationUsecaseTest {

    @Mock
    private ActiveGenerationGetService activeGenerationGetService;

    @Mock
    private ActiveGenerationPutService activeGenerationPutService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ActiveGenerationUsecase activeGenerationUsecase;

    @Test
    @DisplayName("활동 기수 변경 시 기수를 저장하고 변경 이벤트를 발행한다 (동기화·점수 초기화는 리스너 체인)")
    void updateActiveGeneration_publishesChangedEvent() {
        activeGenerationUsecase.updateActiveGeneration(16);

        then(activeGenerationPutService).should().updateActiveGeneration(16);
        then(eventPublisher).should().publishEvent(new ActiveGenerationChangedEvent(16));
    }
}
