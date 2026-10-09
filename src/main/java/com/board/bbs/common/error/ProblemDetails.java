package com.board.bbs.common.error;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** 오류 코드로 응답 본문을 만든다. 예외 처리기와 보안 필터가 같은 형식을 쓰도록 한 곳에 둔다. */
public final class ProblemDetails {

  /** 오류 코드를 담는 확장 필드 이름. */
  public static final String CODE_PROPERTY = "code";

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

  /** 요청 경로에 URI에서 허용되지 않는 문자가 있어도 오류 응답을 만드는 데 실패하지 않도록, 그런 문자는 인코딩한다. */
  static URI instanceOf(String requestUri) {
    try {
      return new URI(requestUri);
    } catch (URISyntaxException e) {
      try {
        return new URI(null, null, requestUri, null);
      } catch (URISyntaxException unrecoverable) {
        return URI.create("");
      }
    }
  }
}
