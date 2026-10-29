package com.gesili.ecommerce.common.constant;

/** Shared API constants (versioning prefix, pagination defaults). */
public final class ApiConstants {

    private ApiConstants() {}

    /** Base path prefix for all public v1 APIs. */
    public static final String API_V1 = "/api/v1";

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
}
