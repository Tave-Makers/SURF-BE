package com.tavemakers.surf.domain.post.domain.service.support;

import com.tavemakers.surf.domain.post.presentation.dto.response.PostResDTO;
import com.tavemakers.surf.domain.post.domain.entity.Post;
import com.tavemakers.surf.domain.post.domain.repository.PostLikeRepository;
import com.tavemakers.surf.domain.scrap.application.query.ScrapGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class FlagsMapper {

    private final ScrapGetService scrapGetService;
    private final PostLikeRepository postLikeRepository;

    public record Flags(Set<Long> scrappedIds, Set<Long> likedIds) {}

    public Flags resolveFlags(Long viewerId, List<Post> posts) {
        List<Long> ids = posts.stream()
                .map(Post::getId)
                .toList();

        if (ids.isEmpty()) {
            return new Flags(Set.of(), Set.of());
        }

        Set<Long> scrappedIds =
                scrapGetService.findScrappedPostIdsByMemberAndPostIds(viewerId, ids);
        Set<Long> likedIds =
                postLikeRepository.findLikedPostIdsByMemberAndPostIds(viewerId, ids);

        return new Flags(scrappedIds, likedIds);
    }

    public PostResDTO toRes(Post p, Flags flags) {
        boolean scr = flags.scrappedIds().contains(p.getId());
        boolean lk  = flags.likedIds().contains(p.getId());
        return PostResDTO.from(p, scr, lk);
    }
}
