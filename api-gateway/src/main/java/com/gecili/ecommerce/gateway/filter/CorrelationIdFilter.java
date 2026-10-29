package com.gecili.ecommerce.gateway.filter;

import com.gecili.ecommerce.common.constant.HttpHeaders;
import com.gecili.ecommerce.common.util.CorrelationIds;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Ensures every request carries a correlation id, propagated downstream and echoed back.
 *
 * <p>If the incoming request has no {@code X-Correlation-Id}, one is generated. The id is
 * added to the mutated request headers (so downstream services and traces share it) and to
 * the response headers (so clients can report it).
 *
 * <p>This filter only reads/writes HEADERS — it never touches the request body — so it is
 * safe on all routes, including the payment webhook route.
 */
@Component
public class CorrelationIdFilter implements WebFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HttpHeaders.CORRELATION_ID);
        String correlationId = CorrelationIds.orGenerate(incoming);

        ServerWebExchange mutated =
                exchange
                        .mutate()
                        .request(r -> r.header(HttpHeaders.CORRELATION_ID, correlationId))
                        .build();
        mutated.getResponse().getHeaders().set(HttpHeaders.CORRELATION_ID, correlationId);
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
