package com.board.bbs.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 예외를 RFC 9457 ProblemDetail로 변환하는 유일한 지점.
 *
 * <p>Spring MVC가 던지는 예외는 이미 알맞은 상태 코드를 담고 있으므로 {@link ResponseEntityExceptionHandler}가 처리하도록 두고, 이
 * 클래스는 확장 필드만 채운다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final String CODE_PROPERTY = "code";

  @ExceptionHandler(BusinessException.class)
  ProblemDetail handleBusiness(BusinessException e, HttpServletRequest request) {
    return toProblemDetail(e.getErrorCode(), e.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
    return toProblemDetail(
        ErrorCode.ACCESS_DENIED,
        ErrorCode.ACCESS_DENIED.getDefaultMessage(),
        request.getRequestURI());
  }

  /** 예상하지 못한 예외만 스택트레이스를 남긴다. 응답에는 내부 정보를 노출하지 않는다. */
  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception e, HttpServletRequest request) {
    log.error("처리되지 않은 예외가 발생했습니다. uri={}", request.getRequestURI(), e);
    return toProblemDetail(
        ErrorCode.INTERNAL_ERROR,
        ErrorCode.INTERNAL_ERROR.getDefaultMessage(),
        request.getRequestURI());
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    Map<String, String> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    error ->
                        error.getDefaultMessage() == null ? "올바르지 않은 값" : error.getDefaultMessage(),
                    (first, second) -> first));

    ProblemDetail problem =
        toProblemDetail(
            ErrorCode.INVALID_REQUEST,
            ErrorCode.INVALID_REQUEST.getDefaultMessage(),
            requestUri(request));
    problem.setProperty("errors", fieldErrors);

    return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
  }

  /** 프레임워크가 만든 응답 본문에도 동일한 확장 필드를 채워 응답 형태를 일관되게 유지한다. */
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      @Nullable Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {

    if (body instanceof ProblemDetail problem) {
      if (problem.getInstance() == null) {
        problem.setInstance(URI.create(requestUri(request)));
      }
      if (problem.getProperties() == null || !problem.getProperties().containsKey(CODE_PROPERTY)) {
        problem.setProperty(CODE_PROPERTY, HttpStatus.valueOf(statusCode.value()).name());
      }
    }
    return super.handleExceptionInternal(ex, body, headers, statusCode, request);
  }

  private ProblemDetail toProblemDetail(ErrorCode errorCode, String detail, String requestUri) {
    HttpStatus status = errorCode.getStatus();
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(status.getReasonPhrase());
    problem.setType(URI.create("urn:bbs:error:" + errorCode.name().toLowerCase()));
    problem.setInstance(URI.create(requestUri));
    problem.setProperty(CODE_PROPERTY, errorCode.name());
    return problem;
  }

  private String requestUri(WebRequest request) {
    if (request instanceof ServletWebRequest servletWebRequest) {
      return servletWebRequest.getRequest().getRequestURI();
    }
    return request.getDescription(false);
  }
}
