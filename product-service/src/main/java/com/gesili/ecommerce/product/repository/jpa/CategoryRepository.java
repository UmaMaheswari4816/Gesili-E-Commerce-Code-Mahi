package com.gesili.ecommerce.product.repository.jpa;

import com.gesili.ecommerce.product.entity.Category;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findBySlug(String slug);

    boolean existsBySlugOrName(String slug, String name);
}
