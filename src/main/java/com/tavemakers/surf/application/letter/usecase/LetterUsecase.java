package com.tavemakers.surf.application.letter.usecase;

import com.tavemakers.surf.application.block.query.BlockGetService;
import com.tavemakers.surf.application.letter.query.LetterGetService;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.letter.entity.Letter;
import com.tavemakers.surf.domain.letter.exception.LetterBlockedException;
import com.tavemakers.surf.domain.letter.exception.LetterMailSendFailException;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.global.util.EmailSender;
import com.tavemakers.surf.presentation.letter.dto.request.LetterCreateReqDTO;
import com.tavemakers.surf.presentation.letter.dto.response.LetterResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LetterUsecase {

    private final MemberGetService memberGetService;
    private final BlockGetService blockGetService;
    private final LetterCreateService letterCreateService;
    private final EmailSender emailSender;
    private final LetterGetService letterGetService;
    private final LogEventEmitter logEventEmitter;

    /** 쪽지 생성(저장·커밋) 후 트랜잭션 밖에서 이메일 발송 */
    public LetterResDTO createLetter(Long senderId, LetterCreateReqDTO req) {
        logEventEmitter.emit("letter_send_api_called", Map.of(
                "sender_id", senderId,
                "receiver_id", req.receiverId()
        ));

        // 1) 발신자 조회
        Member sender = memberGetService.getMember(senderId);

        // 2) 수신자 조회
        Member receiver = memberGetService.getMember(req.receiverId());

        // 3) 어느 방향이든 차단 관계가 있으면 저장·이벤트·메일보다 먼저 거부한다
        if (blockGetService.existsBetween(senderId, receiver.getId())) {
            throw new LetterBlockedException();
        }

        // 4) validation 로그 (@Valid 통과 이후 실행되므로 result=true)
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

        // 5) 엔티티 생성
        Letter letter = Letter.create(
                req.title(),
                req.content(),
                req.sns(),
                req.replyEmail(),
                sender,
                receiver
        );

        // 6) 저장 + 알림 이벤트 발행 (트랜잭션 커밋 → AFTER_COMMIT 리스너 발화)
        Letter saved = letterCreateService.save(letter);

        // 7) 이메일 본문 생성
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

        // 8) 이메일 전송 (트랜잭션 밖, 실패 시 예외 — 저장된 쪽지는 유지)
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

        logEventEmitter.emit("letter_send_api_succeeded", Map.of(
                "sender_id", senderId,
                "receiver_id", req.receiverId(),
                "letter_id", saved.getLetterId(),
                "email_sent", emailSent
        ));

        // 9) 저장된 엔티티 기반으로 Response 생성
        return LetterResDTO.from(saved);
    }

    /** 발신한 쪽지 목록 조회 */
    public Slice<LetterResDTO> getSentLetters(Long senderId, Pageable pageable) {
        return letterGetService.getSentLetters(senderId, pageable)
                .map(LetterResDTO::from);
    }
}
