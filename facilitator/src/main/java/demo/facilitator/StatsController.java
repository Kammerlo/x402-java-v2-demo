package demo.facilitator;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Customization 5, add your own endpoints: the facilitator is an ordinary Spring Boot app. */
@RestController
class StatsController {

  private final FacilitatorStats stats;

  StatsController(FacilitatorStats stats) {
    this.stats = stats;
  }

  /** What the hooks and the chain backend counted since startup. */
  @GetMapping("/stats")
  Map<String, Long> stats() {
    return stats.snapshot();
  }
}
