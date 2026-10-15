package demo.server;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Option B: no annotation. This controller has no x402 code at all. The offer
 * {@code x402.routes.market-report} declares {@code path: "GET /api/reports/:market"}, so
 * X402ServletFilter charges every matching request before it reaches this method. Use this to
 * charge for endpoints you can't or don't want to annotate (another team's controller, a
 * static resource, a proxy).
 */
@RestController
class ReportController {

  /** Paid: 2 tADA per request, configured only in application.yml. */
  @GetMapping("/api/reports/{market}")
  Map<String, Object> report(@PathVariable String market) {
    return Map.of(
        "market", market,
        "date", LocalDate.now().toString(),
        "close", 0.4275,
        "changePercent", -1.8,
        "volume", 312_450_000,
        "topMovers", List.of("ADA/USD", "ADA/EUR"));
  }
}
