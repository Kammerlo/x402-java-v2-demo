package demo.facilitator;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.x402.core.hooks.HookResults;
import org.x402.core.types.PaymentRequirements;
import org.x402.spring.facilitator.X402FacilitatorCustomizer;

/**
 * Customization 2b, refuse: a business rule as before-hooks. This facilitator only serves the
 * sellers listed in {@code demo.allowed-payees}; a payment to anyone else is answered with
 * {@code isValid=false} (verify) or {@code success=false} (settle), never submitted.
 *
 * <p>The rule sits on both verify and settle: a resource server may call settle without an
 * earlier verify, so a verify-only rule could be bypassed.
 */
@Configuration
class PayeePolicy {

  private static final Logger LOG = LoggerFactory.getLogger(PayeePolicy.class);

  @Bean
  X402FacilitatorCustomizer payeePolicyHooks(@Value("${demo.allowed-payees}") String allowed,
      FacilitatorStats stats) {
    Set<String> payees = Arrays.stream(allowed.split(","))
        .map(String::strip).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
    return facilitator -> facilitator
        .onBeforeVerify(ctx -> check(ctx.requirements(), payees, stats))
        .onBeforeSettle(ctx -> check(ctx.requirements(), payees, stats));
  }

  private static HookResults.Decision check(PaymentRequirements requirements, Set<String> payees,
      FacilitatorStats stats) {
    if (payees.contains(requirements.payTo())) {
      return HookResults.Decision.proceed();
    }
    stats.increment("policy.refused");
    LOG.warn("REFUSED payment to {}: not an allowed payee", requirements.payTo());
    return HookResults.Decision.abort("payee_not_allowed");
  }
}
