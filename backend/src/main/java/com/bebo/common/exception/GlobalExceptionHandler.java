package com.bebo.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.DateTimeException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /*
   * Xử lý lỗi từ @Valid trên request DTO.
   *
   * Ví dụ:
   * @NotBlank
   * @Email
   * @Size
   * @Min
   * @Max
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();

    exception
        .getBindingResult()
        .getFieldErrors()
        .forEach(
            error -> {
              String message =
                  error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage();

              fieldErrors.putIfAbsent(error.getField(), message);
            });

    return buildResponse(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_ERROR",
        "Request validation failed",
        request,
        fieldErrors);
  }

  /*
   * Xử lý validation trên path variable,
   * request parameter hoặc method-level validation.
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleConstraintViolation(
      ConstraintViolationException exception, HttpServletRequest request) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();

    exception
        .getConstraintViolations()
        .forEach(
            violation ->
                fieldErrors.put(violation.getPropertyPath().toString(), violation.getMessage()));

    return buildResponse(
        HttpStatus.BAD_REQUEST,
        "CONSTRAINT_VIOLATION",
        "Request validation failed",
        request,
        fieldErrors);
  }

  /*
   * Xử lý JSON sai cú pháp hoặc sai định dạng.
   *
   * Ví dụ:
   * startDate: "not-a-date"
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadableMessage(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    return buildResponse(
        HttpStatus.BAD_REQUEST,
        "INVALID_REQUEST_BODY",
        "Request body is malformed or contains an invalid value",
        request);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiError> handleBadRequest(
      BadRequestException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage(), request);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiError> handleConflict(
      ConflictException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage(), request);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(
      NotFoundException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), request);
  }

  /*
   * BadCredentialsException được ném trong
   * AuthService.login().
   */
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiError> handleBadCredentials(
      BadCredentialsException exception, HttpServletRequest request) {
    return buildResponse(
        HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect", request);
  }

  /*
   * Xử lý giá trị Java không hợp lệ,
   * đặc biệt là ZoneId.of(...).
   */
  @ExceptionHandler({IllegalArgumentException.class, DateTimeException.class})
  public ResponseEntity<ApiError> handleInvalidArgument(
      RuntimeException exception, HttpServletRequest request) {
    return buildResponse(
        HttpStatus.BAD_REQUEST,
        "INVALID_ARGUMENT",
        exception.getMessage() == null ? "An argument is invalid" : exception.getMessage(),
        request);
  }

  /*
   * Database vẫn phải có unique constraint.
   * Handler này xử lý race condition khi hai
   * request vượt qua kiểm tra exists cùng lúc.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> handleDataIntegrityViolation(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    log.warn("Database integrity violation at {}", request.getRequestURI());

    return buildResponse(
        HttpStatus.CONFLICT,
        "DATA_CONFLICT",
        "The submitted data conflicts with an existing record",
        request);
  }

  /*
   * Không trả stack trace hoặc nội dung exception
   * nội bộ về client.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpectedException(
      Exception exception, HttpServletRequest request) {
    log.error("Unexpected error at {}", request.getRequestURI(), exception);

    return buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_SERVER_ERROR",
        "An unexpected error occurred",
        request);
  }

  private ResponseEntity<ApiError> buildResponse(
      HttpStatus status, String code, String message, HttpServletRequest request) {
    return buildResponse(status, code, message, request, Map.of());
  }

  private ResponseEntity<ApiError> buildResponse(
      HttpStatus status,
      String code,
      String message,
      HttpServletRequest request,
      Map<String, String> fieldErrors) {
    ApiError error = ApiError.create(status, code, message, request.getRequestURI(), fieldErrors);

    return ResponseEntity.status(status).body(error);
  }
}
