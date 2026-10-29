package com.gesili.ecommerce.common.exception;

import com.gesili.ecommerce.common.error.ErrorType;

/**
 * Base type for all domain/business exceptions across services.
 *
 * <p>Carries an {@link ErrorType} so a shared exception handler can map it to the correct
 * RFC 7807 response and HTTP status without a chain of {@code instanceof} checks. Business
 * rules themselves live in the services; this is only the shared carrier.
 */
public class BusinessException extends RuntimeException {

    private final transient ErrorType errorType;

    public BusinessException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public BusinessException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
