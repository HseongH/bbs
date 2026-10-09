package com.board.bbs.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 개발용 compose 파일에서 서비스의 컨테이너 이미지를 읽는다.
 *
 * <p>통합 테스트가 같은 이미지를 쓰도록 버전을 한 곳에서만 관리한다. 의존성 업데이트가 compose 파일을 올리면 테스트 이미지도 함께 바뀐다.
 */
final class ComposeImages {

  /** 빌드가 넘겨주는 compose 파일의 절대 경로. 작업 디렉터리에 기대지 않기 위해서다. */
  private static final String COMPOSE_FILE_PROPERTY = "bbs.compose-file";

  private ComposeImages() {}

  static String of(String service) {
    Path composeFile = composeFile();
    try {
      return parse(Files.readString(composeFile), service);
    } catch (IOException e) {
      throw new UncheckedIOException(composeFile + "을 읽을 수 없습니다.", e);
    }
  }

  private static Path composeFile() {
    String path = System.getProperty(COMPOSE_FILE_PROPERTY);
    if (path == null) {
      throw new IllegalStateException(
          "시스템 속성 " + COMPOSE_FILE_PROPERTY + "이 없습니다. Gradle로 테스트를 실행하세요.");
    }
    return Path.of(path);
  }

  static String parse(String compose, String service) {
    Pattern pattern =
        Pattern.compile(
            "^  " + Pattern.quote(service) + ":\\s*$(?:\\n {4}.*$)*?\\n {4}image:\\s*(\\S+)\\s*$",
            Pattern.MULTILINE);
    Matcher matcher = pattern.matcher(compose);
    if (!matcher.find()) {
      throw new IllegalStateException("compose 파일에 " + service + " 서비스의 이미지가 없습니다.");
    }
    return matcher.group(1);
  }
}
