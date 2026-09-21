package com.board.bbs.post.adapter.out.persistence;

import com.board.bbs.member.adapter.out.persistence.QMemberJpaEntity;
import com.board.bbs.post.application.PostSearchCondition;
import com.board.bbs.post.application.PostSummary;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

/** 목록 조회 전용 질의. 도메인 객체를 거치지 않고 필요한 컬럼만 프로젝션한다. */
@Repository
@RequiredArgsConstructor
public class PostQueryRepository {

  private static final QPostJpaEntity POST = QPostJpaEntity.postJpaEntity;
  private static final QMemberJpaEntity MEMBER = QMemberJpaEntity.memberJpaEntity;

  private final JPAQueryFactory queryFactory;

  /**
   * 조건에 맞는 게시글 요약을 페이지 단위로 조회한다.
   *
   * @param condition 검색 조건
   * @param pageable 페이지 정보
   * @return 게시글 요약 페이지
   */
  public Page<PostSummary> search(PostSearchCondition condition, Pageable pageable) {
    BooleanBuilder where = new BooleanBuilder(POST.deletedAt.isNull());

    String keyword = condition.keyword();
    if (keyword != null) {
      where.and(
          POST.title.containsIgnoreCase(keyword).or(POST.content.containsIgnoreCase(keyword)));
    }

    Long authorId = condition.authorId();
    if (authorId != null) {
      where.and(POST.authorId.eq(authorId));
    }

    List<PostSummary> content =
        queryFactory
            .select(
                Projections.constructor(
                    PostSummary.class,
                    POST.id,
                    POST.title,
                    POST.authorId,
                    MEMBER.nickname,
                    POST.viewCount,
                    POST.likeCount,
                    POST.createdAt))
            .from(POST)
            .join(MEMBER)
            .on(MEMBER.id.eq(POST.authorId))
            .where(where)
            // 작성 시각이 같은 행들의 순서를 식별자로 고정해야 페이지 경계에서 행이 겹치거나 빠지지 않는다.
            .orderBy(POST.createdAt.desc(), POST.id.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    return PageableExecutionUtils.getPage(
        content,
        pageable,
        () -> {
          Long total = queryFactory.select(POST.count()).from(POST).where(where).fetchOne();
          return total == null ? 0L : total;
        });
  }
}
