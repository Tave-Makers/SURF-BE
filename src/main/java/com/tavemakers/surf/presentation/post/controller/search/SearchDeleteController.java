package com.tavemakers.surf.presentation.post.controller.search;

import com.tavemakers.surf.domain.post.service.search.RecentSearchService;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.presentation.post.controller.ResponseMessage.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "검색", description = "게시글 검색 및 최근 검색어 관련 API")
public class SearchDeleteController {

    private final RecentSearchService recentSearchService;

    /** 최근 검색어 전체 삭제 */
    @Operation(summary = "최근 검색어 전체 삭제", description = "최근 검색어를 전체 삭제합니다.")
    @DeleteMapping("/v1/user/search/recent")
    public ApiResponse<Void> clear() {
        Long memberId = SecurityUtils.getCurrentMemberId();
        recentSearchService.clearAll(memberId);
        return ApiResponse.response(HttpStatus.NO_CONTENT, RECENT_SEARCH_DELETED.getMessage());
    }

    /** 최근 검색어 단건 삭제 */
    @Operation(summary = "최근 검색어 단건 삭제", description = "최근 검색어를 단건 삭제합니다.")
    @DeleteMapping("/v1/user/search/recent/{keyword}")
    public ApiResponse<Void> deleteOne(
            @PathVariable String keyword
    ) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        recentSearchService.deleteOne(memberId, keyword);
        return ApiResponse.response(HttpStatus.NO_CONTENT, RECENT_SEARCH_ONE_DELETED.getMessage());
    }
}
