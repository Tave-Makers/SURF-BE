package com.tavemakers.surf.domain.post.entity;

import com.tavemakers.surf.domain.post.dto.request.PostImageCreateReqDTO;
import com.tavemakers.surf.global.common.entity.BaseEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImageUrl extends BaseEntity {

    @Id @Tsid
    @Column(name = "post_image_url_id")
    private Long id;

    @Column(nullable = false, length = 500)
    private String originalUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false)
    private Integer sequence;

    public static PostImageUrl of(Post post, PostImageCreateReqDTO dto) {
        return PostImageUrl.builder()
                .post(post)
                .originalUrl(dto.originalUrl())
                .sequence(dto.sequence())
                .build();
    }

}
