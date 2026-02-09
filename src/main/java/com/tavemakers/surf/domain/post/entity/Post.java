package com.tavemakers.surf.domain.post.entity;

import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardCategory;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.post.dto.request.PostCreateReqDTO;
import com.tavemakers.surf.domain.post.dto.request.PostUpdateReqDTO;
import com.tavemakers.surf.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    private static final String WEBP_EXTENSION = ".webp";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @NotBlank
    private String title;

    @NotBlank
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    private LocalDateTime postedAt;

    @Column(length = 500)
    private String thumbnailUrl;

    private boolean pinned; // 상단 고정

    @Column(nullable = false)
    private long scrapCount = 0L;

    private long likeCount = 0L;

    private long commentCount = 0L;

    @Version
    private Long version;

    private boolean isReserved;

    private int viewCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;
    private String boardName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private BoardCategory category;
    private String categoryName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private Boolean hasSchedule = false;

    @Column(nullable = true)
    private Long scheduleId;


    public static Post of(PostCreateReqDTO req, Board board, BoardCategory category, Member member) {
        return Post.builder()
                .title(req.title())
                .content(req.content())
                .pinned(req.pinned() != null ? req.pinned() : false)
                .postedAt(LocalDateTime.now())
                .board(board)
                .boardName(board.getName())
                .category(category)
                .categoryName(category.getName())
                .member(member)
                .scrapCount(0L)
                .likeCount(0L)
                .commentCount(0L)
                .isReserved(req.isReserved())
                .hasSchedule(req.hasSchedule() != null ? req.hasSchedule() : false)
                .viewCount(0)
                .build();
    }

    public void update(PostUpdateReqDTO req, Board board, BoardCategory category) {
        this.title = req.title();
        this.content = req.content();
        this.pinned = req.pinned() != null ? req.pinned() : this.pinned;

        this.board = board;
        this.boardName = board.getName();

        this.category = category;
        this.categoryName = category.getName();

        this.hasSchedule = req.hasSchedule() != null ? req.hasSchedule() : this.hasSchedule;

        if (Boolean.FALSE.equals(req.hasSchedule())) {
            this.scheduleId = null;
        }
    }

    @PrePersist
    void syncNamesOnInsert() {
        // INSERT 전에 한 번 더 동기화 (NPE 방지)
        if (board != null) this.boardName = board.getName();
        if (category != null) this.categoryName = category.getName();
    }

    public void increaseCommentCount() {
        this.commentCount++;
    }

    public void decreaseCommentCount() {
        if (this.commentCount > 0) this.commentCount--;
    }

    public void publish() {
        this.isReserved = false;
        this.postedAt = LocalDateTime.now();
    }

    public void addThumbnailUrl(String originalUrl) {
        if (originalUrl == null) {
            this.thumbnailUrl = null;
            return;
        }

        String url = originalUrl.replace("/original/", "/thumbnail/");
        int dotIndex = url.lastIndexOf('.');
        this.thumbnailUrl = url.substring(0, dotIndex) + WEBP_EXTENSION;
    }

    public boolean isOwner(Long memberId) {
        return member.getId().equals(memberId);
    }

    public void changeHasSchedule(boolean hasSchedule) {
        this.hasSchedule = hasSchedule;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void addScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public void updateScheduleIdNull(){
        this.scheduleId = null;
    }

}