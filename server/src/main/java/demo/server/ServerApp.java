package demo.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The paid resource server. The starter reads application.yml, builds an X402ResourceServer with
 * the Cardano scheme, syncs /supported with the facilitator at startup, and puts
 * X402ServletFilter in front of the controllers. The filter decides per request whether a
 * payment is due: through an @X402Payment annotation, or through an offer's `path:`.
 */
@SpringBootApplication
public class ServerApp {

  public static void main(String[] args) {
    SpringApplication.run(ServerApp.class, args);
  }
}
