package com.tavemakers.surf.domain.post.event;

import com.tavemakers.surf.global.common.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 게시글 첨부파일 삭제 이벤트 리스너 - 트랜잭션 커밋 이후 S3 파일 삭제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostFilesDeletedListener {

    private final S3Service s3Service;

    /** DB 커밋 이후 S3 파일 삭제 (실패해도 orphan 정리로 복구 가능하므로 예외를 전파하지 않는다) */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFilesDeleted(PostFilesDeletedEvent event) {
        event.fileUrls().forEach(fileUrl -> {
            try {
                s3Service.deleteFile(fileUrl);
            } catch (Exception e) {
                log.warn("[PostFileDelete] S3 파일 삭제 실패. fileUrl={}", fileUrl, e);
            }
        });
    }
}
