package com.gesili.ecommerce.product.service;

import com.gesili.ecommerce.common.exception.ResourceNotFoundException;
import com.gesili.ecommerce.product.dto.ProductDtos.ProductResponse;
import com.gesili.ecommerce.product.dto.ProductDtos.ProductSearchResult;
import com.gesili.ecommerce.product.mapper.ProductMapper;
import com.gesili.ecommerce.product.repository.jpa.ProductRepository;
import com.gesili.ecommerce.product.repository.search.ProductSearchRepository;
import com.gesili.ecommerce.product.search.ProductDocument;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The QUERY side of CQRS. Point lookups (by id) read the authoritative write model; search
 * and filtering read the Elasticsearch read model, which is optimized for those access
 * patterns and scales independently of writes.
 *
 * <p>FLOW (the READ half of CQRS):
 * <pre>
 *   browser → gateway → ProductQueryController → HERE
 *     getById()  → PostgreSQL (write model = source of truth)
 *     search()   → Elasticsearch (read model, fast full-text)   ← kept in sync by
 *                                                                  ProductProjectionService
 * </pre>
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
