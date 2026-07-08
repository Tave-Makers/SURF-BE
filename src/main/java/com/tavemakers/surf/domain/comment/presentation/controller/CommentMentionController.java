package com.tavemakers.surf.domain.comment.presentation.controller;

import com.tavemakers.surf.domain.comment.presentation.dto.response.MentionSearchResDTO;
import com.tavemakers.surf.domain.comment.domain.service.CommentMentionService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.tavemakers.surf.domain.comment.presentation.controller.ResponseMessage.*;

@Tag(name = "댓글 멘션", description = "댓글에서 멘션할 회원 검색 API (자동완성)")
@RestController
@RequestMapping
@RequiredArgsConstructor
public class CommentMentionController {

    private final CommentMentionService commentMentionService;

    @Operation(summary = "멘션할 회원 검색", description = "이름 두 글자 이상 입력 시 회원 검색")
    @GetMapping("/v1/user/comments/mentions/search")
    public ApiResponse<List<MentionSearchResDTO>> searchMentionableMembers(
            @RequestParam("keyword") String keyword
    ) {
        List<MentionSearchResDTO> result = commentMentionService.searchMentionableMembers(keyword);
        return ApiResponse.response(HttpStatus.OK, COMMENT_MENTION_SEARCH_SUCCESS.getMessage(), result);
    }
}
