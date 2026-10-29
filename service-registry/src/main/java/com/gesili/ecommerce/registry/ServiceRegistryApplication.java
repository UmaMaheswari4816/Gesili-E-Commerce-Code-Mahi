package com.gesili.ecommerce.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Netflix Eureka Server — the service registry.
 *
 * <p>Every other service registers here on startup and sends periodic heartbeats; clients
 * then resolve logical service names (e.g. {@code lb://product-service}) to live instances.
 * This removes hardcoded host/port coupling and enables client-side load balancing.
 *
 * <p>Started after the Config Server (from which it imports its own config) and before the
 * business services. In production you run two or more Eureka instances that replicate to
 * each other (peer awareness) so the registry itself is not a single point of failure — see
 * the {@code peer} profile in {@code application.yml} and this module's README.
 */
@SpringBootApplication
@EnableEurekaServer
public class ServiceRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceRegistryApplication.class, args);
    }
}
