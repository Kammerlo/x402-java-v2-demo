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
 * <p>Which hook sees what:
 * <ul>
 *   <li>{@code onAfterVerify}: valid payments only. An invalid one goes to {@code onVerifyFailure}.
 *   <li>{@code onAfterSettle}: every settle answer, successful or not. Chain-provider outages
 *       arrive here as {@code success=false} (often "settlement_pending"), not as exceptions.
 *   <li>{@code onSettleFailure}: only thrown errors, e.g. no scheme registered for the network.
 *   <li>A before-hook abort (see {@link PayeePolicy}) skips the hooks of that call.
 * </ul>
 * Failure hooks may answer instead of failing by returning {@code Recovery.recovered(response)}.
 */
@Configuration
class AuditHooks {

  private static final Logger LOG = LoggerFactory.getLogger(AuditHooks.class);

  @Bean
  X402FacilitatorCustomizer auditLogging(FacilitatorStats stats) {
    return facilitator -> facilitator
        .onAfterVerify(ctx -> {
          stats.increment("verify.valid");
          LOG.info("VERIFIED {} {} to {}", ctx.requirements().amount(),
              ctx.requirements().asset(), ctx.requirements().payTo());
        })
        .onVerifyFailure(ctx -> {
          stats.increment("verify.invalid");
          LOG.info("VERIFY FAILED {}", ctx.error().getMessage());
          return HookResults.Recovery.none();
        })
        .onAfterSettle(ctx -> {
          stats.increment(ctx.result().success() ? "settle.success" : "settle.failed");
          LOG.info("SETTLED  tx={}: success={} reason={}", ctx.result().transaction(),
              ctx.result().success(), ctx.result().errorReason());
        })
        .onSettleFailure(ctx -> {
          // A thrown error, not a "no": e.g. no scheme for the network. Provider outages don't
          // land here; the Cardano scheme answers them with success=false.
          stats.increment("settle.error");
          LOG.warn("SETTLE ERROR {}", ctx.error().getMessage());
          return HookResults.Recovery.none();
        });
  }
}
