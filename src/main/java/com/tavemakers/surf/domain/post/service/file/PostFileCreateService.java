package com.tavemakers.surf.domain.post.service.file;

import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.entity.PostFileUrl;
import com.tavemakers.surf.domain.post.repository.PostFileUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostFileCreateService {

    private final PostFileUrlRepository postFileUrlRepository;

    /** 게시글 첨부파일 생성 입력 (표현형 DTO를 도메인에 노출하지 않기 위한 도메인 입력 타입) */
    public record FileData(String fileUrl, String originalFileName, Integer sequence) {}

    /** 게시글 첨부파일 일괄 저장 (트랜잭션 경계는 호출자 usecase가 소유) */
    public List<PostFileUrl> saveAll(Post post, List<FileData> fileDataList) {
        if (fileDataList == null || fileDataList.isEmpty()) {
            return List.of();
        }

        List<PostFileUrl> filesUrlList = fileDataList.stream()
                .map(data -> PostFileUrl.of(post, data.fileUrl(), data.originalFileName(), data.sequence()))
                .toList();
        return postFileUrlRepository.saveAll(filesUrlList);
    }
}
