package com.tavemakers.surf.domain.member.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.QTrack;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.Part;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;
import com.querydsl.core.types.dsl.Expressions;

import static com.tavemakers.surf.domain.member.entity.QMember.member;
import static com.tavemakers.surf.domain.member.entity.QTrack.track;

@Repository
@RequiredArgsConstructor
public class MemberSearchRepository {

    private final JPAQueryFactory queryFactory;

    /*
     * NOTE: SURF 규칙
     * "기수 > 이름 > 대학 > 가입일" 순으로 정렬
     * */
    public Slice<Member> searchMembers(Integer generation, Part part, String keyword, Pageable pageable) {
        BooleanBuilder builder = createSearchBuilder(generation, part, keyword);

        builder.and(member.status.eq(MemberStatus.APPROVED));

        NumberExpression<Integer> maxGeneration = getMaxGenerationExpression();
        List<Member> results = queryFactory
                .selectFrom(member)
                .distinct()
                .leftJoin(member.tracks, track)
                .where(builder)
                .orderBy(
                        maxGeneration.desc().nullsLast(),
                        member.name.asc(),
                        member.university.asc(),
                        member.createdAt.asc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = false;
        if (results.size() > pageable.getPageSize()) {
            hasNext = true;
            results.remove(results.size() - 1);
        }

        return new SliceImpl<>(results, pageable, hasNext);
    }

    public Slice<Member> searchMemberInAdminPage(Integer generation, String keyword, Pageable pageable) {
        BooleanBuilder builder = createAdminPageMemberSearchBuilder(generation, keyword);
        builder.and(member.status.eq(MemberStatus.APPROVED));

        List<Member> results = queryFactory
                .selectFrom(member)
                .distinct()
                .leftJoin(member.tracks, track)
                .where(builder)
                .orderBy(
                        member.name.asc(),
                        member.createdAt.asc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = false;
        if (results.size() > pageable.getPageSize()) {
            hasNext = true;
            results.remove(results.size() - 1);
        }

        return new SliceImpl<>(results, pageable, hasNext);
    }

    public Long countMembersByMemberStatusesAndKeyword(Integer generation, Part part, String keyword) {
        BooleanBuilder builder = createSearchBuilder(generation, part, keyword);
        builder.and(member.status.eq(MemberStatus.APPROVED));

        return queryFactory
                .select(member.countDistinct())
                .from(member)
                .leftJoin(member.tracks, track)
                .where(builder)
                .fetchOne();
    }

    private BooleanBuilder createSearchBuilder(Integer generation, Part part, String keyword) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(member.status.ne(MemberStatus.WITHDRAWN));

        if (generation != null) {
            builder.and(member.tracks.any().generation.eq(generation));
        }

        if (part != null) {
            builder.and(member.tracks.any().part.eq(part));
        }

        if (keyword != null && !keyword.isBlank()) {
            builder.and(member.name.containsIgnoreCase(keyword)
                    .or(member.university.containsIgnoreCase(keyword))
                    .or(member.graduateSchool.containsIgnoreCase(keyword)));
        }

        return builder;
    }

    private BooleanBuilder createAdminPageMemberSearchBuilder(Integer generation,String keyword) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(member.status.ne(MemberStatus.WITHDRAWN));

        if (generation != null) {
            builder.and(member.tracks.any().generation.eq(generation));
        }

        if (keyword != null && !keyword.isBlank()) {
            builder.and(member.name.containsIgnoreCase(keyword));
        }

        return builder;
    }

    private NumberExpression<Integer> getMaxGenerationExpression() {
        QTrack subTrack = new QTrack("subTrack");
        return Expressions.asNumber(
                JPAExpressions
                        .select(subTrack.generation.max())
                        .from(subTrack)
                        .where(subTrack.member.eq(member))
        );
    }

    public Slice<Member> findWaitingMembersByName(String keyword, Pageable pageable, List<MemberStatus> statuses) {

        List<Member> results = queryFactory
                .selectFrom(member)
                .where(
                        member.status.in(statuses),
                        containsName(keyword) // 이름 검색 조건
                )
                .orderBy(member.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        return checkLastPage(pageable, results);
    }

    // 동적 쿼리 조건: 이름 포함 여부
    private BooleanExpression containsName(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return member.name.contains(keyword);
    }

    // Slice 변환 로직 (무한 스크롤용)
    private <T> Slice<T> checkLastPage(Pageable pageable, List<T> results) {
        boolean hasNext = false;

        // 조회한 결과가 요청한 사이즈보다 크면 다음 페이지가 있음
        if (results.size() > pageable.getPageSize()) {
            hasNext = true;
            results.remove(pageable.getPageSize()); // +1개 조회한 것은 제거
        }

        return new SliceImpl<>(results, pageable, hasNext);
    }

    public Long countMembersByMemberStatusesAndKeyword(List<MemberStatus> statuses, String keyword) {
        if (statuses == null || statuses.isEmpty()) return 0L;

        return queryFactory
                .select(member.count())
                .from(member)
                .where(
                        member.status.in(statuses),
                        containsKeyword(keyword)
                )
                .fetchOne();
    }

    private BooleanExpression containsKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        return member.name.containsIgnoreCase(keyword);
    }

}
