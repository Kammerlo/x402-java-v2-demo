package demo.facilitator;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/** Counters filled by the hooks and the chain backend, shown by {@link StatsController}. */
@Component
class FacilitatorStats {

  private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

  void increment(String name) {
    counters.computeIfAbsent(name, k -> new AtomicLong()).incrementAndGet();
  }

  Map<String, Long> snapshot() {
    Map<String, Long> copy = new TreeMap<>();
    counters.forEach((k, v) -> copy.put(k, v.get()));
    return copy;
  }
}
