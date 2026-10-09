package com.board.bbs.auth.forward;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.http.HttpMethod;

/**
 * 진입점이 ForwardAuth로 전달한 원래 요청.
 *
 * <p>auth가 판정한 경로와 board가 처리하는 경로가 달라지면 규칙을 우회할 수 있다. 그래서 해석이 둘 이상 가능한 경로(인코딩된 구분자, 상대 경로 조각, 세미콜론,
 * 이중 인코딩, 제어 문자)는 판정하지 않고 거부한다 (COM-NFR-009). 쿼리 문자열은 경로 판정에 쓰지 않으므로 검사하지 않는다.
 *
 * @param method 원래 요청의 메서드
 * @param path 퍼센트 디코딩한 원래 경로 (쿼리 제외)
 */
public record ForwardedRequest(HttpMethod method, String path) {

  static final String METHOD_HEADER = "X-Forwarded-Method";
  static final String URI_HEADER = "X-Forwarded-Uri";

  private static final Pattern FORBIDDEN_ENCODINGS =
      Pattern.compile("%(2f|5c|25|00)", Pattern.CASE_INSENSITIVE);

  /**
   * 요청 헤더에서 원래 요청을 복원한다.
   *
   * @param request 진입점이 보낸 ForwardAuth 요청
   * @return 원래 요청
   * @throws AmbiguousForwardedRequestException 헤더가 없거나 경로가 모호한 경우
   */
  public static ForwardedRequest from(HttpServletRequest request) {
    String method = request.getHeader(METHOD_HEADER);
    String uri = request.getHeader(URI_HEADER);
    if (method == null || method.isBlank() || uri == null) {
      throw new AmbiguousForwardedRequestException("원래 요청의 메서드나 경로가 없습니다.");
    }
    return new ForwardedRequest(
        HttpMethod.valueOf(method.trim().toUpperCase(Locale.ROOT)), decodedPath(rawPath(uri)));
  }

  /**
   * 원래 요청을 복원하되, 해석할 수 없으면 비어 있는 값을 돌려준다. 규칙 판정처럼 예외를 던질 수 없는 곳에서 쓴다.
   *
   * @param request 진입점이 보낸 ForwardAuth 요청
   * @return 원래 요청
   */
  public static Optional<ForwardedRequest> tryFrom(HttpServletRequest request) {
    try {
      return Optional.of(from(request));
    } catch (AmbiguousForwardedRequestException e) {
      return Optional.empty();
    }
  }

  /**
   * 오류 응답의 {@code instance}로 쓸 경로. 브라우저가 보기에 오류가 난 곳은 원래 요청이므로, ForwardAuth 요청이면 원래 경로(쿼리 제외)를 쓴다.
   *
   * @param request 요청
   * @return 경로
   */
  public static String instancePathOf(HttpServletRequest request) {
    String uri = request.getHeader(URI_HEADER);
    if (ForwardAuthController.PATH.equals(request.getRequestURI()) && uri != null) {
      return rawPath(uri);
    }
    return request.getRequestURI();
  }

  private static String rawPath(String uri) {
    int query = uri.indexOf('?');
    return query < 0 ? uri : uri.substring(0, query);
  }

  private static String decodedPath(String raw) {
    if (!raw.startsWith("/")
        || raw.contains("//")
        || raw.indexOf(';') >= 0
        || raw.indexOf('\\') >= 0
        || FORBIDDEN_ENCODINGS.matcher(raw).find()) {
      throw new AmbiguousForwardedRequestException("모호한 경로입니다.");
    }
    String decoded = percentDecode(raw);
    for (String segment : decoded.split("/", -1)) {
      if (segment.equals(".") || segment.equals("..")) {
        throw new AmbiguousForwardedRequestException("상대 경로 조각이 있습니다.");
      }
    }
    if (decoded.chars().anyMatch(c -> c < 0x20 || c == 0x7f)) {
      throw new AmbiguousForwardedRequestException("제어 문자가 있습니다.");
    }
    return decoded;
  }

  /** UTF-8로 엄격하게 디코딩한다. 잘못된 인코딩을 대체 문자로 바꾸지 않고 거부한다. */
  private static String percentDecode(String raw) {
    ByteBuffer bytes = ByteBuffer.allocate(raw.length() * 3);
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      if (c == '%') {
        if (i + 2 >= raw.length()
            || !HexFormat.isHexDigit(raw.charAt(i + 1))
            || !HexFormat.isHexDigit(raw.charAt(i + 2))) {
          throw new AmbiguousForwardedRequestException("잘못된 퍼센트 인코딩입니다.");
        }
        bytes.put((byte) HexFormat.fromHexDigits(raw, i + 1, i + 3));
        i += 2;
      } else {
        bytes.put(String.valueOf(c).getBytes(StandardCharsets.UTF_8));
      }
    }
    bytes.flip();
    try {
      return StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(bytes)
          .toString();
    } catch (CharacterCodingException e) {
      throw new AmbiguousForwardedRequestException("UTF-8이 아닌 경로입니다.");
    }
  }
}
