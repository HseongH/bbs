package com.board.bbs.token;

import java.time.Duration;
import java.util.Set;

/** 내부 토큰의 계약 값. auth와 업무 서비스가 반드시 같은 값을 써야 하므로 이곳에만 둔다. */
public final class InternalTokens {

  /** 발급자. 환경마다 주소가 달라도 검증이 깨지지 않도록 URL이 아닌 식별자를 쓴다. */
  public static final String ISSUER = "urn:bbs:auth";

  /** 대상. 업무 서비스가 하나뿐이므로 공통 값 하나를 쓴다. */
  public static final String AUDIENCE = "bbs";

  /** 진입점에서 업무 서비스로 가는 한 번의 요청에만 쓰므로 짧게 둔다. */
  public static final Duration LIFETIME = Duration.ofSeconds(60);

  public static final String NICKNAME = "nickname";
  public static final String EMAIL = "email";
  public static final String ROLES = "roles";

  /** 게시판이 정의한 역할. IdP가 붙이는 그 밖의 역할은 업무 서비스에 넘기지 않는다. */
  public static final Set<String> KNOWN_ROLES = Set.of("USER", "ADMIN");

  private InternalTokens() {}
}
