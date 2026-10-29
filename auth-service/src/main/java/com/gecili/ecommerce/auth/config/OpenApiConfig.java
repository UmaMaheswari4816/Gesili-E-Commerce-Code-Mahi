package com.gecili.ecommerce.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata + a JWT Bearer security scheme so the Swagger UI "Authorize" button lets
 * you paste an access token and call protected endpoints.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenAPI() {
        final String scheme = "bearerAuth";
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Gecili Auth Service API")
                                .description("Registration, login, RS256 JWT access + rotating refresh tokens, JWKS.")
                                .version("v1")
                                .contact(new Contact().name("Gecili").url("https://github.com/UmaMaheswari4816/Gecili-E-Commerce"))
                                .license(new License().name("MIT")))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        scheme,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description("Paste an access token from /api/v1/auth/login")));
    }
}
