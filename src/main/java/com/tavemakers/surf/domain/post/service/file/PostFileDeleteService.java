package com.tavemakers.surf.domain.post.service.file;

import com.tavemakers.surf.domain.post.entity.PostFileUrl;
import com.tavemakers.surf.domain.post.repository.PostFileUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostFileDeleteService {

    private final PostFileUrlRepository repository;

    /** 게시글 첨부파일 일괄 삭제 */
    @Transactional
    public void deleteAll(List<PostFileUrl> beforeFiles) {
        if (beforeFiles == null || beforeFiles.isEmpty()) {
            return;
        }
        repository.deleteAllInBatch(beforeFiles);
    }
}
