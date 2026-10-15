package demo.facilitator;

import com.bloxbean.cardano.client.api.model.ProtocolParams;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.x402.mechanisms.cardano.chain.BlockfrostChainBackend;
import org.x402.mechanisms.cardano.chain.CardanoChainBackend;
import org.x402.mechanisms.cardano.chain.SettlementEvidence;
import org.x402.mechanisms.cardano.chain.SubmissionResult;
import org.x402.mechanisms.cardano.chain.UtxoSnapshot;
import org.x402.spring.facilitator.X402FacilitatorServerProperties;

/**
 * Customization 4, replace a building block: the chain backend. A CardanoChainBackend bean takes
 * the place of the Blockfrost backend the YAML would build. The same works for the
 * SettlementClaimStore (a database instead of memory) and, with several networks, through an
 * X402CardanoNetworkBeans bean that returns a backend per network.
 *
 * <p>This one wraps Blockfrost and counts the calls. A real one could talk to your own node,
 * Ogmios or Koios. When you wrap, delegate <b>every</b> method, the default ones too: a default
 * you forget silently switches a capability off (evidence lookups, phase-1 validation, the
 * "never sent" check the facilitator uses to retry safely).
 *
 * <p>A backend you build yourself also gets none of the settings the YAML path would apply. The
 * Blockfrost provider checks need {@code networkMagic} (is this really preprod?) and
 * {@code maxTipAge} (is the provider's index fresh?). Without them, a lagging index could report a
 * spent input as unspent. So the registration below sets them as the YAML path does.
 */
final class MeteredChainBackend implements CardanoChainBackend {

  private final CardanoChainBackend delegate;
  private final FacilitatorStats stats;

  MeteredChainBackend(CardanoChainBackend delegate, FacilitatorStats stats) {
    this.delegate = delegate;
    this.stats = stats;
  }

  @Configuration
  static class Registration {
    @Bean
    CardanoChainBackend meteredChainBackend(
        @Value("${demo.blockfrost-project-id}") String projectId,
        X402FacilitatorServerProperties properties, ObjectProvider<Clock> clock,
        FacilitatorStats stats) {
      BlockfrostChainBackend blockfrost = new BlockfrostChainBackend(
          BlockfrostChainBackend.Config.builder()
              // The network the YAML configures (this demo has one).
              .network(properties.cardano().networks().get(0).network())
              .projectId(projectId)
              .networkMagic(1)                                    // preprod's; mainnet 764824073
              .maxTipAge(properties.cardano().maxTipAge())        // default 5m
              .clock(clock.getIfAvailable(Clock::systemUTC))
              .build());
      return new MeteredChainBackend(blockfrost, stats);
    }
  }

  @Override
  public UtxoSnapshot getUtxo(String ref, String network) {
    stats.increment("chain.getUtxo");
    return delegate.getUtxo(ref, network);
  }

  @Override
  public long getCurrentSlot(String network) {
    stats.increment("chain.getCurrentSlot");
    return delegate.getCurrentSlot(network);
  }

  @Override
  public SubmissionResult submitTransaction(String signedTransactionBase64, String network) {
    stats.increment("chain.submitTransaction");
    return delegate.submitTransaction(signedTransactionBase64, network);
  }

  @Override
  public SettlementEvidence getTransactionEvidence(String txHash, String network) {
    stats.increment("chain.getTransactionEvidence");
    return delegate.getTransactionEvidence(txHash, network);
  }

  // The rest is passed straight through.

  @Override
  public ProtocolParams getProtocolParameters(String network) {
    return delegate.getProtocolParameters(network);
  }

  @Override
  public boolean isDefinitiveSubmissionRejection(Throwable error) {
    return delegate.isDefinitiveSubmissionRejection(error);
  }

  @Override
  public void evaluateTransaction(String signedTransactionBase64, String network) {
    delegate.evaluateTransaction(signedTransactionBase64, network);
  }

  @Override
  public boolean isSubmissionNeverSent(Throwable error) {
    return delegate.isSubmissionNeverSent(error);
  }

  @Override
  public long getIndexedSlot(String network) {
    return delegate.getIndexedSlot(network);
  }

  @Override
  public void checkHealth(String network) {
    delegate.checkHealth(network);
  }

  @Override
  public boolean supportsTransactionEvidence() {
    return delegate.supportsTransactionEvidence();
  }

  @Override
  public boolean supportsPhase1Validation() {
    return delegate.supportsPhase1Validation();
  }

  @Override
  public void validatePhase1Transaction(String signedTransactionBase64, String network) {
    delegate.validatePhase1Transaction(signedTransactionBase64, network);
  }
}
