package com.gecili.ecommerce.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server.
 *
 * <p>Serves centralized configuration from the {@code config-repo/} folder using the
 * <strong>native</strong> (filesystem) backend. Starting the config server first, before any
 * business service, is deliberate: services import their configuration from here at boot via
 * {@code spring.config.import=configserver:...}. If the config server is unreachable in dev,
 * services use {@code optional:configserver:} so they can still start; in production you
 * typically fail fast instead.
 *
 * <p>{@link EnableConfigServer} turns this Boot app into a config server, exposing endpoints
 * such as {@code /{application}/{profile}} and {@code /{application}-{profile}.yml}.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
