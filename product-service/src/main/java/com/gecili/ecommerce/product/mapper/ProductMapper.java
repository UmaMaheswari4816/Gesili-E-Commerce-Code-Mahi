package com.gecili.ecommerce.product.mapper;

import com.gecili.ecommerce.product.dto.ProductDtos.CategoryResponse;
import com.gecili.ecommerce.product.dto.ProductDtos.ProductResponse;
import com.gecili.ecommerce.product.dto.ProductDtos.ProductSearchResult;
import com.gecili.ecommerce.product.entity.Category;
import com.gecili.ecommerce.product.entity.Product;
import com.gecili.ecommerce.product.search.ProductDocument;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct mappings between entities/documents and DTOs. */
@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toProductResponse(Product product);

    CategoryResponse toCategoryResponse(Category category);

    @Mapping(target = "id", source = "id", qualifiedByName = "stringToUuid")
    ProductSearchResult toSearchResult(ProductDocument doc);

    @org.mapstruct.Named("stringToUuid")
    static UUID stringToUuid(String id) {
        return id == null ? null : UUID.fromString(id);
    }
}
