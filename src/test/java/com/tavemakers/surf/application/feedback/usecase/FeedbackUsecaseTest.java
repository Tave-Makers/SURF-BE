package com.tavemakers.surf.application.feedback.usecase;

import com.tavemakers.surf.domain.feedback.entity.Feedback;
import com.tavemakers.surf.domain.feedback.service.FeedbackService;
import com.tavemakers.surf.presentation.feedback.dto.request.FeedbackCreateReqDTO;
import com.tavemakers.surf.presentation.feedback.dto.response.FeedbackResDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class FeedbackUsecaseTest {

    @Mock
    private FeedbackService feedbackService;

    @InjectMocks
    private FeedbackUsecase feedbackUsecase;

    private Feedback feedback(Long id, String content, String writerHash, LocalDateTime createdAt) {
        Feedback feedback = Feedback.of(content, writerHash);
        ReflectionTestUtils.setField(feedback, "id", id);
        ReflectionTestUtils.setField(feedback, "createdAt", createdAt);
        return feedback;
    }

    @Test
    @DisplayName("피드백 생성은 요청 content와 memberId를 도메인 서비스로 그대로 전달하고, 반환된 엔티티를 ResDTO로 매핑한다")
    void createFeedback_delegatesAndMapsEntityToDto() {
        Long memberId = 10L;
        FeedbackCreateReqDTO req = new FeedbackCreateReqDTO("열심히 일해주세요");
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 10, 12, 0);
        Feedback saved = feedback(1L, "열심히 일해주세요", "hash-abc", createdAt);
        given(feedbackService.createFeedback("열심히 일해주세요", memberId)).willReturn(saved);

        FeedbackResDTO result = feedbackUsecase.createFeedback(req, memberId);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.content()).isEqualTo("열심히 일해주세요");
        assertThat(result.createdAt()).isEqualTo(createdAt);
        then(feedbackService).should().createFeedback("열심히 일해주세요", memberId);
    }

    @Test
    @DisplayName("피드백 목록 조회는 도메인 서비스가 반환한 엔티티 Slice를 ResDTO Slice로 매핑한다")
    void getFeedbacks_mapsEntitySliceToDtoSlice() {
        Pageable pageable = PageRequest.of(0, 20);
        Feedback f1 = feedback(1L, "첫번째", "h1", LocalDateTime.of(2026, 7, 10, 10, 0));
        Feedback f2 = feedback(2L, "두번째", "h2", LocalDateTime.of(2026, 7, 10, 11, 0));
        Slice<Feedback> entitySlice = new SliceImpl<>(List.of(f1, f2), pageable, false);
        given(feedbackService.getFeedbacks(pageable)).willReturn(entitySlice);

        Slice<FeedbackResDTO> result = feedbackUsecase.getFeedbacks(pageable);

        assertThat(result.getContent())
                .extracting(FeedbackResDTO::id, FeedbackResDTO::content)
                .containsExactly(
                        tuple(1L, "첫번째"),
                        tuple(2L, "두번째")
                );
    }
}
