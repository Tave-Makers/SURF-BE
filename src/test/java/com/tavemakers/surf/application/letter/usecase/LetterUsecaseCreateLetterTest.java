package com.tavemakers.surf.application.letter.usecase;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.presentation.letter.dto.request.LetterCreateReqDTO;
import com.tavemakers.surf.presentation.letter.dto.response.LetterResDTO;
import com.tavemakers.surf.application.letter.event.LetterEmailListener;
import com.tavemakers.surf.domain.letter.event.LetterEmailRequestedEvent;
import com.tavemakers.surf.domain.letter.event.LetterSentEvent;
import com.tavemakers.surf.application.letter.query.LetterGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.global.util.EmailSender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSendException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.verifyNoInteractions;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.willThrow;

/**
 * 쪽지 생성 검증 — 이메일 발송이 요청 흐름에서 분리(AFTER_COMMIT 비동기 이벤트)됐는지 확인한다.
 *
 * <p>성능 측정에서 동기 SMTP 발송이 응답을 ~3초 지연시키는 것이 확인되어,
 * 발송을 LetterEmailRequestedEvent + LetterEmailListener 로 옮겼다.
 * 요청 스레드는 저장 커밋까지만 책임지고, 이메일은 커밋 후 비동기로 발송된다.
 * 발송 실패는 응답에 영향을 주지 않는다(쪽지는 이미 커밋됨, 실패는 서버 로그).
 *
 * <p>usecase 가 트랜잭션을 열지 않으므로 테스트 트랜잭션을 NOT_SUPPORTED 로 끄고
 * 픽스처는 TransactionTemplate 으로 명시적 커밋한다 (MemberDismissRollbackTest 패턴).
 */
@DataJpaTest
@Import({
        LetterUsecase.class,
        LetterCreateService.class,
        LetterGetService.class,
        LetterUsecaseCreateLetterTest.AfterCommitProbe.class,
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LetterUsecaseCreateLetterTest {

    /** AFTER_COMMIT 리스너와 동일한 조건으로 두 이벤트의 발화 여부를 기록하는 프로브 */
    @TestConfiguration
    static class AfterCommitProbe {
        final AtomicInteger sentFired = new AtomicInteger();
        final AtomicReference<LetterEmailRequestedEvent> emailEvent = new AtomicReference<>();

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void onLetterSent(LetterSentEvent event) {
            sentFired.incrementAndGet();
        }

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void onEmailRequested(LetterEmailRequestedEvent event) {
            emailEvent.set(event);
        }
    }

    @Autowired
    private LetterUsecase letterUsecase;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private AfterCommitProbe afterCommitProbe;

    @MockBean
    private MemberGetService memberGetService;
    @MockBean
    private EmailSender emailSender;
    @MockBean
    private LogEventEmitter logEventEmitter;

    private Member sender;
    private Member receiver;

    @BeforeEach
    void setUp() {
        afterCommitProbe.sentFired.set(0);
        afterCommitProbe.emailEvent.set(null);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            this.sender = persistMember("sender");
            this.receiver = persistMember("receiver");
        });
        given(memberGetService.getMember(sender.getId())).willReturn(sender);
        given(memberGetService.getMember(receiver.getId())).willReturn(receiver);
    }

    @Test
    @DisplayName("쪽지가 저장되고 알림·이메일 이벤트가 커밋 후(AFTER_COMMIT) 발화한다")
    void 쪽지_저장_후_두_이벤트가_커밋_후_발화한다() {
        LetterResDTO result = letterUsecase.createLetter(sender.getId(), req());

        assertThat(result.letterId()).isNotNull();
        assertThat(countLetters()).as("쪽지가 저장되어야 한다").isEqualTo(1);
        assertThat(afterCommitProbe.sentFired.get())
                .as("LetterSentEvent(알림) 가 AFTER_COMMIT 으로 발화해야 한다")
                .isEqualTo(1);
        LetterEmailRequestedEvent email = afterCommitProbe.emailEvent.get();
        assertThat(email).as("LetterEmailRequestedEvent(이메일) 가 AFTER_COMMIT 으로 발화해야 한다").isNotNull();
        assertThat(email.receiverEmail()).isEqualTo(receiver.getEmail());
        assertThat(email.senderName()).isEqualTo(sender.getName());
    }

    @Test
    @DisplayName("요청 흐름은 SMTP 를 호출하지 않는다 — 발송은 리스너 책임 (응답 지연 제거)")
    void 요청_흐름에서_메일을_보내지_않는다() {
        letterUsecase.createLetter(sender.getId(), req());

        verifyNoInteractions(emailSender);
    }

    @Test
    @DisplayName("리스너의 메일 발송이 실패해도 예외를 전파하지 않는다 (쪽지는 이미 커밋됨)")
    void 리스너_메일_실패는_예외를_전파하지_않는다() {
        EmailSender failing = mock(EmailSender.class);
        willThrow(new MailSendException("smtp down"))
                .given(failing).sendMail(anyString(), anyString(), anyString());
        LetterEmailListener listener = new LetterEmailListener(failing);

        assertThatCode(() -> listener.handle(new LetterEmailRequestedEvent(
                1L, sender.getId(), receiver.getId(), "보낸이",
                receiver.getEmail(), "제목", "내용", "reply@test.com", null)))
                .doesNotThrowAnyException();
    }

    private LetterCreateReqDTO req() {
        return new LetterCreateReqDTO(
                receiver.getId(), "제목", "내용", null, "reply@test.com");
    }

    private long countLetters() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> entityManager
                .createQuery("select count(l) from Letter l where l.sender.id = :id", Long.class)
                .setParameter("id", sender.getId())
                .getSingleResult());
    }

    private Member persistMember(String prefix) {
        Member member = Member.builder()
                .provider(Provider.KAKAO)
                .providerId(String.valueOf(System.nanoTime()))
                .kakaoId(System.nanoTime())
                .name("회원")
                .email(prefix + System.nanoTime() + "@test.com")
                .phoneNumber(String.valueOf(System.nanoTime()))
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        entityManager.persist(member);
        return member;
    }
}
