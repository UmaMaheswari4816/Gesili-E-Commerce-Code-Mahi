package com.gesili.ecommerce.cart.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for cart aggregate logic (merge, update, remove, total) — no Redis needed. */
class CartTest {

    private CartLine line(String sku, int qty, String price) {
        return new CartLine(UUID.randomUUID(), sku, sku, qty, new BigDecimal(price));
    }

    @Test
    void addOrMerge_accumulatesSameSku() {
        Cart cart = Cart.empty(UUID.randomUUID());
        cart.addOrMerge(line("SKU-A-001", 2, "100.00"));
        cart.addOrMerge(line("SKU-A-001", 3, "100.00"));

        assertThat(cart.getLines()).hasSize(1);
        assertThat(cart.getLines().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void total_sumsLineTotals() {
        Cart cart = Cart.empty(UUID.randomUUID());
        cart.addOrMerge(line("SKU-A-001", 2, "100.00")); // 200
        cart.addOrMerge(line("SKU-B-001", 1, "49.50"));  // 49.50

        assertThat(cart.total()).isEqualByComparingTo("249.50");
    }

    @Test
    void setQuantityToZero_removesLine() {
        Cart cart = Cart.empty(UUID.randomUUID());
        cart.addOrMerge(line("SKU-A-001", 2, "100.00"));
        cart.setQuantity("SKU-A-001", 0);

        assertThat(cart.getLines()).isEmpty();
    }

    @Test
    void removeSku_dropsThatLineOnly() {
        Cart cart = Cart.empty(UUID.randomUUID());
        cart.addOrMerge(line("SKU-A-001", 1, "10.00"));
        cart.addOrMerge(line("SKU-B-001", 1, "20.00"));
        cart.removeSku("SKU-A-001");

        assertThat(cart.getLines()).hasSize(1);
        assertThat(cart.getLines().get(0).getSku()).isEqualTo("SKU-B-001");
    }
}
