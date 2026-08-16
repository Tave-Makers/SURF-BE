package com.tavemakers.surf.application.letter.usecase;

import com.tavemakers.surf.presentation.letter.dto.request.LetterCreateReqDTO;
import com.tavemakers.surf.presentation.letter.dto.response.LetterResDTO;
import com.tavemakers.surf.domain.letter.entity.Letter;
import com.tavemakers.surf.application.letter.query.LetterGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class LetterUsecase {

    private final MemberGetService memberGetService;
    private final LetterCreateService letterCreateService;
    private final LetterGetService letterGetService;
    private final LogEventEmitter logEventEmitter;

    /** 쪽지 생성 — 저장 커밋 후 이메일·알림은 AFTER_COMMIT 리스너가 비동기 발송 */
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

        // 3) 엔티티 생성
        Letter letter = Letter.create(
                req.title(),
                req.content(),
                req.sns(),
                req.replyEmail(),
                sender,
                receiver
        );

        // 4) 저장 + 알림·이메일 이벤트 발행 (트랜잭션 커밋 → AFTER_COMMIT 리스너가 비동기 발송)
        Letter saved = letterCreateService.save(letter);

        logEventEmitter.emit("letter_send_api_succeeded", Map.of(
                "sender_id", senderId,
                "receiver_id", req.receiverId(),
                "letter_id", saved.getLetterId()
        ));

        // 5) 저장된 엔티티 기반으로 Response 생성
        return LetterResDTO.from(saved);
    }

    /** 발신한 쪽지 목록 조회 */
    public Slice<LetterResDTO> getSentLetters(Long senderId, Pageable pageable) {
        return letterGetService.getSentLetters(senderId, pageable)
                .map(LetterResDTO::from);
    }
}
