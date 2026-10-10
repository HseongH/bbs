package com.board.bbs.member.adapter.in.web;

import com.board.bbs.common.error.BusinessException;
import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.common.security.CurrentMember;
import com.board.bbs.member.application.service.MemberService;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.token.InternalUser;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** {@link CurrentMember}가 붙은 파라미터를 내부 토큰이 가리키는 회원의 식별자로 채운다. 회원이 없으면 만든다. */
@Component
public class CurrentMemberArgumentResolver implements HandlerMethodArgumentResolver {

  private final MemberService memberService;

  CurrentMemberArgumentResolver(MemberService memberService) {
    this.memberService = memberService;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(CurrentMember.class)
        && MemberId.class.equals(parameter.getParameterType());
  }

  @Override
  @Nullable
  public Object resolveArgument(
      MethodParameter parameter,
      @Nullable ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      @Nullable WebDataBinderFactory binderFactory) {

    CurrentMember annotation = parameter.getParameterAnnotation(CurrentMember.class);
    boolean required = annotation == null || annotation.required();

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Object principal = authentication == null ? null : authentication.getPrincipal();

    // 처음 보는 사용자면 토큰의 클레임으로 회원을 만든다 (MEM-FR-002). 이미 있으면 조회만 한다.
    if (principal instanceof Jwt jwt) {
      InternalUser user = InternalUser.from(jwt);
      return memberService.provision(user.subject(), user.nickname(), user.email());
    }

    if (required) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED);
    }
    return null;
  }
}
