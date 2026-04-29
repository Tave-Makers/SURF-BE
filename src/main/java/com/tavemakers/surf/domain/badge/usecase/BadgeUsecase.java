package com.tavemakers.surf.domain.badge.usecase;

import com.tavemakers.surf.domain.badge.dto.request.BadgeCreateReqDTO;
import com.tavemakers.surf.domain.badge.dto.request.BadgeUpdateReqDTO;
import com.tavemakers.surf.domain.badge.dto.response.BadgeDetailResDTO;
import com.tavemakers.surf.domain.badge.dto.response.BadgeResDTO;
import com.tavemakers.surf.domain.badge.dto.response.BadgeSliceResDTO;
import com.tavemakers.surf.domain.badge.entity.Badge;
import com.tavemakers.surf.domain.badge.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BadgeUsecase {

    private final BadgeCreateService badgeCreateService;
    private final BadgeUpdateService badgeUpdateService;
    private final BadgeDeleteService badgeDeleteService;
    private final BadgeGetService badgeGetService;

    /** 배지 생성 */
    @Transactional
    public Long create(BadgeCreateReqDTO dto) {
        return badgeCreateService.create(dto);
    }

    /** 배지 리스트 조회 */
    @Transactional(readOnly = true)
    public BadgeSliceResDTO getBadgeList(int pageSize, int pageNum) {

        // 페이지 번호, 사이즈, 정렬조건(id 내림차순) 기반 Pageable 생성
        Pageable pageable = PageRequest.of(
                pageNum,
                pageSize,
                Sort.by(Sort.Direction.DESC, "id")
        );

        // 활성 배지 목록을 Slice 형태로 조회
        Slice<Badge> slice = badgeGetService.getBadgeList(pageable);

        return BadgeSliceResDTO.from(slice.map(BadgeResDTO::from));
    }

    /** 배지 단건 조회 */
    @Transactional(readOnly = true)
    public BadgeDetailResDTO getBadge(Long badgeId) {
        Badge badge = badgeGetService.getBadgeDetail(badgeId);
        return BadgeDetailResDTO.from(badge);
    }

    /** 배지 수정 */
    @Transactional
    public void update(Long badgeId, BadgeUpdateReqDTO dto) {
        badgeUpdateService.update(badgeId, dto);
    }

    /** 배지 삭제 */
    @Transactional
    public void delete(Long badgeId) {
        badgeDeleteService.delete(badgeId);
    }
}