package com.tavemakers.surf.application.post.usecase;

import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.entity.PostFileUrl;
import com.tavemakers.surf.domain.post.entity.PostImageUrl;
import com.tavemakers.surf.domain.post.service.file.PostFileCreateService;
import com.tavemakers.surf.domain.post.service.image.PostImageCreateService;
import com.tavemakers.surf.domain.post.service.post.PostCreateService;

import com.tavemakers.surf.presentation.post.dto.request.PostCreateReqDTO;
import com.tavemakers.surf.presentation.post.dto.request.PostFileCreateReqDTO;
import com.tavemakers.surf.presentation.post.dto.request.PostImageCreateReqDTO;
import com.tavemakers.surf.presentation.post.dto.response.PostDetailResDTO;
import com.tavemakers.surf.presentation.post.dto.response.PostFileResDTO;
import com.tavemakers.surf.presentation.post.dto.response.PostImageResDTO;
import com.tavemakers.surf.application.reservation.usecase.ReservationUsecase;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 게시글 생성 Usecase — 트랜잭션 경계를 소유하고 ReqDTO를 해체해 도메인에 전달하며
 * 도메인 결과(엔티티)를 표현형(DTO)으로 매핑한다.
 */
@Service
@RequiredArgsConstructor
public class PostCreateUsecase {

    private final PostCreateService postCreateService;
    private final ReservationUsecase reservationUsecase;
    private final LogEventEmitter logEventEmitter;

    /** 게시글 생성 (예약 포함) */
    @Transactional
    public PostDetailResDTO createPost(PostCreateReqDTO req, Long memberId) {
        PostCreateService.PostCreateResult result = postCreateService.createPost(
                req.title(), req.content(), req.pinned(), req.isReserved(), req.hasSchedule(),
                req.boardId(), req.categoryId(),
                toImageData(req.imageUrlList()), toFileData(req.fileList()), memberId);

        Post saved = result.post();

        if (req.isReserved()) {
            reservationUsecase.reservePost(saved.getId(), req.reservedAt());
        }

        logEventEmitter.emit("post.create", Map.of(
                "post_id", saved.getId(),
                "board_id", req.boardId(),
                "title_length", req.title().length(),
                "has_image", req.hasImage()
        ));

        LocalDateTime reservedAt = req.isReserved() ? req.reservedAt() : null;
        return PostDetailResDTO.of(saved, false, false, true,
                toImageResponse(result.images()), toFileResponse(result.files()), reservedAt, 0);
    }

    private List<PostImageCreateService.ImageData> toImageData(List<PostImageCreateReqDTO> list) {
        if (list == null) return null;
        return list.stream()
                .map(d -> new PostImageCreateService.ImageData(d.originalUrl(), d.sequence()))
                .toList();
    }

    private List<PostFileCreateService.FileData> toFileData(List<PostFileCreateReqDTO> list) {
        if (list == null) return null;
        return list.stream()
                .map(d -> new PostFileCreateService.FileData(d.fileUrl(), d.originalFileName(), d.sequence()))
                .toList();
    }

    private List<PostImageResDTO> toImageResponse(List<PostImageUrl> images) {
        if (images.isEmpty()) return null;
        return images.stream()
                .map(PostImageResDTO::from)
                .sorted(Comparator.comparing(PostImageResDTO::sequence))
                .toList();
    }

    private List<PostFileResDTO> toFileResponse(List<PostFileUrl> files) {
        if (files.isEmpty()) return null;
        return files.stream()
                .map(PostFileResDTO::from)
                .toList();
    }
}
