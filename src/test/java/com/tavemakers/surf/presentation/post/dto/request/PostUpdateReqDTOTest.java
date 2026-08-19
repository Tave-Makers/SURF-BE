package com.tavemakers.surf.presentation.post.dto.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * `withMaskedText` 사본 계약 검증.
 * title·content만 교체하고 나머지 필드는 그대로, null은 null로 보존해야 한다.
 */
class PostUpdateReqDTOTest {

    private static final LocalDateTime RESERVED_AT = LocalDateTime.of(2026, 8, 15, 10, 0);
    private static final List<PostImageCreateReqDTO> IMAGES =
            List.of(new PostImageCreateReqDTO("https://img/1.png", 1));
    private static final List<PostFileCreateReqDTO> FILES =
            List.of(new PostFileCreateReqDTO("https://file/1.pdf", "자료.pdf", 1));

    private PostUpdateReqDTO req(String title, String content) {
        return req(title, content, true, false, false);
    }

    private PostUpdateReqDTO req(String title, String content,
                                 boolean isReservationChanged,
                                 boolean isImageChanged,
                                 boolean isFileChanged) {
        return new PostUpdateReqDTO(
                title, content, 2L, true,
                isReservationChanged, RESERVED_AT,
                isImageChanged, IMAGES, isFileChanged, FILES, false);
    }

    @Test
    @DisplayName("title·content는 마스킹 값으로 교체되고 나머지 필드는 그대로 복사된다")
    void withMaskedText_replacesTextAndKeepsOtherFields() {
        PostUpdateReqDTO origin = req("씨발 제목", "씨발 본문");

        PostUpdateReqDTO masked = origin.withMaskedText("** 제목", "** 본문");

        assertThat(masked.title()).isEqualTo("** 제목");
        assertThat(masked.content()).isEqualTo("** 본문");
        assertThat(masked.categoryId()).isEqualTo(2L);
        assertThat(masked.pinned()).isTrue();
        assertThat(masked.isReservationChanged()).isTrue();
        assertThat(masked.reservedAt()).isEqualTo(RESERVED_AT);
        assertThat(masked.isImageChanged()).isFalse();
        assertThat(masked.imageUrlList()).isEqualTo(IMAGES);
        assertThat(masked.isFileChanged()).isFalse();
        assertThat(masked.fileList()).isEqualTo(FILES);
        assertThat(masked.hasSchedule()).isFalse();
        assertThat(origin.title()).as("원본은 변경되지 않는다").isEqualTo("씨발 제목");
    }

    /**
     * 동형(Boolean) 필드끼리 생성자 인자 순서가 교차되지 않았는지 전수 검사한다.
     * Boolean은 값이 2개뿐이라 한 조합만으로는 교차를 못 잡으므로,
     * 세 플래그가 각각 유일한 값을 갖는 세 패턴을 돌려 모든 쌍을 커버한다.
     */
    @ParameterizedTest(name = "reservation={0}, image={1}, file={2}")
    @CsvSource({"true,false,false", "false,true,false", "false,false,true"})
    @DisplayName("title·content 외 모든 record 컴포넌트가 원본과 동일하다(필드 교차 방지)")
    void withMaskedText_keepsEveryOtherComponent(boolean reservation, boolean image, boolean file)
            throws Exception {
        PostUpdateReqDTO origin = req("제목", "본문", reservation, image, file);

        PostUpdateReqDTO masked = origin.withMaskedText("**", "**");

        for (RecordComponent component : PostUpdateReqDTO.class.getRecordComponents()) {
            String name = component.getName();
            if (name.equals("title") || name.equals("content")) continue;
            assertThat(component.getAccessor().invoke(masked))
                    .as(name)
                    .isEqualTo(component.getAccessor().invoke(origin));
        }
    }

    @Test
    @DisplayName("null 입력은 null로 보존된다 — PATCH의 'null이면 기존값 유지' 계약")
    void withMaskedText_preservesNull() {
        PostUpdateReqDTO origin = req(null, null);

        PostUpdateReqDTO masked = origin.withMaskedText(null, null);

        assertThat(masked.title()).isNull();
        assertThat(masked.content()).isNull();
        assertThat(masked.categoryId()).isEqualTo(2L);
    }
}
