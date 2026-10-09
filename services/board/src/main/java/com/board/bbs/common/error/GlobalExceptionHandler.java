package com.board.bbs.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 예외를 RFC 9457 ProblemDetail로 변환하는 유일한 지점.
 *
 * <p>Spring MVC가 던지는 예외는 이미 알맞은 상태 코드를 담고 있으므로 {@link ResponseEntityExceptionHandler}가 처리하도록 두고, 이
 * 클래스는 확장 필드만 채운다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  ProblemDetail handleBusiness(BusinessException e, HttpServletRequest request) {
    String detail =
        Objects.requireNonNullElse(e.getMessage(), e.getErrorCode().getDefaultMessage());
    return ProblemDetails.of(e.getErrorCode(), detail, request.getRequestURI());
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
    return ProblemDetails.of(ErrorCode.ACCESS_DENIED, request.getRequestURI());
  }

  /** 예상하지 못한 예외만 스택트레이스를 남긴다. 응답에는 내부 정보를 노출하지 않는다. */
  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception e, HttpServletRequest request) {
    log.error("처리되지 않은 예외가 발생했습니다. uri={}", request.getRequestURI(), e);
    return ProblemDetails.of(ErrorCode.INTERNAL_ERROR, request.getRequestURI());
  }

  @Override
  @Nullable
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
                    GlobalExceptionHandler::messageOf,
                    (first, second) -> first));

    ProblemDetail problem = ProblemDetails.of(ErrorCode.INVALID_REQUEST, requestUri(request));
    problem.setProperty("errors", fieldErrors);

    return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
  }

  /** 경로 변수처럼 본문이 아닌 인자의 제약 위반도 본문 검증 실패와 같은 형태로 돌려준다. */
  @Override
  @Nullable
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    Map<String, String> errors = new LinkedHashMap<>();
    for (ParameterValidationResult result : ex.getParameterValidationResults()) {
      if (result instanceof ParameterErrors bodyErrors) {
        bodyErrors
            .getFieldErrors()
            .forEach(error -> errors.putIfAbsent(error.getField(), messageOf(error)));
      } else {
        String name =
            Objects.requireNonNullElse(result.getMethodParameter().getParameterName(), "unknown");
        result.getResolvableErrors().forEach(error -> errors.putIfAbsent(name, messageOf(error)));
      }
    }

    ProblemDetail problem = ProblemDetails.of(ErrorCode.INVALID_REQUEST, requestUri(request));
    problem.setProperty("errors", errors);

    return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
  }

  /** 프레임워크가 만든 응답 본문에도 동일한 확장 필드를 채워 응답 형태를 일관되게 유지한다. */
  @Override
  @Nullable
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      @Nullable Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {

    if (body instanceof ProblemDetail problem) {
      if (problem.getInstance() == null) {
        problem.setInstance(ProblemDetails.instanceOf(requestUri(request)));
      }
      if (problem.getProperties() == null
          || !problem.getProperties().containsKey(ProblemDetails.CODE_PROPERTY)) {
        problem.setProperty(
            ProblemDetails.CODE_PROPERTY, HttpStatus.valueOf(statusCode.value()).name());
      }
    }
    return super.handleExceptionInternal(ex, body, headers, statusCode, request);
  }

  private static String messageOf(MessageSourceResolvable error) {
    return Objects.requireNonNullElse(error.getDefaultMessage(), "올바르지 않은 값");
  }

  private String requestUri(WebRequest request) {
    if (request instanceof ServletWebRequest servletWebRequest) {
      return servletWebRequest.getRequest().getRequestURI();
    }
    return request.getDescription(false);
  }
}
