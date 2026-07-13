package com.tavemakers.surf.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 게시판→게시글→조회→좋아요→댓글→댓글좋아요 로 이어지는 도메인 횡단 플로우 E2E.
 *
 * <p>관통 도메인/계층:
 * <ul>
 *   <li>board: 관리자 게시판/카테고리 생성 (presentation → application.board.usecase → domain.board.service → repository)</li>
 *   <li>post: 게시글 생성/상세조회 (usecase 트랜잭션 + 이벤트 발행 + ViewCountService Redis 폴백)</li>
 *   <li>post.like / comment.like: 좋아요 토글</li>
 *   <li>comment: 댓글 생성 (usecase → domain.comment + mention 조회 조립)</li>
 *   <li>member: 작성자/뷰어 로딩(MemberGetService), 권한(JWT filter)</li>
 * </ul>
 * HTTP 요청 → 상태코드/응답 바디 필드 → DB 반영(PostRepository)까지 검증한다.
 */
class PostCommentFlowE2ETest extends E2ESupport {

    @Autowired
    private PostRepository postRepository;

    private Long readDataField(MvcResult result, String field) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return root.path("data").path(field).asLong();
    }

    @Test
    @DisplayName("게시판 개설 → 게시글 작성 → 조회 → 좋아요 → 댓글 → 댓글 좋아요 전체 플로우")
    void fullBoardPostCommentFlow() throws Exception {
        Member admin = persistMember(MemberRole.ADMIN);   // 게시판/카테고리 개설 + 게시글 작성
        Member reader = persistMember(MemberRole.MEMBER); // 조회/좋아요/댓글 작성

        // 1) 게시판 생성 (관리자) — GENERAL 이라 일반 회원도 글쓰기 가능
        MvcResult boardRes = mockMvc.perform(post("/v1/admin/boards")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "E2E 자유게시판",
                                "type", "GENERAL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andReturn();
        Long boardId = readDataField(boardRes, "id");
        assertThat(boardId).isPositive();

        // 2) 카테고리 생성 (관리자)
        MvcResult categoryRes = mockMvc.perform(post("/v1/admin/boards/" + boardId + "/categories")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "잡담",
                                "slug", "e2e-chat"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andReturn();
        Long categoryId = readDataField(categoryRes, "id");

        // 3) 게시글 작성 (관리자가 작성)
        MvcResult postRes = mockMvc.perform(post("/v1/user/posts")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "boardId", boardId,
                                "categoryId", categoryId,
                                "title", "E2E 통합 게시글",
                                "content", "layer-first 재편 후 배선 검증"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.title").value("E2E 통합 게시글"))
                .andReturn();
        Long postId = readDataField(postRes, "postId");

        // DB 반영 검증
        assertThat(postRepository.findById(postId)).isPresent();

        // 4) 게시글 상세 조회 (다른 회원 = reader 시점; ViewCountService 는 Redis 부재 시 DB 폴백)
        mockMvc.perform(get("/v1/user/posts/" + postId)
                        .header("Authorization", bearer(reader)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.postId").value(postId))
                .andExpect(jsonPath("$.data.title").value("E2E 통합 게시글"))
                .andExpect(jsonPath("$.data.likedByMe").value(false));

        // 5) 게시글 좋아요 (reader)
        mockMvc.perform(post("/v1/user/posts/" + postId + "/like")
                        .header("Authorization", bearer(reader)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 좋아요 반영: 다시 조회하면 likedByMe = true
        mockMvc.perform(get("/v1/user/posts/" + postId)
                        .header("Authorization", bearer(reader)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likedByMe").value(true));

        // 6) 댓글 작성 (reader)
        MvcResult commentRes = mockMvc.perform(post("/v1/user/posts/" + postId + "/comments")
                        .header("Authorization", bearer(reader))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "통합 테스트 댓글입니다"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.content").value("통합 테스트 댓글입니다"))
                .andReturn();
        Long commentId = readDataField(commentRes, "id");

        // 7) 댓글 좋아요 토글 (admin) → liked=true
        mockMvc.perform(post("/v1/user/comments/" + commentId + "/like")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.liked").value(true));
    }
}
