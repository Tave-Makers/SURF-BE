package com.tavemakers.surf.domain.letter.facade;

import com.tavemakers.surf.domain.letter.dto.request.LetterCreateReqDTO;
import com.tavemakers.surf.domain.letter.dto.response.LetterResDTO;
import com.tavemakers.surf.domain.letter.entity.Letter;
import com.tavemakers.surf.domain.letter.event.LetterSentEvent;
import com.tavemakers.surf.domain.letter.exception.LetterMailSendFailException;
import com.tavemakers.surf.domain.letter.service.LetterGetService;
import com.tavemakers.surf.domain.letter.service.LetterCreateService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.service.MemberGetService;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.global.util.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LetterFacade {

    private final MemberGetService memberGetService;
    private final LetterCreateService letterCreateService;
    private final EmailSender emailSender;
    private final LetterGetService letterGetService;
    private final ApplicationEventPublisher eventPublisher;
    private final LogEventEmitter logEventEmitter;

    /** 쪽지 생성 및 이메일 발송 */
    @Transactional
    public LetterResDTO createLetter(Long senderId, LetterCreateReqDTO req) {
        logEventEmitter.emit("letter_send_api_called", Map.of(
                "sender_id", senderId,
                "receiver_id", req.receiverId()
        ));

        // 1) 발신자 조회
        Member sender = memberGetService.getMember(senderId);

        // 2) 수신자 조회
        Member receiver = memberGetService.getMember(req.receiverId());

        // validation 로그 (@Valid 통과 이후 실행되므로 result=true)
        String replyEmail = req.replyEmail();
        String emailDomain = replyEmail.contains("@")
                ? replyEmail.substring(replyEmail.indexOf('@') + 1) : "";
        logEventEmitter.emit("letter_email_validation_checked", Map.of(
                "requester_id", senderId,
                "email_domain", emailDomain,
                "validation_result", true
        ));
        logEventEmitter.emit("letter_title_validation_checked", Map.of(
                "requester_id", senderId,
                "title_length", req.title().length(),
                "validation_result", true
        ));
        logEventEmitter.emit("letter_body_validation_checked", Map.of(
                "requester_id", senderId,
                "content_length", req.content().length(),
                "validation_result", true
        ));

        // 3) 이메일 본문 생성
        String emailBody = """
        [Surf에서 %s님이 보낸 쪽지입니다.]

        %s

        회신 희망 이메일: %s
        SNS: %s
        """
                .formatted(
                        sender.getName(),
                        req.content(),
                        req.replyEmail(),
                        req.sns() != null ? req.sns() : "-"
                );

        // 4) 이메일 전송 (실패 시 예외)
        boolean emailSent = false;
        try {
            emailSender.sendMail(receiver.getEmail(), req.title(), emailBody);
            emailSent = true;
        } catch (MailException e) {
            Map<String, Object> failedProps = new HashMap<>();
            failedProps.put("sender_id", senderId);
            failedProps.put("receiver_id", req.receiverId());
            failedProps.put("status_code", 500);
            failedProps.put("error_code", "MAIL_SEND_FAIL");
            failedProps.put("error_message", e.getMessage() != null ? e.getMessage() : "smtp error");
            logEventEmitter.emitError("letter_send_api_failed", failedProps, "쪽지 전송 실패 - 이메일 발송 오류");
            throw new LetterMailSendFailException();
        }

        // 5) 엔티티 생성
        Letter letter = Letter.create(
                req.title(),
                req.content(),
                req.sns(),
                req.replyEmail(),
                sender,
                receiver
        );

        // 6) 저장
        Letter saved = letterCreateService.save(letter);

        // 7) 알림 이벤트 발행
        eventPublisher.publishEvent(new LetterSentEvent(
                receiver.getId(),
                sender.getName(),
                sender.getId()
        ));

        logEventEmitter.emit("letter_send_api_succeeded", Map.of(
                "sender_id", senderId,
                "receiver_id", req.receiverId(),
                "letter_id", saved.getLetterId(),
                "email_sent", emailSent
        ));

        // 8) 저장된 엔티티 기반으로 Response 생성
        return LetterResDTO.from(saved);
    }

    /** 발신한 쪽지 목록 조회 */
    @Transactional
    public Slice<LetterResDTO> getSentLetters(Long senderId, Pageable pageable) {
        return letterGetService.getSentLetters(senderId, pageable)
                .map(LetterResDTO::from);
    }
}
