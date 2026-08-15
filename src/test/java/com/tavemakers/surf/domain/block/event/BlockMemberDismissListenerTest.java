package com.tavemakers.surf.domain.block.event;

import com.tavemakers.surf.domain.block.service.BlockDeleteService;
import com.tavemakers.surf.domain.member.event.MemberDismissedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

/**
 * 제명 시 차단 관계 정리 리스너.
 *
 * <p>운영 FK가 RESTRICT라 이 정리가 빠지면 회원 hard delete 자체가 실패한다.
 * 부수효과가 아니라 제명의 선행 조건이므로 회귀를 막는다.
 */
@ExtendWith(MockitoExtension.class)
class BlockMemberDismissListenerTest {

    @Mock
    private BlockDeleteService blockDeleteService;

    @InjectMocks
    private BlockMemberDismissListener listener;

    @Test
    @DisplayName("제명 이벤트를 받으면 해당 회원의 양방향 차단 관계를 정리한다")
    void 제명시_양방향_관계를_정리한다() {
        listener.onMemberDismissed(new MemberDismissedEvent(42L));

        then(blockDeleteService).should().deleteAllRelatedTo(42L);
    }
}
