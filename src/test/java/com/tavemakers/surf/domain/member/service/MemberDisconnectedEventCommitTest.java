package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.SocialAccount;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.domain.member.event.MemberDisconnectedEvent;
import com.tavemakers.surf.domain.member.event.MemberDisconnectedEvent.SocialAccountSnapshot;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 연결 해제 이벤트의 AFTER_COMMIT 의미론 검증 —
 * 탈퇴 트랜잭션이 롤백되면 리스너가 실행되지 않고(외부 unlink/revoke 미수행),
 * 커밋되면 clear() 이전에 캡처된 스냅샷 그대로 전달되어야 한다.
 */
@DataJpaTest
@Import({MemberWithdrawService.class, MemberDisconnectedEventCommitTest.RecordingListenerConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MemberDisconnectedEventCommitTest {

    /** AFTER_COMMIT 시점에 수신한 이벤트를 기록하는 테스트 전용 리스너. */
    @TestConfiguration
    static class RecordingListenerConfig {
        final List<MemberDisconnectedEvent> received = new CopyOnWriteArrayList<>();

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void on(MemberDisconnectedEvent event) {
            received.add(event);
        }
    }

    @Autowired
    private MemberWithdrawService memberWithdrawService;

    @Autowired
    private RecordingListenerConfig recordingListener;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private MemberGetService memberGetService;

    @BeforeEach
    void setUp() {
        recordingListener.received.clear();
    }

    private Long persistMemberWithBothProviders() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            Member member = Member.builder()
                    .name("회원")
                    .email("commit" + System.nanoTime() + "@test.com")
                    .status(MemberStatus.APPROVED)
                    .role(MemberRole.MEMBER)
                    .memberType(MemberType.YB)
                    .activityStatus(true)
                    .build();
            member.addSocialAccount(SocialAccount.builder()
                    .provider(Provider.KAKAO)
                    .providerId(String.valueOf(System.nanoTime()))
                    .kakaoId(777L)
                    .build());
            member.addSocialAccount(SocialAccount.builder()
                    .provider(Provider.APPLE)
                    .providerId("apple-" + System.nanoTime())
                    .appleRefreshToken("apple-rt")
                    .build());
            entityManager.persist(member);
            return member.getId();
        });
    }

    @Test
    @DisplayName("탈퇴 트랜잭션 커밋 시 clear() 전에 캡처한 Kakao+Apple 스냅샷이 리스너에 전달된다")
    void commit_deliversSnapshotsCapturedBeforeClear() {
        Long memberId = persistMemberWithBothProviders();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Member member = entityManager.find(Member.class, memberId);
            memberWithdrawService.expel(member);
            assertThat(member.getSocialAccounts()).as("커밋 전 이미 clear 됨").isEmpty();
        });

        assertThat(recordingListener.received).hasSize(1);
        MemberDisconnectedEvent event = recordingListener.received.get(0);
        assertThat(event.memberId()).isEqualTo(memberId);
        assertThat(event.socialAccounts()).containsExactlyInAnyOrder(
                new SocialAccountSnapshot(Provider.KAKAO, 777L, null),
                new SocialAccountSnapshot(Provider.APPLE, null, "apple-rt"));
    }

    @Test
    @DisplayName("탈퇴 트랜잭션이 롤백되면 리스너는 실행되지 않는다 (외부 unlink/revoke 미수행)")
    void rollback_doesNotInvokeListener() {
        Long memberId = persistMemberWithBothProviders();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Member member = entityManager.find(Member.class, memberId);
            memberWithdrawService.expel(member);
            status.setRollbackOnly();
        });

        assertThat(recordingListener.received).isEmpty();

        // 롤백으로 회원·소셜 계정 상태도 원복된다.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Member member = entityManager.find(Member.class, memberId);
            assertThat(member.getStatus()).isEqualTo(MemberStatus.APPROVED);
            assertThat(member.getSocialAccounts()).hasSize(2);
        });
    }
}
