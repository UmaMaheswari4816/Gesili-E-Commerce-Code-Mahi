package com.gesili.ecommerce.product.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Enables {@code @Async} so the CQRS projection runs off the request thread. */
@Configuration
@EnableAsync
public class AsyncConfig {}
