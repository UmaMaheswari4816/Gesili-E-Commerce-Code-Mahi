package com.gecili.ecommerce.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Shared Bean Validation constraint for stock-keeping unit (SKU) strings.
 *
 * <p>A SKU is 4&ndash;32 chars of uppercase letters, digits and hyphens, e.g.
 * {@code TSHIRT-RED-XL-001}. Defined in common-lib so product, inventory, cart and order
 * services validate SKUs identically. The regex is the single source of truth.
 */
@Documented
@Constraint(validatedBy = {}) // uses the @Pattern-style meta-constraint below
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@jakarta.validation.constraints.Pattern(regexp = Sku.PATTERN)
@jakarta.validation.ReportAsSingleViolation
public @interface Sku {

    String PATTERN = "^[A-Z0-9][A-Z0-9-]{2,30}[A-Z0-9]$";

    String message() default "must be a valid SKU (4-32 chars: A-Z, 0-9, hyphen)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
