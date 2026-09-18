package com.board.bbs.common.security;

import com.board.bbs.common.error.BusinessException;
import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.member.application.port.out.LoadMemberPort;
import com.board.bbs.member.domain.Member;
import com.board.bbs.member.domain.MemberId;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** {@link CurrentMember}가 붙은 파라미터를 현재 로그인 회원의 식별자로 채운다. */
@Component
@RequiredArgsConstructor
public class CurrentMemberArgumentResolver implements HandlerMethodArgumentResolver {

  private final LoadMemberPort loadMemberPort;

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(CurrentMember.class)
        && MemberId.class.equals(parameter.getParameterType());
  }

  @Override
  @Nullable public Object resolveArgument(
      MethodParameter parameter,
      @Nullable ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      @Nullable WebDataBinderFactory binderFactory) {

    CurrentMember annotation = parameter.getParameterAnnotation(CurrentMember.class);
    boolean required = annotation == null || annotation.required();

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Object principal = authentication == null ? null : authentication.getPrincipal();

    if (principal instanceof OidcUser oidcUser) {
      String subject = Objects.requireNonNull(oidcUser.getSubject(), "OIDC 토큰에는 sub가 반드시 있다.");
      return loadMemberPort
          .findBySubject(subject)
          .map(Member::getId)
          .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    if (required) {
      throw new BusinessException(ErrorCode.UNAUTHENTICATED);
    }
    return null;
  }
}
