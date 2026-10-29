package com.gecili.ecommerce.product.repository.search;

import com.gecili.ecommerce.product.search.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Elasticsearch read-model repository. Derived queries here run full-text search against the
 * denormalized {@link ProductDocument} index.
 */
public interface ProductSearchRepository
        extends ElasticsearchRepository<ProductDocument, String> {

    /** Full-text search across name and description, active products only. */
    Page<ProductDocument> findByActiveTrueAndNameContainingOrActiveTrueAndDescriptionContaining(
            String name, String description, Pageable pageable);

    Page<ProductDocument> findByCategorySlugAndActiveTrue(String categorySlug, Pageable pageable);

    Page<ProductDocument> findByActiveTrue(Pageable pageable);
}
