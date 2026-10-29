package com.gecili.ecommerce.common.error;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.ProblemDetail;

/**
 * Factory helpers that build Spring's RFC 7807 {@link ProblemDetail} with the project's
 * conventions applied (stable error {@code code}, {@code timestamp}, {@code correlationId},
 * and optional field-level {@code errors}).
 *
 * <p>RFC 7807 defines a standard JSON shape for API errors ({@code type}, {@code title},
 * {@code status}, {@code detail}, {@code instance}). Using it everywhere means clients and
 * the aggregated Swagger UI document a single error schema. Services register a
 * {@code @RestControllerAdvice} that delegates here (added per service from Phase 3 on).
 */
public final class ProblemDetails {

    public static final String PROP_CODE = "code";
    public static final String PROP_TIMESTAMP = "timestamp";
    public static final String PROP_CORRELATION_ID = "correlationId";
    public static final String PROP_ERRORS = "errors";

    private ProblemDetails() {}

    /** Build a ProblemDetail from an {@link ErrorType} with a specific detail message. */
    public static ProblemDetail of(ErrorType errorType, String detail, String correlationId) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(errorType.status(), detail);
        pd.setType(URI.create(errorType.type()));
        pd.setTitle(errorType.title());
        pd.setProperty(PROP_CODE, errorType.code());
        pd.setProperty(PROP_TIMESTAMP, Instant.now().toString());
        if (correlationId != null) {
            pd.setProperty(PROP_CORRELATION_ID, correlationId);
        }
        return pd;
    }

    /** Build a validation ProblemDetail carrying field-level errors. */
    public static ProblemDetail validation(
            String detail, String correlationId, List<FieldViolation> violations) {
        ProblemDetail pd = of(ErrorType.VALIDATION_ERROR, detail, correlationId);
        pd.setProperty(PROP_ERRORS, violations);
        return pd;
    }

    /** Attach arbitrary extra properties (used by service-specific advices). */
    public static ProblemDetail withProperties(ProblemDetail pd, Map<String, Object> extra) {
        extra.forEach(pd::setProperty);
        return pd;
    }

    /**
     * A single field validation violation, serialized under {@code errors}.
     *
     * @param field   the offending field (dotted path for nested objects)
     * @param message the violation message
     * @param rejected the rejected value (may be null)
     */
    public record FieldViolation(String field, String message, Object rejected) {}
}
