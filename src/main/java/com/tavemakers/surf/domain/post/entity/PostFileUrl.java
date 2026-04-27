package com.tavemakers.surf.domain.post.entity;

import com.tavemakers.surf.domain.post.dto.request.PostFileCreateReqDTO;
import com.tavemakers.surf.global.common.entity.BaseEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostFileUrl extends BaseEntity {

    @Id @Tsid
    @Column(name = "post_file_url_id")
    private Long id;

    @Column(nullable = false, length = 500)
    private String fileUrl;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    private Integer sequence;

    public static PostFileUrl of(Post post, PostFileCreateReqDTO dto) {
        return PostFileUrl.builder()
                .post(post)
                .fileUrl(dto.fileUrl())
                .originalFileName(dto.originalFileName())
                .sequence(dto.sequence())
                .build();
    }
}
