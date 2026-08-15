package com.tavemakers.surf.presentation.block.dto.response;

import com.tavemakers.surf.domain.block.entity.Block;
import com.tavemakers.surf.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** 관리자 차단 관계 행 — 강제 해제에 필요한 blockId와 양쪽 회원을 함께 내려준다 */
@Schema(description = "관리자용 차단 관계")
public record BlockAdminResDTO(

        @Schema(description = "차단 관계 ID (강제 해제에 사용)", example = "101")
        Long blockId,

        @Schema(description = "차단을 등록한 회원")
        BlockMemberResDTO blocker,

        @Schema(description = "차단당한 회원")
        BlockMemberResDTO blocked,

        @Schema(description = "차단 일시")
        LocalDateTime blockedAt
) {

    public static BlockAdminResDTO of(Block block, Member blocker, Member blocked) {
        return new BlockAdminResDTO(
                block.getId(),
                BlockMemberResDTO.from(blocker),
                BlockMemberResDTO.from(blocked),
                block.getCreatedAt()
        );
    }
}
