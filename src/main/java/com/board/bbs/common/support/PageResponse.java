package com.board.bbs.common.support;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 페이징 응답의 공통 형태.
 *
 * <p>Spring Data의 Page를 그대로 직렬화하면 내부 구조가 API 계약이 되어 버린다. 이 record가 그 경계를 끊는다.
 *
 * @param <T> 응답 요소 타입
 */
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {

  /**
   * Spring Data의 Page를 응답 형태로 변환한다.
   *
   * @param page 변환할 페이지
   * @param <T> 응답 요소 타입
   * @return 페이징 응답
   */
  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isLast());
  }
}
