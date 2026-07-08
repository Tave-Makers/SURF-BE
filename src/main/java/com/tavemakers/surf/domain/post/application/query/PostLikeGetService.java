package com.tavemakers.surf.domain.post.application.query;

import com.tavemakers.surf.domain.member.presentation.dto.response.MemberLikeListResDTO;
import com.tavemakers.surf.domain.post.presentation.dto.response.PostLikeListResDTO;
import com.tavemakers.surf.domain.post.domain.repository.PostLikeRepository;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeGetService {
    private final PostLikeRepository postLikeRepository;

    /** 게시글에 좋아요한 회원 목록 조회 */
    public PostLikeListResDTO getPostLikes(Long postId) {
        List<MemberLikeListResDTO> likes = postLikeRepository.findLikedMembersByPostId(postId)
                .stream()
                .map(MemberLikeListResDTO::from)
                .toList();

        return new PostLikeListResDTO(likes);
    }

    /** 특정 회원이 좋아요한 게시글 ID 목록 조회 */
    public Set<Long> findLikedPostIdsByMemberAndPostIds(Long memberId, Collection<Long> postIds) {
        return postLikeRepository.findLikedPostIdsByMemberAndPostIds(memberId, postIds);
    }
}
