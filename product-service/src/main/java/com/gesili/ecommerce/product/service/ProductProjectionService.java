package com.gesili.ecommerce.product.service;

import com.gesili.ecommerce.product.entity.Category;
import com.gesili.ecommerce.product.entity.Product;
import com.gesili.ecommerce.product.repository.jpa.CategoryRepository;
import com.gesili.ecommerce.product.repository.search.ProductSearchRepository;
import com.gesili.ecommerce.product.search.ProductDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Projects write-model changes ({@link Product}) into the Elasticsearch read model
 * ({@link ProductDocument}) — the sync half of CQRS.
 *
 * <p>Runs asynchronously so catalog writes (the command side) don't block on ES indexing;
 * the read model is <em>eventually consistent</em>. In a larger system this projection would
 * be driven by the outbox/events rather than an in-process {@code @Async} call; here we keep
 * it simple and note the trade-off (a crash between the DB commit and the projection can
 * leave ES briefly stale until the next write or a reindex).
 */
@Slf4j
@Service
public class ProductProjectionService {

    private final ProductSearchRepository searchRepository;
    private final CategoryRepository categoryRepository;

    public ProductProjectionService(
            ProductSearchRepository searchRepository, CategoryRepository categoryRepository) {
        this.searchRepository = searchRepository;
        this.categoryRepository = categoryRepository;
    }

    @Async
    public void project(Product product) {
        try {
            Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
            ProductDocument doc =
                    ProductDocument.builder()
                            .id(ProductDocument.idOf(product.getId()))
                            .sku(product.getSku())
                            .name(product.getName())
                            .description(product.getDescription())
                            .price(product.getPrice())
                            .currency(product.getCurrency())
                            .categoryId(product.getCategoryId().toString())
                            .categoryName(category != null ? category.getName() : null)
                            .categorySlug(category != null ? category.getSlug() : null)
                            .active(product.isActive())
                            .build();
            searchRepository.save(doc);
            log.debug("Projected product {} into ES read model", product.getId());
        } catch (Exception e) {
            // Do not fail the write if ES is down; log for a later reindex.
            log.error("Failed to project product {} into ES: {}", product.getId(), e.getMessage());
        }
    }

    @Async
    public void remove(java.util.UUID productId) {
        try {
            searchRepository.deleteById(ProductDocument.idOf(productId));
        } catch (Exception e) {
            log.error("Failed to remove product {} from ES: {}", productId, e.getMessage());
        }
    }
}
