package com.gecili.ecommerce.cart.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A user's cart, stored in Redis as a JSON value. Kept simple and serializable; no JPA, no
 * database. The cart is the aggregate; {@link CartLine} is a value object within it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart implements Serializable {

    private UUID userId;
    private List<CartLine> lines = new ArrayList<>();

    public static Cart empty(UUID userId) {
        return new Cart(userId, new ArrayList<>());
    }

    /** Add or merge a line by SKU (quantities accumulate). */
    public void addOrMerge(CartLine line) {
        for (CartLine existing : lines) {
            if (existing.getSku().equals(line.getSku())) {
                existing.setQuantity(existing.getQuantity() + line.getQuantity());
                return;
            }
        }
        lines.add(line);
    }

    public void setQuantity(String sku, int quantity) {
        lines.removeIf(l -> l.getSku().equals(sku) && quantity <= 0);
        for (CartLine l : lines) {
            if (l.getSku().equals(sku)) {
                l.setQuantity(quantity);
                return;
            }
        }
    }

    public void removeSku(String sku) {
        lines.removeIf(l -> l.getSku().equals(sku));
    }

    public BigDecimal total() {
        return lines.stream()
                .map(l -> l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
