package com.gecili.ecommerce.common.exception;

import com.gecili.ecommerce.common.error.ErrorType;

/** Thrown when a requested resource does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(ErrorType.NOT_FOUND, message);
    }

    /** Convenience: "{resource} with id {id} not found". */
    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException(resource + " with id " + id + " not found");
    }
}
