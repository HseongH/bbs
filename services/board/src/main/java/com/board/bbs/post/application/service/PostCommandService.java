package com.board.bbs.post.application.service;

import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.application.port.out.PostRepository;
import com.board.bbs.post.domain.Content;
import com.board.bbs.post.domain.Post;
import com.board.bbs.post.domain.PostDeleted;
import com.board.bbs.post.domain.PostId;
import com.board.bbs.post.domain.Title;
import java.time.Instant;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 게시글 쓰기 유스케이스 구현. 권한 판단은 도메인이 하고 여기서는 조율만 한다. */
@Service
public class PostCommandService {

  private final PostRepository postRepository;
  private final ApplicationEventPublisher eventPublisher;

  PostCommandService(PostRepository postRepository, ApplicationEventPublisher eventPublisher) {
    this.postRepository = postRepository;
    this.eventPublisher = eventPublisher;
  }

  /**
   * 게시글을 작성한다.
   *
   * @param author 작성자 식별자
   * @param title 제목
   * @param content 본문
   * @return 작성된 게시글의 식별자
   */
  @Transactional
  public PostId create(MemberId author, String title, String content) {
    Post saved = postRepository.save(Post.write(new Title(title), new Content(content), author));
    return Objects.requireNonNull(saved.getId(), "저장된 게시글은 식별자를 가진다.");
  }

  /**
   * 게시글을 수정한다.
   *
   * @param id 게시글 식별자
   * @param requester 요청한 회원 식별자
   * @param title 새 제목
   * @param content 새 본문
   */
  @Transactional
  public void update(PostId id, MemberId requester, String title, String content) {
    Post post = postRepository.load(id);
    post.updateBy(requester, new Title(title), new Content(content));
    postRepository.save(post);
  }

  /**
   * 게시글을 삭제한다.
   *
   * @param id 게시글 식별자
   * @param requester 요청한 회원 식별자
   * @param admin 관리자 여부
   */
  @Transactional
  public void delete(PostId id, MemberId requester, boolean admin) {
    Post post = postRepository.load(id);
    Instant now = Instant.now();
    post.deleteBy(requester, admin, now);
    postRepository.save(post);
    eventPublisher.publishEvent(new PostDeleted(id, now));
  }
}
