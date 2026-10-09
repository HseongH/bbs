package com.board.bbs.auth.error;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;

/**
 * 오류 응답을 쓴다. auth의 응답은 모두 보안 필터 단계에서 나가므로 MVC의 예외 처리기 대신 이곳에서 직접 쓴다.
 *
 * <p>형식은 board의 {@code ProblemDetails}와 같다. 브라우저는 어느 서비스가 거부했는지 구분할 수 없어야 한다 (COM-IF-003).
 */
public final class ProblemResponses {

  private static final JacksonJsonHttpMessageConverter WRITER =
      new JacksonJsonHttpMessageConverter();
  private static final char[] HEX = "0123456789ABCDEF".toCharArray();

  private ProblemResponses() {}

  /**
   * 오류 코드의 기본 설명으로 응답을 쓴다.
   *
   * @param response 응답
   * @param errorCode 오류 코드
   * @param instancePath 브라우저가 보낸 원래 요청의 경로
   * @throws IOException 응답을 쓰지 못한 경우
   */
  public static void write(
      HttpServletResponse response, AuthErrorCode errorCode, String instancePath)
      throws IOException {
    response.setStatus(errorCode.getStatus().value());
    WRITER.write(
        of(errorCode, instancePath),
        MediaType.APPLICATION_PROBLEM_JSON,
        new ServletServerHttpResponse(response));
  }

  static ProblemDetail of(AuthErrorCode errorCode, String instancePath) {
    HttpStatus status = errorCode.getStatus();
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, errorCode.getDefaultMessage());
    problem.setTitle(status.getReasonPhrase());
    problem.setType(URI.create("urn:bbs:error:" + errorCode.name().toLowerCase(Locale.ROOT)));
    problem.setInstance(instanceOf(instancePath));
    problem.setProperty("code", errorCode.name());
    return problem;
  }

  /** 경로에서 허용되지 않는 바이트만 퍼센트 인코딩한다. 이미 인코딩된 {@code %XX}는 그대로 둔다. */
  static URI instanceOf(String path) {
    byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
    StringBuilder encoded = new StringBuilder(bytes.length);
    for (int i = 0; i < bytes.length; i++) {
      int value = bytes[i] & 0xFF;
      if (isPathCharacter(value) || (value == '%' && isEscapeAt(bytes, i))) {
        encoded.append((char) value);
      } else {
        encoded.append('%').append(HEX[value >> 4]).append(HEX[value & 0x0F]);
      }
    }
    return URI.create(encoded.toString());
  }

  private static boolean isPathCharacter(int value) {
    return (value >= 'a' && value <= 'z')
        || (value >= 'A' && value <= 'Z')
        || (value >= '0' && value <= '9')
        || "-._~!$&'()*+,;=:@/".indexOf(value) >= 0;
  }

  private static boolean isEscapeAt(byte[] bytes, int index) {
    return index + 2 < bytes.length
        && HexFormat.isHexDigit(bytes[index + 1])
        && HexFormat.isHexDigit(bytes[index + 2]);
  }
}
