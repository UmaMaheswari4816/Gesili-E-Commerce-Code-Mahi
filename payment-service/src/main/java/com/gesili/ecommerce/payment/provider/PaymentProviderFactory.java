package com.gesili.ecommerce.payment.provider;

import com.gesili.ecommerce.payment.config.PaymentProperties;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Factory that resolves the active {@link PaymentProvider} by key — from the
 * {@code X-Payment-Provider} header when present, else the configured default. Spring injects
 * all provider beans; adding a provider needs no change here.
 */
@Component
public class PaymentProviderFactory {

    private final Map<String, PaymentProvider> byName;
    private final PaymentProperties props;

    public PaymentProviderFactory(List<PaymentProvider> providers, PaymentProperties props) {
        this.byName =
                providers.stream().collect(Collectors.toMap(PaymentProvider::name, Function.identity()));
        this.props = props;
    }

    public PaymentProvider resolve(String requested) {
        String key = (requested == null || requested.isBlank()) ? props.defaultProvider() : requested;
        PaymentProvider provider = byName.get(key.toLowerCase());
        if (provider == null) {
            throw new IllegalArgumentException("Unknown payment provider: " + key);
        }
        return provider;
    }

    public PaymentProvider byName(String name) {
        return resolve(name);
    }
}
