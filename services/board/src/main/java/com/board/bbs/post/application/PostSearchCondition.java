package com.board.bbs.post.application;

import org.jspecify.annotations.Nullable;

/**
 * 게시글 검색 조건. 지정되지 않은 필드는 조건에서 제외된다.
 *
 * @param keyword 제목과 본문에서 찾을 키워드. 공백만 있는 값은 null로 정규화된다
 * @param authorId 작성자 식별자
 */
public record PostSearchCondition(@Nullable String keyword, @Nullable Long authorId) {

  /** 공백뿐인 키워드를 null로 통일해 이후 조건 판단을 단순하게 만든다. */
  public PostSearchCondition {
    keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
  }
}
