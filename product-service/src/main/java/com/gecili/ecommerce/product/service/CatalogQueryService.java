package com.gecili.ecommerce.product.service;

import com.gecili.ecommerce.common.exception.ResourceNotFoundException;
import com.gecili.ecommerce.product.dto.ProductDtos.ProductResponse;
import com.gecili.ecommerce.product.dto.ProductDtos.ProductSearchResult;
import com.gecili.ecommerce.product.mapper.ProductMapper;
import com.gecili.ecommerce.product.repository.jpa.ProductRepository;
import com.gecili.ecommerce.product.repository.search.ProductSearchRepository;
import com.gecili.ecommerce.product.search.ProductDocument;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The QUERY side of CQRS. Point lookups (by id) read the authoritative write model; search
 * and filtering read the Elasticsearch read model, which is optimized for those access
 * patterns and scales independently of writes.
 */
@Service
public class CatalogQueryService {

    private final ProductRepository products;
    private final ProductSearchRepository search;
    private final ProductMapper mapper;

    public CatalogQueryService(
            ProductRepository products, ProductSearchRepository search, ProductMapper mapper) {
        this.products = products;
        this.search = search;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        return products
                .findById(id)
                .map(mapper::toProductResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", id));
    }

    /** Full-text search over the read model. Blank query returns all active products. */
    public Page<ProductSearchResult> search(String query, Pageable pageable) {
        Page<ProductDocument> page;
        if (query == null || query.isBlank()) {
            page = search.findByActiveTrue(pageable);
        } else {
            page =
                    search.findByActiveTrueAndNameContainingOrActiveTrueAndDescriptionContaining(
                            query, query, pageable);
        }
        return page.map(mapper::toSearchResult);
    }

    public Page<ProductSearchResult> byCategory(String slug, Pageable pageable) {
        return search.findByCategorySlugAndActiveTrue(slug, pageable).map(mapper::toSearchResult);
    }
}
