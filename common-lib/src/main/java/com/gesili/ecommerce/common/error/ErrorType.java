package com.gesili.ecommerce.common.error;

import org.springframework.http.HttpStatus;

/**
 * Catalogue of application error types used to build RFC 7807 {@code ProblemDetail}
 * responses consistently across services.
 *
 * <p>Each entry carries a stable machine-readable {@code code}, the {@code type} URI
 * reference (relative; a service may resolve it against its base), a default
 * {@code title} and the {@link HttpStatus} to return. Keeping this in common-lib means
 * every service surfaces the same error contract to clients.
 */
public enum ErrorType {
    VALIDATION_ERROR("GESILI-400-001", "/errors/validation", "Validation failed", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("GESILI-401-001", "/errors/unauthorized", "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("GESILI-403-001", "/errors/forbidden", "Forbidden", HttpStatus.FORBIDDEN),
    NOT_FOUND("GESILI-404-001", "/errors/not-found", "Resource not found", HttpStatus.NOT_FOUND),
    CONFLICT("GESILI-409-001", "/errors/conflict", "Conflict", HttpStatus.CONFLICT),
    IDEMPOTENCY_CONFLICT(
            "GESILI-409-002", "/errors/idempotency", "Idempotency key conflict", HttpStatus.CONFLICT),
    INSUFFICIENT_STOCK(
            "GESILI-409-003", "/errors/insufficient-stock", "Insufficient stock", HttpStatus.CONFLICT),
    PAYMENT_ERROR("GESILI-402-001", "/errors/payment", "Payment error", HttpStatus.PAYMENT_REQUIRED),
    DOWNSTREAM_UNAVAILABLE(
            "GESILI-503-001",
            "/errors/downstream-unavailable",
            "A downstream service is unavailable",
            HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR(
            "GESILI-500-001", "/errors/internal", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String type;
    private final String title;
    private final HttpStatus status;

    ErrorType(String code, String type, String title, HttpStatus status) {
        this.code = code;
        this.type = type;
        this.title = title;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public String type() {
        return type;
    }

    public String title() {
        return title;
    }

    public HttpStatus status() {
        return status;
    }
}
