package demo.facilitator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.x402.core.hooks.HookResults;
import org.x402.spring.facilitator.X402FacilitatorCustomizer;

/**
 * Customization 2a, observe: hooks around every verify and settle. An X402FacilitatorCustomizer
 * bean receives the X402Facilitator before it serves requests; any number of them can exist.
 *
 * <p>After-hooks only observe (book a payment, notify the seller, export metrics). Failure hooks
 * may answer instead of failing by returning {@code Recovery.recovered(response)}.
 */
@Configuration
class AuditHooks {

  private static final Logger LOG = LoggerFactory.getLogger(AuditHooks.class);

  @Bean
  X402FacilitatorCustomizer auditLogging(FacilitatorStats stats) {
    return facilitator -> facilitator
        .onAfterVerify(ctx -> {
          stats.increment(ctx.result().isValid() ? "verify.valid" : "verify.invalid");
          LOG.info("VERIFIED {} {} to {}: valid={} reason={}", ctx.requirements().amount(),
              ctx.requirements().asset(), ctx.requirements().payTo(), ctx.result().isValid(),
              ctx.result().invalidReason());
        })
        .onAfterSettle(ctx -> {
          stats.increment(ctx.result().success() ? "settle.success" : "settle.failed");
          LOG.info("SETTLED  tx={}: success={} reason={}", ctx.result().transaction(),
              ctx.result().success(), ctx.result().errorReason());
        })
        .onSettleFailure(ctx -> {
          // An exception, not a "no": the chain provider was down, for example.
          stats.increment("settle.error");
          LOG.warn("SETTLE ERROR {}", ctx.error().getMessage());
          return HookResults.Recovery.none();
        });
  }
}
