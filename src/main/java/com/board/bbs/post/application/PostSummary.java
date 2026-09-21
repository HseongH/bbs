package com.board.bbs.post.application;

import java.time.Instant;

/**
 * 목록 화면에 필요한 최소 정보. 본문은 담지 않는다.
 *
 * @param id 게시글 식별자
 * @param title 제목
 * @param authorId 작성자 식별자
 * @param authorNickname 작성자 닉네임
 * @param viewCount 조회수
 * @param likeCount 좋아요 수
 * @param createdAt 작성 시각
 */
public record PostSummary(
    Long id,
    String title,
    Long authorId,
    String authorNickname,
    long viewCount,
    long likeCount,
    Instant createdAt) {}
