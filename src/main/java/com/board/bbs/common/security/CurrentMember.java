package com.board.bbs.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 컨트롤러 파라미터에 현재 로그인 회원의 식별자를 주입한다. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentMember {

  /**
   * 인증이 필수인지 여부.
   *
   * @return false이면 미인증 시 null을 주입한다
   */
  boolean required() default true;
}
