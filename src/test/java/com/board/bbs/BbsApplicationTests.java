package com.board.bbs;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("인프라 구성 전까지 비활성화한다. Task 2에서 Testcontainers 기반으로 복구한다.")
class BbsApplicationTests {

  @Test
  void contextLoads() {}
}
