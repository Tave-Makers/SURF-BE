package com.tavemakers.surf.domain.post.event;

import java.util.List;

/**
 * 게시글 첨부파일 DB 삭제 완료 이벤트 - 트랜잭션 커밋 이후 S3 파일 삭제용
 */
public record PostFilesDeletedEvent(List<String> fileUrls) {
}
