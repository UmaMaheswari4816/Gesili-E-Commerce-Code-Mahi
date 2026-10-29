package com.gesili.ecommerce.product.mapper;

import com.gesili.ecommerce.product.dto.ProductDtos.CategoryResponse;
import com.gesili.ecommerce.product.dto.ProductDtos.ProductResponse;
import com.gesili.ecommerce.product.dto.ProductDtos.ProductSearchResult;
import com.gesili.ecommerce.product.entity.Category;
import com.gesili.ecommerce.product.entity.Product;
import com.gesili.ecommerce.product.search.ProductDocument;
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
