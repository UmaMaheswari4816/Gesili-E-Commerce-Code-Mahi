package com.gecili.ecommerce.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Enables {@link AuthProperties} binding. */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfig {}
