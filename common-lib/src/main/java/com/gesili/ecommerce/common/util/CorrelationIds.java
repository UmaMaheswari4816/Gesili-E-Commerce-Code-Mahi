package com.gesili.ecommerce.common.util;

import java.util.UUID;

/** Small helpers for generating and normalizing correlation ids. */
public final class CorrelationIds {

    private CorrelationIds() {}

    /** Generate a fresh correlation id. */
    public static String generate() {
        return UUID.randomUUID().toString();
    }

    /** Return the given id, or a freshly generated one if it is null/blank. */
    public static String orGenerate(String candidate) {
        return (candidate == null || candidate.isBlank()) ? generate() : candidate;
    }
}
