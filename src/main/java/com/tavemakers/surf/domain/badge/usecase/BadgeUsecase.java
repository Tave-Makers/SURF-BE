package com.tavemakers.surf.domain.badge.usecase;

import com.tavemakers.surf.domain.badge.dto.request.BadgeCreateReqDTO;
import com.tavemakers.surf.domain.badge.dto.request.BadgeUpdateReqDTO;
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
@Transactional
public class BadgeUsecase {

    private final BadgeCreateService badgeCreateService;
    private final BadgeUpdateService badgeUpdateService;
    private final BadgeDeleteService badgeDeleteService;
    private final BadgeGetService badgeGetService;

    /** 배지 생성 */
    public Long create(BadgeCreateReqDTO dto) {
        return badgeCreateService.create(dto);
    }

    /** 활성 배지 목록 조회 */
    public BadgeSliceResDTO getBadgeList(int pageSize, int pageNum) {

        // 페이지 번호, 사이즈, 정렬조건(id 내림차순) 기반 Pageable 생성
        Pageable pageable = PageRequest.of(
                pageNum,
                pageSize,
                Sort.by(Sort.Direction.DESC, "id")
        );

        // 활성 배지 목록을 Slice 형태로 조회
        Slice<Badge> slice =
                badgeGetService.getBadgeList(pageNum);

        return BadgeSliceResDTO.from(slice.map(BadgeResDTO::from));
    }

    /** 배지 수정 */
    public void update(Long badgeId, BadgeUpdateReqDTO dto) {
        badgeUpdateService.update(badgeId, dto);
    }

    /** 배지 삭제 */
    public void delete(Long badgeId) {
        badgeDeleteService.delete(badgeId);
    }
}