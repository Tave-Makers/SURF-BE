package com.tavemakers.surf.presentation.post.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PostScheduleTagValidationTest {
    private PostUpdateReqDTO update(Boolean hasSchedule, Long scheduleId) {
        return new PostUpdateReqDTO(null, null, null, null, null, null,
                null, null, null, null, hasSchedule, scheduleId);
    }

    @Test
    void validatesLinkDetachAndUnchangedContracts() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(update(null, 5L))).isEmpty();
            assertThat(validator.validate(update(true, 5L))).isEmpty();
            assertThat(validator.validate(update(false, null))).isEmpty();
            assertThat(validator.validate(update(null, null))).isEmpty();
            assertThat(validator.validate(update(false, 5L))).isNotEmpty();
            assertThat(validator.validate(update(true, null))).isNotEmpty();
            assertThat(validator.validate(update(null, -1L))).isNotEmpty();
        }
    }

    @Test
    void maskedCopyPreservesScheduleId() {
        var masked = update(null, 5L).withMaskedText(null, null);
        assertThat(masked.scheduleId()).isEqualTo(5L);
    }

    @Test
    void creationRequiresIdWhenFlagIsTrue() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new PostCreateReqDTO(
                    1L, 2L, "제목", "본문", false, null, null, null, true, null))).isNotEmpty();
            assertThat(validator.validate(new PostCreateReqDTO(
                    1L, 2L, "제목", "본문", false, null, null, null, null, 5L))).isEmpty();
        }
    }
}
