package com.tavemakers.surf.application.letter.usecase;

import com.tavemakers.surf.application.block.query.BlockGetService;
import com.tavemakers.surf.application.letter.query.LetterGetService;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.letter.event.LetterSentEvent;
import com.tavemakers.surf.domain.letter.exception.LetterBlockedException;
import com.tavemakers.surf.domain.letter.exception.LetterMailSendFailException;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.global.util.EmailSender;
import com.tavemakers.surf.presentation.letter.dto.request.LetterCreateReqDTO;
import com.tavemakers.surf.presentation.letter.dto.response.LetterResDTO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

/**
 * 쪽지 생성 R5 수정(저장 커밋 후 트랜잭션 밖 메일 발송) 검증.
 *
 * <p>수정 전에는 @Transactional 안에서 메일을 먼저 보내고 저장했기 때문에 저장 실패 시
 * "유령 메일"(메일만 가고 쪽지 없음)이 발생했다. 수정 후에는 쪽지를 먼저 저장(커밋)하고
 * 메일을 트랜잭션 밖에서 보내므로, 메일 실패 시 예외(500)는 그대로 전파되되 쪽지 레코드는
 * 남는다(orphan — 사용자가 감수하기로 결정한 트레이드오프).
 *
 * <p>또한 LetterSentEvent 발행이 usecase(비트랜잭션)에서 LetterCreateService.save 트랜잭션
 * 안으로 이동했는지 검증한다. notification 의 알림 리스너가
 * @TransactionalEventListener(AFTER_COMMIT)라서, 활성 트랜잭션 밖에서 발행된 이벤트는
 * 조용히 드롭되기 때문이다(AfterCommitProbe 로 실제 발화 확인).
 *
 * <p>usecase 가 더 이상 트랜잭션을 열지 않으므로 테스트 트랜잭션을 NOT_SUPPORTED 로 끄고
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

    /** AFTER_COMMIT 알림 리스너와 동일한 조건으로 이벤트 발화 여부를 기록하는 프로브 */
    @TestConfiguration
    static class AfterCommitProbe {
        final AtomicInteger fired = new AtomicInteger();

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void onLetterSent(LetterSentEvent event) {
            fired.incrementAndGet();
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
    private BlockGetService blockGetService;
    @MockBean
    private EmailSender emailSender;
    @MockBean
    private LogEventEmitter logEventEmitter;

    private Member sender;
    private Member receiver;

    @BeforeEach
    void setUp() {
        afterCommitProbe.fired.set(0);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            this.sender = persistMember("sender");
            this.receiver = persistMember("receiver");
        });
        given(memberGetService.getMember(sender.getId())).willReturn(sender);
        given(memberGetService.getMember(receiver.getId())).willReturn(receiver);
    }

    @Test
    @DisplayName("정상 경로: 쪽지가 저장되고 LetterSentEvent 가 커밋 후(AFTER_COMMIT) 발화한다")
    void 쪽지_저장_후_이벤트가_커밋_후_발화한다() {
        LetterResDTO result = letterUsecase.createLetter(sender.getId(), req());

        assertThat(result.letterId()).isNotNull();
        assertThat(countLetters()).as("쪽지가 저장되어야 한다").isEqualTo(1);
        assertThat(afterCommitProbe.fired.get())
                .as("LetterSentEvent 가 save 트랜잭션 안에서 발행되어 AFTER_COMMIT 리스너가 발화해야 한다")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("메일 발송 실패 시 LetterMailSendFailException 이 전파되지만 쪽지는 이미 커밋되어 남는다(orphan)")
    void 메일_실패해도_쪽지는_이미_저장되어_있다() {
        willThrow(new MailSendException("smtp down"))
                .given(emailSender).sendMail(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> letterUsecase.createLetter(sender.getId(), req()))
                .isInstanceOf(LetterMailSendFailException.class);

        assertThat(countLetters())
                .as("저장이 메일 발송보다 먼저 커밋되어 쪽지 레코드가 남아야 한다")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("차단 관계이면 쪽지·LetterSentEvent·이메일이 모두 생성되지 않는다")
    void 차단시_저장_이벤트_이메일이_모두_없다() {
        given(blockGetService.existsBetween(sender.getId(), receiver.getId())).willReturn(true);

        assertThatThrownBy(() -> letterUsecase.createLetter(sender.getId(), req()))
                .isInstanceOf(LetterBlockedException.class);

        assertThat(countLetters()).as("차단된 쪽지는 DB에 저장되지 않아야 한다").isZero();
        assertThat(afterCommitProbe.fired.get()).as("LetterSentEvent가 발행되지 않아야 한다").isZero();
        then(emailSender).should(never()).sendMail(anyString(), anyString(), anyString());
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
