package io.github.ericmschoi.theworldthatday;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TheWorldThatDayApplication {

  public static void main(String[] args) {
    SpringApplication.run(TheWorldThatDayApplication.class, args);
  }
}
