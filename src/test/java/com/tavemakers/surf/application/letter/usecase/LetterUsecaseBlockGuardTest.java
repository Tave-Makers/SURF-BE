package com.tavemakers.surf.application.letter.usecase;

import com.tavemakers.surf.application.block.query.BlockGetService;
import com.tavemakers.surf.application.letter.query.LetterGetService;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.letter.exception.LetterBlockedException;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.global.util.EmailSender;
import com.tavemakers.surf.presentation.letter.dto.request.LetterCreateReqDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

/** 쪽지 발송 전 양방향 차단 가드 검증 */
@ExtendWith(MockitoExtension.class)
class LetterUsecaseBlockGuardTest {

    private static final Long MEMBER_A = 1L;
    private static final Long MEMBER_B = 2L;

    @Mock
    private MemberGetService memberGetService;
    @Mock
    private BlockGetService blockGetService;
    @Mock
    private LetterCreateService letterCreateService;
    @Mock
    private EmailSender emailSender;
    @Mock
    private LetterGetService letterGetService;
    @Mock
    private LogEventEmitter logEventEmitter;

    @InjectMocks
    private LetterUsecase letterUsecase;

    @Test
    @DisplayName("A가 B를 차단하면 A에서 B로 쪽지를 보낼 수 없고 부수효과가 없다")
    void 차단한_상대에게는_쪽지를_보낼_수_없다() {
        givenMembers();
        given(blockGetService.existsBetween(MEMBER_A, MEMBER_B)).willReturn(true);

        assertBlocked(MEMBER_A, MEMBER_B);

        assertLookupAndGuardOrder(MEMBER_A, MEMBER_B);
    }

    @Test
    @DisplayName("A가 B를 차단하면 B에서 A로 보내는 쪽지도 거부되어 차단 방향이 노출되지 않는다")
    void 나를_차단한_상대에게도_쪽지를_보낼_수_없다() {
        givenMembers();
        given(blockGetService.existsBetween(MEMBER_B, MEMBER_A)).willReturn(true);

        assertBlocked(MEMBER_B, MEMBER_A);

        assertLookupAndGuardOrder(MEMBER_B, MEMBER_A);
    }

    private void assertBlocked(Long senderId, Long receiverId) {
        assertThatThrownBy(() -> letterUsecase.createLetter(senderId, request(receiverId)))
                .isInstanceOf(LetterBlockedException.class)
                .satisfies(exception -> {
                    LetterBlockedException blocked = (LetterBlockedException) exception;
                    assertThat(blocked.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(blocked.getMessage()).isEqualTo("쪽지를 보낼 수 없는 상대입니다.");
                });

        then(letterCreateService).should(never()).save(any());
        then(emailSender).should(never()).sendMail(anyString(), anyString(), anyString());

        assertBlockedFailureLogged(senderId, receiverId);
    }

    /**
     * 차단 거부도 메일 실패와 동일한 실패 이벤트를 남긴다.
     * 남기지 않으면 진입 시 emit 한 letter_send_api_called 만 있고 종결 이벤트가 없어,
     * 분석 퍼널에서 원인 불명 이탈이 되고 차단 발동 횟수를 측정할 수 없다.
     */
    @SuppressWarnings("unchecked")
    private void assertBlockedFailureLogged(Long senderId, Long receiverId) {
        ArgumentCaptor<Map<String, Object>> props = ArgumentCaptor.forClass(Map.class);
        then(logEventEmitter).should()
                .emitError(eq("letter_send_api_failed"), props.capture(), anyString());

        assertThat(props.getValue())
                .containsEntry("sender_id", senderId)
                .containsEntry("receiver_id", receiverId)
                .containsEntry("status_code", 403)
                .containsEntry("error_code", "LETTER_BLOCKED");
    }

    /** 발신자·수신자를 검증한 다음 차단 관계를 검사하는 순서를 고정한다 */
    private void assertLookupAndGuardOrder(Long senderId, Long receiverId) {
        InOrder order = inOrder(memberGetService, blockGetService);
        order.verify(memberGetService).getMember(senderId);
        order.verify(memberGetService).getMember(receiverId);
        order.verify(blockGetService).existsBetween(senderId, receiverId);
    }

    private void givenMembers() {
        given(memberGetService.getMember(MEMBER_A)).willReturn(member(MEMBER_A));
        given(memberGetService.getMember(MEMBER_B)).willReturn(member(MEMBER_B));
    }

    private LetterCreateReqDTO request(Long receiverId) {
        return new LetterCreateReqDTO(receiverId, "제목", "내용", null, "reply@test.com");
    }

    private Member member(Long memberId) {
        Member member = Member.builder()
                .name("회원" + memberId)
                .email("member" + memberId + "@test.com")
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }
}
