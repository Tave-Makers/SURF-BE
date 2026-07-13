package com.tavemakers.surf.domain.feedback.service;

import com.tavemakers.surf.domain.feedback.entity.Feedback;
import com.tavemakers.surf.domain.feedback.exception.TooManyFeedbackException;
import com.tavemakers.surf.domain.feedback.repository.FeedbackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private WriterHashService writerHashService;

    @InjectMocks
    private FeedbackService feedbackService;

    @Test
    @DisplayName("당일 작성 횟수가 제한(3회) 미만이면 피드백을 저장하고 반환한다")
    void 제한_미만이면_저장한다() {
        Long memberId = 1L;
        given(writerHashService.hashDaily(eq(memberId), any(LocalDate.class))).willReturn("hash-abc");
        given(feedbackRepository.countByWriterHash("hash-abc")).willReturn(2L);
        Feedback saved = Feedback.of("내용", "hash-abc");
        given(feedbackRepository.save(any(Feedback.class))).willReturn(saved);

        Feedback result = feedbackService.createFeedback("내용", memberId);

        assertThat(result).isSameAs(saved);
        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        then(feedbackRepository).should().save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("내용");
        assertThat(captor.getValue().getWriterHash()).isEqualTo("hash-abc");
    }

    @Test
    @DisplayName("당일 작성 횟수가 정확히 제한(3회)에 도달하면 초과 예외를 던지고 저장하지 않는다")
    void 제한에_도달하면_예외를_던진다() {
        Long memberId = 1L;
        given(writerHashService.hashDaily(eq(memberId), any(LocalDate.class))).willReturn("hash-abc");
        given(feedbackRepository.countByWriterHash("hash-abc")).willReturn(3L);

        assertThatThrownBy(() -> feedbackService.createFeedback("내용", memberId))
                .isInstanceOf(TooManyFeedbackException.class);

        then(feedbackRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("당일 작성 횟수가 제한을 초과했으면 예외를 던진다")
    void 제한_초과시에도_예외를_던진다() {
        given(writerHashService.hashDaily(anyLong(), any(LocalDate.class))).willReturn("hash-abc");
        given(feedbackRepository.countByWriterHash("hash-abc")).willReturn(5L);

        assertThatThrownBy(() -> feedbackService.createFeedback("내용", 1L))
                .isInstanceOf(TooManyFeedbackException.class);
    }

    @Test
    @DisplayName("피드백 목록 조회는 생성일 역순 페이징을 리포지토리에 위임한다")
    void 목록_조회는_리포지토리에_위임한다() {
        Pageable pageable = PageRequest.of(0, 10);
        Slice<Feedback> expected = new SliceImpl<>(List.of());
        given(feedbackRepository.findAllByOrderByCreatedAtDesc(pageable)).willReturn(expected);

        Slice<Feedback> result = feedbackService.getFeedbacks(pageable);

        assertThat(result).isSameAs(expected);
        then(feedbackRepository).should().findAllByOrderByCreatedAtDesc(pageable);
    }
}
