package com.securechat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.CannotCreateTransactionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the entire application.
 * Centralizes error handling logic and provides consistent API error responses.
 * Annotated with @RestControllerAdvice to apply to all @RestController classes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  // Logger for tracking exceptions (useful for debugging and monitoring)
  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // Handles custom validation exceptions (business logic validation failures)
  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<Map<String, String>> handleValidationException(ValidationException ex) {
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("error", ex.getMessage()); // Returns the validation error message
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse); // HTTP 400
  }

  // Handles attempts to create duplicate resources (e.g., duplicate
  // username/email)
  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<Map<String, String>> handleDuplicateResourceException(DuplicateResourceException ex) {
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("error", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse); // HTTP 409
  }

  // Handles user not found scenarios (e.g., invalid user ID in requests)
  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleUserNotFoundException(UserNotFoundException ex) {
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("error", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse); // HTTP 404
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<String> handleAccessDenied(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ex.getMessage());
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Void> handleNotFound(ResourceNotFoundException ex) {
    return ResponseEntity.notFound().build();
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<String> handleUnauthorized(UnauthorizedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ex.getMessage());
  }

  // Handles authentication failures (wrong username/password)
  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<Map<String, String>> handleInvalidCredentialsException(InvalidCredentialsException ex) {
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("error", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse); // HTTP 401
  }

  // Handles Spring validation failures (@Valid annotation failures)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    // Extracts field-level validation errors (e.g., @NotBlank, @Size violations)
    ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors); // HTTP 400
  }

  // Handles malformed JSON requests (e.g., invalid JSON syntax)
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, String>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
    // Typical causes: malformed JSON, wrong Content-Type, unexpected JSON structure
    logger.warn("Request body could not be read/parsed", ex); // Log at WARN level
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("error", "Malformed JSON request");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse); // HTTP 400
  }

  // Handles missing required query parameters (@RequestParam missing)
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<Map<String, String>> handleMissingServletRequestParameterException(
      MissingServletRequestParameterException ex) {
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("error", "Missing required parameter: " + ex.getParameterName());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse); // HTTP 400
  }

  // Handles missing file upload parts (@RequestPart missing)
  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<Map<String, String>> handleMissingServletRequestPartException(
      MissingServletRequestPartException ex) {
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("error", "Missing required request part: " + ex.getRequestPartName());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse); // HTTP 400
  }

  // Handles illegal argument exceptions (e.g., invalid method arguments)
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ex.getMessage());
  }

  // Handles path parameter type mismatches (e.g., expecting UUID but got string)
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<Map<String, String>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("error",
        "Invalid path parameter: " + ex.getName() + " should be " + ex.getRequiredType().getSimpleName());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse); // HTTP 400
  }

  // Catch-all for RuntimeExceptions (unexpected runtime errors)
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
    logger.error("Unhandled runtime exception", ex); // Log at ERROR level
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("error", ex.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse); // HTTP 500
  }

  /**
   * Handle database connection failures and data access exceptions
   * Implements NFE-AVAIL-047: Graceful degradation with 503 Service Unavailable
   * Returns user-friendly message instead of exposing database errors
   */
  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<Map<String, String>> handleDataAccessException(DataAccessException ex) {
    logger.error("Database access error - service degraded", ex);
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("error", "Service temporarily unavailable. Please try again later.");
    errorResponse.put("status", "degraded"); // Additional status information
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse); // HTTP 503
  }

  // Handles database transaction creation failures (database may be down)
  @ExceptionHandler(CannotCreateTransactionException.class)
  public ResponseEntity<Map<String, String>> handleCannotCreateTransactionException(
      CannotCreateTransactionException ex) {
    logger.error("Cannot create database transaction - database may be unavailable", ex);
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("error", "Service temporarily unavailable due to database issues.");
    errorResponse.put("status", "degraded");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse); // HTTP 503
  }

  // Ultimate catch-all for any unhandled exceptions
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
    logger.error("Unhandled exception", ex);
    Map<String, String> errorResponse = new HashMap<>();
    errorResponse.put("error", "Internal server error"); // Generic message for security
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse); // HTTP 500
  }
}