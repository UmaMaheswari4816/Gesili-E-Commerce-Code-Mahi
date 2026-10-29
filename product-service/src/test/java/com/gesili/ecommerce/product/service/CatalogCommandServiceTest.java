package com.gesili.ecommerce.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gesili.ecommerce.common.exception.ConflictException;
import com.gesili.ecommerce.common.exception.ResourceNotFoundException;
import com.gesili.ecommerce.product.dto.ProductDtos.CreateProductRequest;
import com.gesili.ecommerce.product.entity.Category;
import com.gesili.ecommerce.product.entity.Product;
import com.gesili.ecommerce.product.mapper.ProductMapper;
import com.gesili.ecommerce.product.repository.jpa.CategoryRepository;
import com.gesili.ecommerce.product.repository.jpa.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the CQRS command side: validation, conflict handling, and projection. */
@ExtendWith(MockitoExtension.class)
class CatalogCommandServiceTest {

    @Mock private ProductRepository products;
    @Mock private CategoryRepository categories;
    @Mock private ProductProjectionService projection;

    private CatalogCommandService service;

    @BeforeEach
    void setUp() {
        ProductMapper mapper = Mappers.getMapper(ProductMapper.class);
        service = new CatalogCommandService(products, categories, projection, mapper);
    }

    @Test
    void createProduct_persistsAndProjects() {
        UUID categoryId = UUID.randomUUID();
        when(products.existsBySku("TSHIRT-RED-XL-001")).thenReturn(false);
        when(categories.findById(categoryId))
                .thenReturn(Optional.of(Category.create("Tees", "tees", null)));
        when(products.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        var req =
                new CreateProductRequest(
                        "TSHIRT-RED-XL-001", "Red Tee", "cotton", new BigDecimal("499.00"), "inr", categoryId);
        var response = service.createProduct(req);

        assertThat(response.sku()).isEqualTo("TSHIRT-RED-XL-001");
        assertThat(response.currency()).isEqualTo("INR"); // upper-cased
        verify(projection).project(any(Product.class));   // read model updated
    }

    @Test
    void createProduct_duplicateSku_conflicts() {
        when(products.existsBySku("DUP-SKU-001")).thenReturn(true);
        var req =
                new CreateProductRequest(
                        "DUP-SKU-001", "x", null, new BigDecimal("1.00"), "INR", UUID.randomUUID());
        assertThatThrownBy(() -> service.createProduct(req)).isInstanceOf(ConflictException.class);
    }

    @Test
    void createProduct_missingCategory_notFound() {
        UUID categoryId = UUID.randomUUID();
        when(products.existsBySku("NEW-SKU-001")).thenReturn(false);
        when(categories.findById(categoryId)).thenReturn(Optional.empty());
        var req =
                new CreateProductRequest(
                        "NEW-SKU-001", "x", null, new BigDecimal("1.00"), "INR", categoryId);
        assertThatThrownBy(() -> service.createProduct(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
