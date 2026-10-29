package com.gecili.ecommerce.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.gecili.ecommerce.product.dto.ProductDtos.CreateProductRequest;
import com.gecili.ecommerce.product.entity.Category;
import com.gecili.ecommerce.product.repository.jpa.CategoryRepository;
import com.gecili.ecommerce.product.repository.search.ProductSearchRepository;
import com.gecili.ecommerce.product.service.CatalogCommandService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Full CQRS flow against real PostgreSQL + Elasticsearch (Testcontainers): create a product
 * on the write side, then assert it becomes searchable in the read model (eventually, since
 * projection is async).
 */
@Testcontainers
@SpringBootTest
class CatalogCqrsIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("gecili_product");

    @Container
    static final ElasticsearchContainer ES =
            new ElasticsearchContainer(
                            DockerImageName.parse(
                                    "docker.elastic.co/elasticsearch/elasticsearch:8.15.3"))
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("discovery.type", "single-node");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.elasticsearch.uris", () -> "http://" + ES.getHttpHostAddress());
        registry.add("spring.config.import", () -> "");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:9001/.well-known/jwks.json");
    }

    @Autowired private CatalogCommandService command;
    @Autowired private CategoryRepository categories;
    @Autowired private ProductSearchRepository search;

    @Test
    void createdProduct_becomesSearchable() {
        Category category = categories.save(Category.create("Tees", "tees", "Cotton tees"));

        var created =
                command.createProduct(
                        new CreateProductRequest(
                                "TSHIRT-BLUE-M-001",
                                "Blue Cotton Tee",
                                "soft cotton",
                                new BigDecimal("399.00"),
                                "INR",
                                category.getId()));

        // Projection is async — wait for the document to appear in the read model.
        await()
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(
                        () -> {
                            var results =
                                    search.findByActiveTrueAndNameContainingOrActiveTrueAndDescriptionContaining(
                                            "Blue", "Blue", PageRequest.of(0, 10));
                            assertThat(results.getContent())
                                    .anyMatch(d -> d.getSku().equals("TSHIRT-BLUE-M-001"));
                        });

        assertThat(created.id()).isNotNull();
    }
}
