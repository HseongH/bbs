package com.board.bbs.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 인증 서비스의 진입점. */
@SpringBootApplication
public class AuthApplication {

  /**
   * 애플리케이션을 시작한다.
   *
   * @param args 실행 인자
   */
  public static void main(String[] args) {
    SpringApplication.run(AuthApplication.class, args);
  }
}
