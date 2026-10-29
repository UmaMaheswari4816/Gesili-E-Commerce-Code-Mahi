package com.gecili.ecommerce.gateway.web;

import com.gecili.ecommerce.common.error.ErrorType;
import com.gecili.ecommerce.common.error.ProblemDetails;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Circuit-breaker fallback endpoints. When a route's Resilience4j circuit opens (the target
 * service is down or slow), the gateway forwards here instead of failing hard, returning a
 * clean RFC 7807 503 so clients get a consistent, documented error.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/{service}")
    public ResponseEntity<ProblemDetail> getFallback(
            @org.springframework.web.bind.annotation.PathVariable String service) {
        return build(service);
    }

    @PostMapping("/{service}")
    public ResponseEntity<ProblemDetail> postFallback(
            @org.springframework.web.bind.annotation.PathVariable String service) {
        return build(service);
    }

    private ResponseEntity<ProblemDetail> build(String service) {
        ProblemDetail pd =
                ProblemDetails.of(
                        ErrorType.DOWNSTREAM_UNAVAILABLE,
                        "The '" + service + "' service is temporarily unavailable. Please retry shortly.",
                        null);
        return ResponseEntity.status(ErrorType.DOWNSTREAM_UNAVAILABLE.status()).body(pd);
    }
}
