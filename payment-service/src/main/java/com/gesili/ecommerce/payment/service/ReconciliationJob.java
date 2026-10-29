package com.gesili.ecommerce.payment.service;

import com.gesili.ecommerce.common.enums.PaymentStatus;
import com.gesili.ecommerce.payment.entity.Payment;
import com.gesili.ecommerce.payment.provider.PaymentProvider;
import com.gesili.ecommerce.payment.provider.PaymentProviderFactory;
import com.gesili.ecommerce.payment.repository.PaymentRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Safety net for missed/dropped webhooks. Periodically resolves payments stuck in transient
 * states — e.g. retry a {@code REFUND_FAILED}, or (in a full impl) query the provider for the
 * real status of an {@code INITIATED} payment and reconcile. ShedLock => one runner.
 *
 * <p>Here we retry refunds that previously failed; querying the provider for stuck INITIATED
 * payments is the documented extension (needs the live provider API).
 */
@Slf4j
@Component
public class ReconciliationJob {

    private final PaymentRepository payments;
    private final PaymentProviderFactory providers;

    public ReconciliationJob(PaymentRepository payments, PaymentProviderFactory providers) {
        this.payments = payments;
        this.providers = providers;
    }

    @Scheduled(fixedDelayString = "${gesili.payment.reconcile-ms:300000}")
    @SchedulerLock(name = "payment-reconciliation", lockAtLeastFor = "PT30S", lockAtMostFor = "PT5M")
    public void reconcile() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(2));
        List<Payment> stuck =
                payments.findByStatusInAndUpdatedAtBefore(List.of(PaymentStatus.REFUND_FAILED), cutoff);
        if (stuck.isEmpty()) {
            return;
        }
        log.info("Reconciliation retrying {} failed refund(s)", stuck.size());
        for (Payment p : stuck) {
            try {
                PaymentProvider provider = providers.byName(p.getProvider());
                var result = provider.refund(p.getProviderRef(), p.getAmount());
                if (result.success()) {
                    p.setStatus(PaymentStatus.REFUNDED);
                    p.touch();
                    payments.save(p);
                }
            } catch (Exception e) {
                log.error("Reconciliation failed for payment {}: {}", p.getId(), e.getMessage());
            }
        }
    }
}
