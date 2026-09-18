package com.board.bbs.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 예외를 RFC 9457 ProblemDetail로 변환하는 유일한 지점. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  ProblemDetail handleBusiness(BusinessException e, HttpServletRequest request) {
    return toProblemDetail(e.getErrorCode(), e.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
    Map<String, String> fieldErrors =
        e.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    FieldError::getField,
                    error ->
                        error.getDefaultMessage() == null ? "올바르지 않은 값" : error.getDefaultMessage(),
                    (first, second) -> first));

    ProblemDetail problem =
        toProblemDetail(
            ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.getDefaultMessage(), request);
    problem.setProperty("errors", fieldErrors);
    return problem;
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
    return toProblemDetail(
        ErrorCode.ACCESS_DENIED, ErrorCode.ACCESS_DENIED.getDefaultMessage(), request);
  }

  /** 예상하지 못한 예외만 스택트레이스를 남긴다. 응답에는 내부 정보를 노출하지 않는다. */
  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception e, HttpServletRequest request) {
    log.error("처리되지 않은 예외가 발생했습니다. uri={}", request.getRequestURI(), e);
    return toProblemDetail(
        ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), request);
  }

  private ProblemDetail toProblemDetail(
      ErrorCode errorCode, String detail, HttpServletRequest request) {

    HttpStatus status = errorCode.getStatus();
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(status.getReasonPhrase());
    problem.setType(URI.create("urn:bbs:error:" + errorCode.name().toLowerCase()));
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("code", errorCode.name());
    return problem;
  }
}
