package io.github.ericmschoi.theworldthatday.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The single source of "now". Tests replace it to move time without sleeping. */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

  /** Dates in the product are Korean calendar dates. */
  public static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

  @Bean
  Clock clock() {
    return Clock.system(SERVICE_ZONE);
  }
}
