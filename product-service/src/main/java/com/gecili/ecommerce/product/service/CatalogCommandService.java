package com.gecili.ecommerce.product.service;

import com.gecili.ecommerce.common.exception.ConflictException;
import com.gecili.ecommerce.common.exception.ResourceNotFoundException;
import com.gecili.ecommerce.product.dto.ProductDtos.CategoryResponse;
import com.gecili.ecommerce.product.dto.ProductDtos.CreateCategoryRequest;
import com.gecili.ecommerce.product.dto.ProductDtos.CreateProductRequest;
import com.gecili.ecommerce.product.dto.ProductDtos.ProductResponse;
import com.gecili.ecommerce.product.dto.ProductDtos.UpdateProductRequest;
import com.gecili.ecommerce.product.entity.Category;
import com.gecili.ecommerce.product.entity.Product;
import com.gecili.ecommerce.product.mapper.ProductMapper;
import com.gecili.ecommerce.product.repository.jpa.CategoryRepository;
import com.gecili.ecommerce.product.repository.jpa.ProductRepository;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The COMMAND side of CQRS: all catalog writes against the PostgreSQL write model. After each
 * successful write it triggers an async projection into the Elasticsearch read model.
 */
@Slf4j
@Service
public class CatalogCommandService {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ProductProjectionService projection;
    private final ProductMapper mapper;

    public CatalogCommandService(
            ProductRepository products,
            CategoryRepository categories,
            ProductProjectionService projection,
            ProductMapper mapper) {
        this.products = products;
        this.categories = categories;
        this.projection = projection;
        this.mapper = mapper;
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest req) {
        if (categories.existsBySlugOrName(req.slug(), req.name())) {
            throw new ConflictException("Category with that name or slug already exists");
        }
        Category c = categories.save(Category.create(req.name(), req.slug(), req.description()));
        return mapper.toCategoryResponse(c);
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest req) {
        if (products.existsBySku(req.sku())) {
            throw new ConflictException("Product with SKU " + req.sku() + " already exists");
        }
        categories
                .findById(req.categoryId())
                .orElseThrow(() -> ResourceNotFoundException.of("Category", req.categoryId()));

        Product p =
                products.save(
                        Product.create(
                                req.sku(),
                                req.name(),
                                req.description(),
                                req.price(),
                                req.currency().toUpperCase(),
                                req.categoryId()));
        projection.project(p);
        return mapper.toProductResponse(p);
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest req) {
        Product p =
                products.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Product", id));
        if (req.name() != null) {
            p.setName(req.name());
        }
        if (req.description() != null) {
            p.setDescription(req.description());
        }
        if (req.price() != null) {
            p.setPrice(req.price());
        }
        if (req.active() != null) {
            p.setActive(req.active());
        }
        p.touch();
        products.save(p);
        projection.project(p);
        return mapper.toProductResponse(p);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Product p =
                products.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Product", id));
        products.delete(p);
        projection.remove(id);
    }
}
