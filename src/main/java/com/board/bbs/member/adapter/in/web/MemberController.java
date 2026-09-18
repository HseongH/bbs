package com.board.bbs.member.adapter.in.web;

import com.board.bbs.common.security.CurrentMember;
import com.board.bbs.member.adapter.in.web.dto.MemberResponse;
import com.board.bbs.member.application.service.MemberService;
import com.board.bbs.member.domain.MemberId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 회원 API. */
@Tag(name = "회원")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

  private final MemberService memberService;

  /**
   * 로그인한 회원의 정보를 반환한다.
   *
   * @param memberId 현재 로그인 회원 식별자
   * @return 회원 정보
   */
  @Operation(summary = "내 정보 조회")
  @GetMapping("/me")
  public MemberResponse me(@CurrentMember MemberId memberId) {
    return MemberResponse.from(memberService.getById(memberId));
  }
}
