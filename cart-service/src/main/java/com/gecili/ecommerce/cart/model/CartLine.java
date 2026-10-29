package com.gecili.ecommerce.cart.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A line in the cart. {@code unitPrice} is a snapshot at add-time, re-validated at checkout. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartLine implements Serializable {
    private UUID productId;
    private String sku;
    private String name;
    private int quantity;
    private BigDecimal unitPrice;
}
