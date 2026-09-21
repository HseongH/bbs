package com.board.bbs.comment.adapter.in.web;

import com.board.bbs.comment.adapter.in.web.dto.CommentResponse;
import com.board.bbs.comment.adapter.in.web.dto.UpdateCommentRequest;
import com.board.bbs.comment.adapter.in.web.dto.WriteCommentRequest;
import com.board.bbs.comment.application.port.in.DeleteCommentUseCase;
import com.board.bbs.comment.application.port.in.ListCommentsUseCase;
import com.board.bbs.comment.application.port.in.UpdateCommentUseCase;
import com.board.bbs.comment.application.port.in.WriteCommentUseCase;
import com.board.bbs.comment.domain.CommentId;
import com.board.bbs.common.security.CurrentMember;
import com.board.bbs.common.support.PageResponse;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.domain.PostId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 댓글 API. */
@Tag(name = "댓글")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

  private static final String ADMIN_ROLE = "ROLE_ADMIN";

  private final WriteCommentUseCase writeCommentUseCase;
  private final ListCommentsUseCase listCommentsUseCase;
  private final UpdateCommentUseCase updateCommentUseCase;
  private final DeleteCommentUseCase deleteCommentUseCase;

  /**
   * 댓글 또는 답글을 작성한다.
   *
   * @param postId 대상 게시글 식별자
   * @param author 현재 로그인 회원 식별자
   * @param request 작성 요청
   * @return 생성된 댓글의 위치를 담은 201 응답
   */
  @Operation(summary = "댓글 작성")
  @PostMapping("/posts/{postId}/comments")
  public ResponseEntity<Void> write(
      @PathVariable Long postId,
      @CurrentMember MemberId author,
      @Valid @RequestBody WriteCommentRequest request) {

    CommentId id =
        writeCommentUseCase.write(
            new PostId(postId), author, request.body(), request.parentCommentId());
    return ResponseEntity.created(URI.create("/api/comments/" + id.value())).build();
  }

  /**
   * 게시글의 댓글 목록을 조회한다.
   *
   * @param postId 게시글 식별자
   * @param pageable 페이지 정보
   * @return 댓글 페이지
   */
  @Operation(summary = "댓글 목록 조회")
  @GetMapping("/posts/{postId}/comments")
  public PageResponse<CommentResponse> list(
      @PathVariable Long postId, @ParameterObject @PageableDefault(size = 20) Pageable pageable) {

    return PageResponse.from(
        listCommentsUseCase.list(new PostId(postId), pageable).map(CommentResponse::from));
  }

  /**
   * 댓글을 수정한다.
   *
   * @param id 댓글 식별자
   * @param requester 현재 로그인 회원 식별자
   * @param request 수정 요청
   */
  @Operation(summary = "댓글 수정")
  @PatchMapping("/comments/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void update(
      @PathVariable Long id,
      @CurrentMember MemberId requester,
      @Valid @RequestBody UpdateCommentRequest request) {

    updateCommentUseCase.update(new CommentId(id), requester, request.body());
  }

  /**
   * 댓글을 삭제한다.
   *
   * @param id 댓글 식별자
   * @param requester 현재 로그인 회원 식별자
   * @param authentication 현재 인증 정보
   */
  @Operation(summary = "댓글 삭제")
  @DeleteMapping("/comments/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @PathVariable Long id, @CurrentMember MemberId requester, Authentication authentication) {

    boolean admin =
        authentication.getAuthorities().stream()
            .anyMatch(authority -> ADMIN_ROLE.equals(authority.getAuthority()));

    deleteCommentUseCase.delete(new CommentId(id), requester, admin);
  }
}
