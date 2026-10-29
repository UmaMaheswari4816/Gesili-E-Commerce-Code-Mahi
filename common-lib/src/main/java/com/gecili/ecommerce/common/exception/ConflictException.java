package com.gecili.ecommerce.common.exception;

import com.gecili.ecommerce.common.error.ErrorType;

/**
 * Thrown on state conflicts (optimistic-lock failures, duplicate idempotency keys,
 * illegal state transitions). Maps to HTTP 409.
 */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(ErrorType.CONFLICT, message);
    }

    public ConflictException(ErrorType errorType, String message) {
        super(errorType, message);
    }
}
