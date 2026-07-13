package com.tavemakers.surf.application.comment.query;

import com.tavemakers.surf.presentation.comment.dto.response.MentionResDTO;
import com.tavemakers.surf.presentation.comment.dto.response.MentionSearchResDTO;
import com.tavemakers.surf.domain.comment.exception.InvalidMentionSearchKeywordException;
import com.tavemakers.surf.domain.comment.repository.CommentMentionRepository;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.application.member.query.MemberGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 댓글 멘션 read-model 조립. 멘션 조회·자동완성 검색의 표현형(DTO) 구성을 담당한다.
 * (쓰기(멘션 저장/삭제)는 domain/comment/service/CommentMentionService 가 담당)
 */
@Service
@RequiredArgsConstructor
public class CommentMentionGetService {

    private final CommentMentionRepository commentMentionRepository;
    private final MemberGetService memberGetService;

    /** 댓글에 달린 멘션 목록 조회 (DTO 변환). 호출자 트랜잭션 안에서 수행된다. */
    public List<MentionResDTO> getMentions(Long commentId) {
        return commentMentionRepository.findByCommentIdWithMember(commentId)
                .stream()
                .map(MentionResDTO::from)
                .toList();
    }

    /** 댓글 ID 목록으로 멘션 일괄 조회 (N+1 방지). 호출자 트랜잭션 안에서 수행된다. */
    public Map<Long, List<MentionResDTO>> getMentionsByCommentIds(List<Long> commentIds) {
        return commentMentionRepository.findAllByCommentIdIn(commentIds).stream()
                .collect(Collectors.groupingBy(
                        cm -> cm.getComment().getId(),
                        Collectors.mapping(MentionResDTO::from, Collectors.toList())
                ));
    }

    /** 멘션 가능한 회원 검색 (두 글자 이상 입력 시) */
    @Transactional(readOnly = true)
    public List<MentionSearchResDTO> searchMentionableMembers(String keyword) {

        // 입력 검증 (비었거나, 두 글자 미만일 경우 예외)
        if (keyword == null || keyword.trim().length() < 2) {
            throw new InvalidMentionSearchKeywordException();
        }

        // 검색어 정제
        String namePart = keyword.trim();

        // DB 조회 (정렬 없이)
        List<Member> candidates = memberGetService.findMentionCandidates(namePart, MemberStatus.WITHDRAWN);

        // 자바에서 정렬: 가장 최근 기수 → 오래된 순
        return candidates.stream()
                .limit(10)
                .map(MentionSearchResDTO::from)
                .toList();
    }
}
