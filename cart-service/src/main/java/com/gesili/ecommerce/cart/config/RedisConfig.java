package com.gesili.ecommerce.cart.config;

import com.gesili.ecommerce.cart.model.Cart;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/** Redis template that stores {@link Cart} as JSON under a String key. */
@Configuration
@EnableConfigurationProperties(CartProperties.class)
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Cart> cartRedisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, Cart> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Cart.class));
        template.afterPropertiesSet();
        return template;
    }
}
