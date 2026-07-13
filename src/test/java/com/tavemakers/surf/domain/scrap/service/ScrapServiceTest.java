package com.tavemakers.surf.domain.scrap.service;

import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.scrap.entity.Scrap;
import com.tavemakers.surf.domain.scrap.exception.ScrapAlreadyExistsException;
import com.tavemakers.surf.domain.scrap.repository.ScrapRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * ScrapService 단위 테스트 — addScrap의 멱등(exists 체크 → saveAndFlush →
 * DataIntegrityViolationException → 409 변환) 경로와 removeScrap/removeAllByMemberId의
 * 조건부 scrapCount 증감을 Spring 컨텍스트 없이 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ScrapServiceTest {

    @Mock
    private ScrapRepository scrapRepository;

    @Mock
    private PostGetService postGetService;

    @Mock
    private MemberGetService memberGetService;

    @InjectMocks
    private ScrapService scrapService;

    private Member memberWithId(Long id) {
        Member member = Member.builder().name("회원").build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Post postWithId(Long id) {
        Post post = Post.builder().title("제목").content("내용").build();
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    @Test
    @DisplayName("addScrap: 아직 스크랩하지 않았다면 저장 후 flush하고 게시글 스크랩 수를 증가시킨다")
    void addScrap_신규스크랩이면_저장하고_스크랩수를_증가시킨다() {
        Member member = memberWithId(1L);
        Post post = postWithId(2L);
        given(memberGetService.getMember(1L)).willReturn(member);
        given(postGetService.getPost(2L)).willReturn(post);
        given(scrapRepository.existsByMemberIdAndPostId(1L, 2L)).willReturn(false);

        scrapService.addScrap(1L, 2L);

        ArgumentCaptor<Scrap> captor = ArgumentCaptor.forClass(Scrap.class);
        then(scrapRepository).should().saveAndFlush(captor.capture());
        assertThat(captor.getValue().getMember()).isEqualTo(member);
        assertThat(captor.getValue().getPost()).isEqualTo(post);
        then(postGetService).should().increaseScrapCount(2L);
    }

    @Test
    @DisplayName("addScrap: 이미 스크랩되어 있으면(순차 재클릭) 저장 없이 조기 종료하고 스크랩 수도 증가시키지 않는다")
    void addScrap_이미스크랩되어있으면_저장없이_멱등종료한다() {
        given(memberGetService.getMember(1L)).willReturn(memberWithId(1L));
        given(postGetService.getPost(2L)).willReturn(postWithId(2L));
        given(scrapRepository.existsByMemberIdAndPostId(1L, 2L)).willReturn(true);

        scrapService.addScrap(1L, 2L);

        then(scrapRepository).should(never()).saveAndFlush(any());
        then(postGetService).should(never()).increaseScrapCount(any());
    }

    @Test
    @DisplayName("addScrap: exists 체크를 통과한 뒤 저장 시점에 유니크 제약 위반(동시 race)이 발생하면 ScrapAlreadyExistsException(409)으로 변환하고 스크랩 수는 증가시키지 않는다")
    void addScrap_저장시_유니크제약위반이면_ScrapAlreadyExistsException으로_변환한다() {
        given(memberGetService.getMember(1L)).willReturn(memberWithId(1L));
        given(postGetService.getPost(2L)).willReturn(postWithId(2L));
        given(scrapRepository.existsByMemberIdAndPostId(1L, 2L)).willReturn(false);
        given(scrapRepository.saveAndFlush(any(Scrap.class)))
                .willThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> scrapService.addScrap(1L, 2L))
                .isInstanceOf(ScrapAlreadyExistsException.class);

        then(postGetService).should(never()).increaseScrapCount(any());
    }

    @Test
    @DisplayName("removeScrap: 삭제된 row가 있으면 게시글 스크랩 수를 감소시킨다")
    void removeScrap_삭제된행이있으면_스크랩수를_감소시킨다() {
        given(scrapRepository.deleteByMemberIdAndPostId(1L, 2L)).willReturn(1);

        scrapService.removeScrap(1L, 2L);

        then(postGetService).should().decreaseScrapCount(2L);
    }

    @Test
    @DisplayName("removeScrap: 삭제된 row가 없으면(스크랩하지 않은 상태) 스크랩 수를 감소시키지 않는다")
    void removeScrap_삭제된행이없으면_스크랩수를_감소시키지_않는다() {
        given(scrapRepository.deleteByMemberIdAndPostId(1L, 2L)).willReturn(0);

        scrapService.removeScrap(1L, 2L);

        then(postGetService).should(never()).decreaseScrapCount(any());
    }

    @Test
    @DisplayName("removeAllByMemberId: 회원이 스크랩한 게시글마다 삭제를 시도하고, 실제로 삭제된 게시글에 대해서만 스크랩 수를 감소시킨다")
    void removeAllByMemberId_스크랩한게시글마다_삭제하고_삭제성공한것만_스크랩수를_감소시킨다() {
        given(scrapRepository.findPostIdsByMemberId(1L)).willReturn(List.of(10L, 20L));
        given(scrapRepository.deleteByMemberIdAndPostId(1L, 10L)).willReturn(1);
        given(scrapRepository.deleteByMemberIdAndPostId(1L, 20L)).willReturn(0);

        scrapService.removeAllByMemberId(1L);

        then(scrapRepository).should().deleteByMemberIdAndPostId(1L, 10L);
        then(scrapRepository).should().deleteByMemberIdAndPostId(1L, 20L);
        then(postGetService).should().decreaseScrapCount(10L);
        then(postGetService).should(never()).decreaseScrapCount(20L);
    }
}
