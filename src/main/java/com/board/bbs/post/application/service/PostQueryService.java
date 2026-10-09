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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
   * 게시글이 삭제되지 않았는지 확인하고, 호출한 트랜잭션이 끝날 때까지 삭제되지 않도록 잠근다.
   *
   * <p>게시글에 딸린 데이터를 만드는 다른 기능이 쓴다. 확인과 저장 사이에 게시글이 삭제되면, 삭제가 함께 지우는 데이터에서 새로 만든 데이터가 빠지기 때문이다. 잠금은
   * 호출한 트랜잭션이 끝나야 풀리므로 트랜잭션 안에서만 부를 수 있다.
   *
   * @param id 게시글 식별자
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void lockAlive(PostId id) {
    postRepository.lockAlive(id);
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
      forgetViewOnRollback(id, viewerKey);
      postRepository.increaseViewCount(id);
    }
    return post;
  }

  /** 조회 기록은 Redis에 있어 트랜잭션과 함께 롤백되지 않는다. 기록을 남겨 두면 그 조회자는 기록이 만료될 때까지 집계되지 않으므로, 롤백되면 기록을 지운다. */
  private void forgetViewOnRollback(PostId id, String viewerKey) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status == STATUS_ROLLED_BACK) {
              viewDeduplicationPort.unmarkViewed(id, viewerKey);
            }
          }
        });
  }
}
