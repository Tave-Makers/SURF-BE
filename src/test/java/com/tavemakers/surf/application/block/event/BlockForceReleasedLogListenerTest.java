package com.tavemakers.surf.application.block.event;

import com.tavemakers.surf.domain.block.event.BlockForceReleasedEvent;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * 관리자 강제 해제 감사 로그가 <b>커밋된 뒤에만</b> 적재되는지 검증한다.
 *
 * <p>LogEventEmitter는 즉시 기록하지 않고 요청 컨텍스트에 적재해 두었다가 요청 종료 시 flush한다.
 * 트랜잭션 안에서 emit하면 커밋이 실패해도 "강제 해제" 성공 이벤트가 남아 감사 로그에 실패 시도가
 * 성공으로 섞인다. AFTER_COMMIT 리스너로 옮긴 이유가 그것이며, 이 테스트가 그 회귀를 막는다.
 *
 * <p>실제 커밋/롤백을 겪어야 하므로 클래스 트랜잭션을 NOT_SUPPORTED로 비활성화하고
 * TransactionTemplate으로 경계를 직접 만든다.
 */
@DataJpaTest
@Import(BlockForceReleasedLogListener.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BlockForceReleasedLogListenerTest {

    private static final BlockForceReleasedEvent EVENT =
            new BlockForceReleasedEvent(9L, 101L, 1L, 2L);

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private LogEventEmitter logEventEmitter;

    @Test
    @DisplayName("커밋되면 admin_id·block_id·blocker_id·blocked_id를 감사 로그로 적재한다")
    void 커밋되면_감사_로그를_적재한다() {
        newTransaction().executeWithoutResult(status -> eventPublisher.publishEvent(EVENT));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> props = ArgumentCaptor.forClass(Map.class);
        then(logEventEmitter).should().emit(eq("block_released_by_admin"), props.capture(), anyString());
        assertThat(props.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "admin_id", 9L,
                "block_id", 101L,
                "blocker_id", 1L,
                "blocked_id", 2L
        ));
    }

    @Test
    @DisplayName("롤백되면 감사 로그를 남기지 않는다 — 실제로 해제되지 않았으므로")
    void 롤백되면_감사_로그가_없다() {
        newTransaction().executeWithoutResult(status -> {
            eventPublisher.publishEvent(EVENT);
            status.setRollbackOnly();
        });

        then(logEventEmitter).should(never()).emit(anyString(), org.mockito.ArgumentMatchers.anyMap(), anyString());
    }

    private TransactionTemplate newTransaction() {
        return new TransactionTemplate(transactionManager);
    }
}
