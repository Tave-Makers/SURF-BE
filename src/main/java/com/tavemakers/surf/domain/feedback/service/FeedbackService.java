package com.tavemakers.surf.domain.feedback.service;

import com.tavemakers.surf.domain.feedback.entity.Feedback;
import com.tavemakers.surf.domain.feedback.exception.TooManyFeedbackException;
import com.tavemakers.surf.domain.feedback.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 피드백 도메인 로직. DTO를 알지 못하며 엔티티만 다룬다.
 * 트랜잭션 경계는 호출자(FeedbackUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final WriterHashService writerHashService;

    private static final int DAILY_LIMIT = 3; // 하루 최대 3회

    /** 피드백 생성 (하루 3회 제한) */
    public Feedback createFeedback(String content, Long memberId) {
        String writerHash = writerHashService.hashDaily(memberId, LocalDate.now());
        long todayCount = feedbackRepository.countByWriterHash(writerHash);
        if (todayCount >= DAILY_LIMIT) throw new TooManyFeedbackException();
        return feedbackRepository.save(Feedback.of(content, writerHash));
    }

    /** 피드백 목록 페이징 조회 */
    public Slice<Feedback> getFeedbacks(Pageable pageable) {
        return feedbackRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
