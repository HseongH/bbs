package com.board.bbs.post.application.port.out;

import com.board.bbs.post.application.PostSearchCondition;
import com.board.bbs.post.application.PostSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 게시글 목록 조회 포트. */
public interface SearchPostPort {

  /**
   * 조건에 맞는 게시글 요약을 페이지 단위로 조회한다.
   *
   * @param condition 검색 조건
   * @param pageable 페이지 정보
   * @return 게시글 요약 페이지
   */
  Page<PostSummary> search(PostSearchCondition condition, Pageable pageable);
}
