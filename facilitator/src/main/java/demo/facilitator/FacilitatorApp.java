package demo.facilitator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The x402 facilitator. The starter builds it from application.yml:
 * X402FacilitatorServerAutoConfiguration creates one ExactCardanoFacilitatorScheme per network,
 * registers it in an X402Facilitator and serves it through FacilitatorHttpHandler.
 *
 * <p>The other classes in this package each show one way to adjust it:
 * <ul>
 *   <li>{@link AuditHooks}: observe every verify and settle (after-hooks).</li>
 *   <li>{@link PayeePolicy}: refuse payments to unknown payees (before-hooks).</li>
 *   <li>{@link ApiKeyFilter}: protect /verify and /settle with ordinary Spring code.</li>
 *   <li>{@link MeteredChainBackend}: replace the chain backend with your own.</li>
 *   <li>{@link StatsController}: expose what the hooks and the backend counted.</li>
 * </ul>
 */
@SpringBootApplication
public class FacilitatorApp {

  public static void main(String[] args) {
    SpringApplication.run(FacilitatorApp.class, args);
  }
}
