package com.tavemakers.surf.domain.post.controller.file;

import com.tavemakers.surf.domain.post.service.post.PostFileDeleteUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static com.tavemakers.surf.domain.post.controller.ResponseMessage.POST_FILE_DELETED;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "게시물", description = "게시물 첨부파일 삭제 API")
public class PostFileDeleteController {

    private final PostFileDeleteUsecase postFileDeleteUsecase;

    /** 게시글 첨부파일 삭제 (작성자/권한 검증은 서비스에서) */
    @Operation(summary = "게시글 첨부파일 삭제", description = "게시글에 첨부된 파일을 삭제합니다. S3 파일은 트랜잭션 커밋 이후 삭제됩니다.")
    @DeleteMapping("/v1/user/posts/files/{fileId}")
    public ApiResponse<Void> deletePostFile(
            @PathVariable(name = "fileId") Long fileId) {
        Long memberId = SecurityUtils.getCurrentMemberId();
        postFileDeleteUsecase.deletePostFile(fileId, memberId);
        return ApiResponse.response(HttpStatus.NO_CONTENT, POST_FILE_DELETED.getMessage());
    }
}
