package com.board.bbs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 게시판 애플리케이션의 진입점. */
@SpringBootApplication
public class BbsApplication {

  /**
   * 애플리케이션을 기동한다.
   *
   * @param args 명령행 인자
   */
  public static void main(String[] args) {
    SpringApplication.run(BbsApplication.class, args);
  }
}
