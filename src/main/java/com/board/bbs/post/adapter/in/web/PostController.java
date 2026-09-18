package com.board.bbs.post.adapter.in.web;

import com.board.bbs.common.security.CurrentMember;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.adapter.in.web.dto.CreatePostRequest;
import com.board.bbs.post.adapter.in.web.dto.PostResponse;
import com.board.bbs.post.adapter.in.web.dto.UpdatePostRequest;
import com.board.bbs.post.application.port.in.CreatePostUseCase;
import com.board.bbs.post.application.port.in.DeletePostUseCase;
import com.board.bbs.post.application.port.in.GetPostUseCase;
import com.board.bbs.post.application.port.in.UpdatePostUseCase;
import com.board.bbs.post.domain.PostId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
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

/** 게시글 API. */
@Tag(name = "게시글")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

  private static final String ADMIN_ROLE = "ROLE_ADMIN";

  private final CreatePostUseCase createPostUseCase;
  private final GetPostUseCase getPostUseCase;
  private final UpdatePostUseCase updatePostUseCase;
  private final DeletePostUseCase deletePostUseCase;

  /**
   * 게시글을 작성한다.
   *
   * @param author 현재 로그인 회원 식별자
   * @param request 작성 요청
   * @return 생성된 게시글의 위치를 담은 201 응답
   */
  @Operation(summary = "게시글 작성")
  @PostMapping
  public ResponseEntity<Void> create(
      @CurrentMember MemberId author, @Valid @RequestBody CreatePostRequest request) {

    PostId postId = createPostUseCase.create(author, request.title(), request.content());
    return ResponseEntity.created(URI.create("/api/posts/" + postId.value())).build();
  }

  /**
   * 게시글을 조회한다.
   *
   * @param id 게시글 식별자
   * @return 게시글 상세
   */
  @Operation(summary = "게시글 단건 조회")
  @GetMapping("/{id}")
  public PostResponse get(@PathVariable Long id) {
    return PostResponse.from(getPostUseCase.getById(new PostId(id)));
  }

  /**
   * 게시글을 수정한다.
   *
   * @param id 게시글 식별자
   * @param requester 현재 로그인 회원 식별자
   * @param request 수정 요청
   */
  @Operation(summary = "게시글 수정")
  @PatchMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void update(
      @PathVariable Long id,
      @CurrentMember MemberId requester,
      @Valid @RequestBody UpdatePostRequest request) {

    updatePostUseCase.update(new PostId(id), requester, request.title(), request.content());
  }

  /**
   * 게시글을 삭제한다.
   *
   * @param id 게시글 식별자
   * @param requester 현재 로그인 회원 식별자
   * @param authentication 현재 인증 정보
   */
  @Operation(summary = "게시글 삭제")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @PathVariable Long id, @CurrentMember MemberId requester, Authentication authentication) {

    boolean admin =
        authentication.getAuthorities().stream()
            .anyMatch(authority -> ADMIN_ROLE.equals(authority.getAuthority()));

    deletePostUseCase.delete(new PostId(id), requester, admin);
  }
}
