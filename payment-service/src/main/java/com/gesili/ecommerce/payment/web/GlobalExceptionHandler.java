package com.gesili.ecommerce.payment.web;

import com.gesili.ecommerce.common.constant.HttpHeaders;
import com.gesili.ecommerce.common.error.ErrorType;
import com.gesili.ecommerce.common.error.ProblemDetails;
import com.gesili.ecommerce.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex, HttpServletRequest req) {
        ErrorType type = ex.getErrorType();
        return ResponseEntity.status(type.status())
                .body(ProblemDetails.of(type, ex.getMessage(), cid(req)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(
            IllegalArgumentException ex, HttpServletRequest req) {
        return ResponseEntity.status(ErrorType.VALIDATION_ERROR.status())
                .body(ProblemDetails.of(ErrorType.VALIDATION_ERROR, ex.getMessage(), cid(req)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(ErrorType.INTERNAL_ERROR.status())
                .body(ProblemDetails.of(ErrorType.INTERNAL_ERROR, "Unexpected error", cid(req)));
    }

    private static String cid(HttpServletRequest req) {
        return req.getHeader(HttpHeaders.CORRELATION_ID);
    }
}
