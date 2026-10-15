package demo.server;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.x402.spring.annotation.X402Payment;

/**
 * Option A: the annotation. {@code @X402Payment("weather")} attaches the offer
 * {@code x402.routes.weather} to this handler method. The offer follows the method: renaming the
 * URL in {@code @GetMapping} keeps it paid, and an unknown offer name fails startup.
 */
@RestController
class WeatherController {

  /** Free: no annotation and no matching {@code path:}, so the filter lets it through. */
  @GetMapping("/api/status")
  Map<String, Object> status() {
    return Map.of("status", "ok", "time", Instant.now().toString());
  }

  /** Paid: 1 tADA per request (offer "weather" in application.yml). */
  @GetMapping("/api/weather/{city}")
  @X402Payment("weather")
  Map<String, Object> weather(@PathVariable String city) {
    return Map.of(
        "city", city,
        "temperatureC", 18.4,
        "humidityPercent", 72,
        "windKmh", 14,
        "observedAt", Instant.now().toString());
  }
}
