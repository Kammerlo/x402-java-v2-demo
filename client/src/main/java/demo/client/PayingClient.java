package demo.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.x402.core.client.SpendControls;
import org.x402.core.client.X402Client;
import org.x402.http.HostPolicy;
import org.x402.http.X402HttpClient;
import org.x402.mechanisms.cardano.chain.BlockfrostChainBackend;
import org.x402.mechanisms.cardano.exact.client.ExactCardanoClientScheme;
import org.x402.mechanisms.cardano.signer.ClientCardanoSignerConfig;
import org.x402.mechanisms.cardano.signer.MnemonicClientCardanoSigner;

/**
 * Pays for the demo's paid endpoints with plain java.net.http, no Spring. Both endpoints look the
 * same to the client, whether the server protects them with an annotation or with configuration.
 *
 * <p>Environment: BUYER_MNEMONIC (a funded preprod wallet) and BLOCKFROST_PROJECT_ID (preprod).
 * Arguments: the URLs to fetch (default: the weather and the report endpoints).
 */
public final class PayingClient {

  private static final String NETWORK = "cardano:preprod";

  private PayingClient() {}

  public static void main(String[] args) throws Exception {
    List<URI> urls = (args.length > 0 ? List.of(args) : List.of(
        "http://127.0.0.1:8080/api/weather/lisbon",
        "http://127.0.0.1:8080/api/reports/ada-usd")).stream().map(URI::create).toList();

    // 1. A wallet: the buyer's mnemonic, with Blockfrost to find its UTxOs.
    //    It signs payments offline; the facilitator is the one that broadcasts them.
    BlockfrostChainBackend blockfrost = new BlockfrostChainBackend(BlockfrostChainBackend.Config.builder()
        .network(NETWORK)
        .projectId(env("BLOCKFROST_PROJECT_ID"))
        .build());
    MnemonicClientCardanoSigner wallet = new MnemonicClientCardanoSigner(ClientCardanoSignerConfig.builder()
        .mnemonic(env("BUYER_MNEMONIC"))
        .network(NETWORK)
        .walletProvider(blockfrost)
        .build());

    // 2. The payment engine: the Cardano exact scheme for this network, and a spending limit.
    //    Spend controls refuse lovelace unless it is allowed: here up to 3 tADA per payment.
    X402Client payments = new X402Client()
        .setSpendControls(SpendControls.builder()
            .allowedAssets(List.of(new SpendControls.AllowedAsset(NETWORK, "lovelace", "3000000")))
            .build())
        .register(NETWORK, new ExactCardanoClientScheme(wallet));

    // 3. A normal JDK HttpClient, wrapped: on a 402 it builds a payment, retries once, and
    //    returns the paid response. HostPolicy: only this host may make us pay.
    HttpClient jdk = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    try (X402HttpClient http = X402HttpClient.wrap(jdk, payments,
        HostPolicy.of(List.of(urls.get(0).getHost())))) {
      for (int i = 0; i < urls.size(); i++) {
        URI url = urls.get(i);
        if (i > 0) {
          // The wallet may spend the change of the previous payment. Give the chain provider a
          // moment to index that block on all its nodes before building the next payment.
          Thread.sleep(20_000);
        }
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(url).GET().build(),
            HttpResponse.BodyHandlers.ofString());

        System.out.println("GET " + url.getPath() + " -> HTTP " + response.statusCode());
        System.out.println("  " + response.body());
        // The receipt from the PAYMENT-RESPONSE header.
        X402HttpClient.paymentResponse(response).ifPresent(receipt -> System.out.println(
            receipt.success()
                ? "  paid: https://preprod.cardanoscan.io/transaction/" + receipt.transaction()
                : "  not paid: " + receipt.errorReason()));
      }
    }
  }

  private static String env(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(name + " is not set (see .env.example)");
    }
    return value.strip();
  }
}
