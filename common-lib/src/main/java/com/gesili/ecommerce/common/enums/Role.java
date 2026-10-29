package com.gesili.ecommerce.common.enums;

/**
 * Coarse-grained authorization roles, owned by the auth-service.
 *
 * <p>Embedded as claims in the RS256 JWT and checked at both the gateway and each
 * resource server (defence in depth). Prefix with {@code ROLE_} when mapping to Spring
 * Security authorities.
 */
public enum Role {

    /** Standard authenticated shopper. */
    CUSTOMER,

    /** Back-office administrator (catalog, inventory, order management). */
    ADMIN,

    /**
     * Machine-to-machine / internal service principal. Used for internal endpoints that
     * are hidden from public Swagger groups and not reachable through the public gateway
     * routes.
     */
    SERVICE
}
