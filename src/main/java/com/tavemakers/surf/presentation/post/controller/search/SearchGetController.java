package com.tavemakers.surf.presentation.post.controller.search;

import com.tavemakers.surf.presentation.post.dto.response.PostResDTO;
import com.tavemakers.surf.domain.post.service.search.PostSearchService;
import com.tavemakers.surf.domain.post.service.search.RecentSearchService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.tavemakers.surf.presentation.post.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "검색", description = "게시글 검색 및 최근 검색어 관련 API")
public class SearchGetController {

    private final PostSearchService postSearchService;
    private final RecentSearchService recentSearchService;

    /** 게시글 검색 + 최근검색 저장 */
    @Operation(summary = "게시글 검색", description = "게시글을 제목과 본문 기준으로 검색하고, 최근 검색어를 저장합니다.")
    @GetMapping("/v1/user/search/posts")
    public ApiResponse<Slice<PostResDTO>> searchPosts(
            @RequestParam @NotBlank(message = "검색어를 입력해주세요") String param,
            @PageableDefault(size = 20, sort = {"postedAt","id"}) Pageable pageable) {

        param = param.trim();
        Long memberId = SecurityUtils.getCurrentMemberId();
        Slice<PostResDTO> response = postSearchService.search(memberId, param, pageable);
        return ApiResponse.response(HttpStatus.OK, SEARCH_COMPLETED.getMessage(), response);
    }

    /** 최근 검색어 10개 */
    @Operation(summary = "최근 검색어 조회", description = "최근 검색어 10개를 조회합니다.")
    @GetMapping("/v1/user/search/recent")
    public ApiResponse<List<String>> recent() {
        Long memberId = SecurityUtils.getCurrentMemberId();
        List<String> response = recentSearchService.getRecent10(memberId);
        return ApiResponse.response(HttpStatus.OK, RECENT_SEARCH_READ.getMessage(), response);
    }
}
