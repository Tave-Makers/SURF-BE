package com.tavemakers.surf.domain.post.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/** 게시글 삭제/수정 권한 거부는 인증 실패(401)가 아니라 정책 거부(403)다 — 프론트의 불필요한 refresh 재시도 방지. */
class PostDeleteAccessDeniedExceptionTest {

    @Test
    @DisplayName("PostDeleteAccessDeniedException 은 403 FORBIDDEN 을 갖는다")
    void status_isForbidden() {
        assertThat(new PostDeleteAccessDeniedException().getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
