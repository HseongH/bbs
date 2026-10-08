package com.board.bbs.post.application.service;

import com.board.bbs.post.application.PostSearchCondition;
import com.board.bbs.post.application.PostSummary;
import com.board.bbs.post.application.port.out.PostRepository;
import com.board.bbs.post.application.port.out.ViewDeduplicationPort;
import com.board.bbs.post.domain.Post;
import com.board.bbs.post.domain.PostId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 게시글 읽기 유스케이스 구현. */
@Service
public class PostQueryService {

  private final PostRepository postRepository;
  private final ViewDeduplicationPort viewDeduplicationPort;

  PostQueryService(PostRepository postRepository, ViewDeduplicationPort viewDeduplicationPort) {
    this.postRepository = postRepository;
    this.viewDeduplicationPort = viewDeduplicationPort;
  }

  /**
   * 게시글을 조회한다.
   *
   * @param id 게시글 식별자
   * @return 게시글
   */
  @Transactional(readOnly = true)
  public Post getById(PostId id) {
    return postRepository.load(id);
  }

  /**
   * 조건에 맞는 게시글 목록을 조회한다.
   *
   * @param condition 검색 조건
   * @param pageable 페이지 정보
   * @return 게시글 요약 페이지
   */
  @Transactional(readOnly = true)
  public Page<PostSummary> search(PostSearchCondition condition, Pageable pageable) {
    return postRepository.search(condition, pageable);
  }

  /**
   * 게시글을 조회하고 최초 조회인 경우 조회수를 올린다.
   *
   * @param id 게시글 식별자
   * @param viewerKey 조회자 식별 키
   * @return 게시글
   */
  @Transactional
  public Post getAndCountView(PostId id, String viewerKey) {
    Post post = postRepository.load(id);
    if (viewDeduplicationPort.markViewed(id, viewerKey)) {
      postRepository.increaseViewCount(id);
    }
    return post;
  }
}
