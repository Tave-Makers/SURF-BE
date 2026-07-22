package com.tavemakers.surf.application.member.usecase;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.entity.PendingSocialIntegration;
import com.tavemakers.surf.domain.member.repository.PendingSocialIntegrationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class PendingIntegrationUsecaseTest {

    @Mock
    private PendingSocialIntegrationRepository pendingSocialIntegrationRepository;

    @InjectMocks
    private PendingIntegrationUsecase pendingIntegrationUsecase;

    @Test
    @DisplayName("issue()는 기존 pending 삭제를 먼저 flush한 뒤 새 row를 저장한다 — INSERT가 DELETE보다 먼저 flush되는 것 방지")
    void issue_savesAndReturnsToken() {
        String token = pendingIntegrationUsecase.issue(1L, 2L, Provider.KAKAO, "user@test.com", "01011112222");

        assertThat(token).isNotBlank();

        // 순서 검증: 삭제 → flush → 신규 저장 (flush 누락 시 재발급이 UNIQUE 위반 → 구 토큰 반환으로 퇴행)
        InOrder inOrder = inOrder(pendingSocialIntegrationRepository);
        inOrder.verify(pendingSocialIntegrationRepository).deleteBySocialAccountId(2L);
        inOrder.verify(pendingSocialIntegrationRepository).flush();

        ArgumentCaptor<PendingSocialIntegration> captor = ArgumentCaptor.forClass(PendingSocialIntegration.class);
        inOrder.verify(pendingSocialIntegrationRepository).saveAndFlush(captor.capture());
        PendingSocialIntegration saved = captor.getValue();
        assertThat(saved.getToken()).isEqualTo(token);
        assertThat(saved.getTempMemberId()).isEqualTo(1L);
        assertThat(saved.getSocialAccountId()).isEqualTo(2L);
        assertThat(saved.getProvider()).isEqualTo(Provider.KAKAO);
    }

    @Test
    @DisplayName("findIssuedToken()은 해당 SocialAccount의 pending 토큰을 반환하고, 없으면 empty를 반환한다")
    void findIssuedToken_returnsWinnerTokenOrEmpty() {
        PendingSocialIntegration winner = PendingSocialIntegration.issue(
                1L, 2L, Provider.KAKAO, "user@test.com", "01011112222", LocalDateTime.now());
        given(pendingSocialIntegrationRepository.findBySocialAccountId(2L)).willReturn(Optional.of(winner));
        given(pendingSocialIntegrationRepository.findBySocialAccountId(99L)).willReturn(Optional.empty());

        assertThat(pendingIntegrationUsecase.findIssuedToken(2L)).contains(winner.getToken());
        assertThat(pendingIntegrationUsecase.findIssuedToken(99L)).isEmpty();
    }
}
