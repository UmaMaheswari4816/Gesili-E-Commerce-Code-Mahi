package com.gesili.ecommerce.cart.service;

import com.gesili.ecommerce.cart.config.CartProperties;
import com.gesili.ecommerce.cart.dto.CartDtos.AddItemRequest;
import com.gesili.ecommerce.cart.dto.CartDtos.CartLineResponse;
import com.gesili.ecommerce.cart.dto.CartDtos.CartResponse;
import com.gesili.ecommerce.cart.model.Cart;
import com.gesili.ecommerce.cart.model.CartLine;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/** Cart operations backed by Redis. Each write refreshes the cart's TTL. */
@Service
public class CartService {

    private static final String KEY_PREFIX = "cart:";

    private final RedisTemplate<String, Cart> redis;
    private final CartProperties props;

    public CartService(RedisTemplate<String, Cart> redis, CartProperties props) {
        this.redis = redis;
        this.props = props;
    }

    public CartResponse get(UUID userId) {
        return toResponse(load(userId));
    }

    public CartResponse addItem(UUID userId, AddItemRequest req) {
        Cart cart = load(userId);
        cart.addOrMerge(
                new CartLine(req.productId(), req.sku(), req.name(), req.quantity(), req.unitPrice()));
        return save(cart);
    }

    public CartResponse updateQuantity(UUID userId, String sku, int quantity) {
        Cart cart = load(userId);
        cart.setQuantity(sku, quantity);
        return save(cart);
    }

    public CartResponse removeItem(UUID userId, String sku) {
        Cart cart = load(userId);
        cart.removeSku(sku);
        return save(cart);
    }

    public void clear(UUID userId) {
        redis.delete(key(userId));
    }

    private Cart load(UUID userId) {
        Cart cart = redis.opsForValue().get(key(userId));
        return cart != null ? cart : Cart.empty(userId);
    }

    private CartResponse save(Cart cart) {
        redis.opsForValue().set(key(cart.getUserId()), cart, Duration.ofSeconds(props.ttlSeconds()));
        return toResponse(cart);
    }

    private static String key(UUID userId) {
        return KEY_PREFIX + userId;
    }

    private CartResponse toResponse(Cart cart) {
        var lines =
                cart.getLines().stream()
                        .map(l -> new CartLineResponse(l.getProductId(), l.getSku(), l.getName(), l.getQuantity(), l.getUnitPrice()))
                        .toList();
        return new CartResponse(cart.getUserId(), lines, cart.total());
    }
}
