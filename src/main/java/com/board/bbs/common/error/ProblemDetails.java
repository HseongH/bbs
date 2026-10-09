package com.board.bbs.common.error;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** 오류 코드로 응답 본문을 만든다. 예외 처리기와 보안 필터가 같은 형식을 쓰도록 한 곳에 둔다. */
public final class ProblemDetails {

  /** 오류 코드를 담는 확장 필드 이름. */
  public static final String CODE_PROPERTY = "code";

  private static final char[] HEX = "0123456789ABCDEF".toCharArray();

  private ProblemDetails() {}

  /**
   * 오류 코드에 맞는 응답 본문을 만든다.
   *
   * @param errorCode 오류 코드
   * @param detail 사용자에게 보여 줄 설명
   * @param requestUri 요청 경로
   * @return 응답 본문
   */
  public static ProblemDetail of(ErrorCode errorCode, String detail, String requestUri) {
    HttpStatus status = errorCode.getStatus();
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(status.getReasonPhrase());
    problem.setType(URI.create("urn:bbs:error:" + errorCode.name().toLowerCase(Locale.ROOT)));
    problem.setInstance(instanceOf(requestUri));
    problem.setProperty(CODE_PROPERTY, errorCode.name());
    return problem;
  }

  /**
   * 오류 코드의 기본 설명으로 응답 본문을 만든다.
   *
   * @param errorCode 오류 코드
   * @param requestUri 요청 경로
   * @return 응답 본문
   */
  public static ProblemDetail of(ErrorCode errorCode, String requestUri) {
    return of(errorCode, errorCode.getDefaultMessage(), requestUri);
  }

  /**
   * 요청 경로를 응답의 {@code instance}로 쓸 수 있는 URI로 바꾼다.
   *
   * <p>경로에서 허용되지 않는 바이트만 퍼센트 인코딩한다. 이미 인코딩된 {@code %XX}는 그대로 두어 이중으로 인코딩하지 않는다. 결과는 항상 올바른 URI이므로,
   * 경로에 어떤 문자가 있어도 오류 응답을 만드는 데 실패하지 않는다.
   */
  static URI instanceOf(String requestUri) {
    byte[] bytes = requestUri.getBytes(StandardCharsets.UTF_8);
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

  /** RFC 3986의 경로 문자(pchar)와 구분자 {@code /}. */
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
