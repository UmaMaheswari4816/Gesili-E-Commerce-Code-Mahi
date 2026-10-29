package com.gecili.ecommerce.auth.web;

import com.gecili.ecommerce.common.constant.HttpHeaders;
import com.gecili.ecommerce.common.error.ErrorType;
import com.gecili.ecommerce.common.error.ProblemDetails;
import com.gecili.ecommerce.common.error.ProblemDetails.FieldViolation;
import com.gecili.ecommerce.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions into RFC 7807 {@link ProblemDetail} responses using the shared
 * {@link ProblemDetails} factory, so this service returns the same error contract as every
 * other service.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex, HttpServletRequest req) {
        ErrorType type = ex.getErrorType();
        ProblemDetail pd = ProblemDetails.of(type, ex.getMessage(), correlationId(req));
        return ResponseEntity.status(type.status()).body(pd);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetails.of(ErrorType.UNAUTHORIZED, ex.getMessage(), correlationId(req));
        return ResponseEntity.status(ErrorType.UNAUTHORIZED.status()).body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<FieldViolation> violations =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(fe -> new FieldViolation(fe.getField(), fe.getDefaultMessage(), fe.getRejectedValue()))
                        .toList();
        ProblemDetail pd =
                ProblemDetails.validation("Request validation failed", correlationId(req), violations);
        return ResponseEntity.status(ErrorType.VALIDATION_ERROR.status()).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest req) {
        ProblemDetail pd =
                ProblemDetails.of(ErrorType.INTERNAL_ERROR, "Unexpected error", correlationId(req));
        return ResponseEntity.status(ErrorType.INTERNAL_ERROR.status()).body(pd);
    }

    private static String correlationId(HttpServletRequest req) {
        return req.getHeader(HttpHeaders.CORRELATION_ID);
    }
}
