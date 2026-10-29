package com.gesili.ecommerce.common.exception;

import com.gesili.ecommerce.common.error.ErrorType;

/** Thrown by inventory-service when stock cannot cover a requested quantity. Maps to 409. */
public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(String message) {
        super(ErrorType.INSUFFICIENT_STOCK, message);
    }
}
