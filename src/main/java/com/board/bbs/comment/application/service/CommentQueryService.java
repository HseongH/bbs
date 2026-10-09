package com.board.bbs.comment.application.service;

import com.board.bbs.comment.application.CommentThread;
import com.board.bbs.comment.application.port.out.CommentRepository;
import com.board.bbs.comment.domain.Comment;
import com.board.bbs.comment.domain.CommentId;
import com.board.bbs.post.application.service.PostQueryService;
import com.board.bbs.post.domain.PostId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 댓글 읽기 유스케이스 구현. */
@Service
public class CommentQueryService {

  private final CommentRepository commentRepository;
  private final PostQueryService postQueryService;

  CommentQueryService(CommentRepository commentRepository, PostQueryService postQueryService) {
    this.commentRepository = commentRepository;
    this.postQueryService = postQueryService;
  }

  /**
   * 게시글의 댓글 목록을 원댓글 단위로 조회한다. 페이지 크기와 전체 건수는 원댓글 수 기준이다.
   *
   * @param postId 게시글 식별자
   * @param pageable 페이지 번호와 크기. 정렬은 무시한다
   * @return 원댓글과 그 대댓글 묶음의 페이지
   * @throws com.board.bbs.common.error.BusinessException 게시글이 없거나 삭제되었으면 POST_NOT_FOUND
   */
  @Transactional(readOnly = true)
  public Page<CommentThread> list(PostId postId, Pageable pageable) {
    postQueryService.getById(postId);

    Page<Comment> roots = commentRepository.listRoots(postId, pageable);
    List<CommentId> rootIds = roots.getContent().stream().map(CommentQueryService::idOf).toList();
    Map<CommentId, List<Comment>> repliesByRoot =
        commentRepository.listRepliesOf(rootIds).stream()
            .collect(
                Collectors.groupingBy(
                    reply -> Objects.requireNonNull(reply.getParentId(), "대댓글은 부모를 가진다.")));

    // 두 조회 사이에 마지막 대댓글이 삭제되면 삭제된 원댓글만 남는다. 그런 묶음은 내보내지 않는다.
    List<CommentThread> threads =
        roots.getContent().stream()
            .map(root -> new CommentThread(root, repliesByRoot.getOrDefault(idOf(root), List.of())))
            .filter(thread -> !thread.root().isDeleted() || !thread.replies().isEmpty())
            .toList();
    return new PageImpl<>(threads, roots.getPageable(), roots.getTotalElements());
  }

  private static CommentId idOf(Comment comment) {
    return Objects.requireNonNull(comment.getId(), "저장된 댓글은 식별자를 가진다.");
  }
}
