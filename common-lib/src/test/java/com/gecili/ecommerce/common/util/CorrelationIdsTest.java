package com.gecili.ecommerce.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CorrelationIdsTest {

    @Test
    void generate_producesNonBlankUniqueIds() {
        String a = CorrelationIds.generate();
        String b = CorrelationIds.generate();
        assertThat(a).isNotBlank();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void orGenerate_keepsExistingId() {
        assertThat(CorrelationIds.orGenerate("abc")).isEqualTo("abc");
    }

    @Test
    void orGenerate_generatesForNullOrBlank() {
        assertThat(CorrelationIds.orGenerate(null)).isNotBlank();
        assertThat(CorrelationIds.orGenerate("   ")).isNotBlank();
    }
}
