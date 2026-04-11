package com.tavemakers.surf.domain.board.entity;

import com.tavemakers.surf.domain.board.dto.request.BoardCategoryCreateReqDTO;
import com.tavemakers.surf.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(
        name = "board_category",
        uniqueConstraints = @UniqueConstraint(columnNames = {"board_id", "slug"}),
        indexes = {
                @Index(name = "idx_boardcategory_board", columnList = "board_id"),
                @Index(name = "idx_boardcategory_slug", columnList = "slug")
        }
)
public class BoardCategory extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // nullable=false 유지
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(nullable = false) @NotBlank
    private String name;

    @Column(nullable = false) @NotBlank
    private String slug; // 전역 unique 제거(중요)

    public void update(String name, String slug) {
        if (name != null) this.name = name;
        if (slug != null) this.slug = slug;
    }

    public static BoardCategory of(Board board, BoardCategoryCreateReqDTO req) {
        return BoardCategory.builder()
                .board(board)
                .name(req.name())
                .slug(req.slug())
                .build();
    }
}