package com.tavemakers.surf.domain.comment.service;

import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.comment.entity.CommentMention;
import com.tavemakers.surf.domain.comment.repository.CommentMentionRepository;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.application.member.query.MemberGetService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 댓글 멘션 쓰기 도메인 로직. DTO를 알지 못하며 엔티티만 다룬다.
 * 트랜잭션 경계는 호출자(usecase)가 소유한다.
 * (멘션 조회·자동완성 검색의 read-model 조립은 application/comment/query/CommentMentionGetService 가 담당)
 */
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
}
