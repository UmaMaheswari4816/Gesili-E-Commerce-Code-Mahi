package com.gecili.ecommerce.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Gecili Notification Service API")
                                .description("Consumes saga events; sends email/SMS; logs notifications.")
                                .version("v1"));
    }
}
