package com.board.bbs.post.adapter.in.web.dto;

import com.board.bbs.post.application.PostSummary;
import java.time.Instant;

/**
 * 게시글 목록 응답. 본문은 담지 않는다.
 *
 * @param id 게시글 식별자
 * @param title 제목
 * @param authorId 작성자 식별자
 * @param authorNickname 작성자 닉네임
 * @param viewCount 조회수
 * @param likeCount 좋아요 수
 * @param createdAt 작성 시각
 */
public record PostSummaryResponse(
    Long id,
    String title,
    Long authorId,
    String authorNickname,
    long viewCount,
    long likeCount,
    Instant createdAt) {

  /**
   * 요약 모델을 응답으로 변환한다.
   *
   * @param summary 게시글 요약
   * @return 목록 응답
   */
  public static PostSummaryResponse from(PostSummary summary) {
    return new PostSummaryResponse(
        summary.id(),
        summary.title(),
        summary.authorId(),
        summary.authorNickname(),
        summary.viewCount(),
        summary.likeCount(),
        summary.createdAt());
  }
}
