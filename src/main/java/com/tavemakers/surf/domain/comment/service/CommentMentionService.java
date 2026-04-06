package com.tavemakers.surf.domain.comment.service;

import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.comment.entity.CommentMention;
import com.tavemakers.surf.domain.comment.repository.CommentMentionRepository;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.service.MemberGetService;
import com.tavemakers.surf.domain.comment.dto.response.MentionResDTO;
import com.tavemakers.surf.domain.comment.dto.response.MentionSearchResDTO;
import com.tavemakers.surf.domain.comment.exception.InvalidMentionSearchKeywordException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentMentionService {

    private final CommentMentionRepository commentMentionRepository;
    private final MemberGetService memberGetService;

    /** 댓글 생성 시 멘션이 있으면 저장 */
    public List<CommentMention> createMentions(Comment comment, List<Long> mentionMemberIds) {
        if (mentionMemberIds == null || mentionMemberIds.isEmpty()) {
            return List.of();
        }

        // 중복 제거
        List<Long> filteredIds = mentionMemberIds.stream()
                .distinct()
                .toList();

        List<Member> mentionedMembers = memberGetService.getMembersByIds(filteredIds);

        List<CommentMention> mentions = mentionedMembers.stream()
                .map(member -> CommentMention.of(comment, member))
                .toList();

        return commentMentionRepository.saveAll(mentions);
    }

    /** 댓글 삭제 시 멘션 전체 삭제 */
    public void deleteAllByComment(Comment comment) {
        commentMentionRepository.deleteAllByComment(comment);
    }

    /** 댓글에 달린 멘션 목록 조회 (DTO 변환) */
    @Transactional(readOnly = true)
    public List<MentionResDTO> getMentions(Long commentId) {
        return commentMentionRepository.findByCommentIdWithMember(commentId)
                .stream()
                .map(MentionResDTO::from)
                .toList();
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
