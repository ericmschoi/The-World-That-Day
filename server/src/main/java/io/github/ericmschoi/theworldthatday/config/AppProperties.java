package io.github.ericmschoi.theworldthatday.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Application settings. {@code dataDir} is the curated data folder ({@code data/} of the
 * repository; the default {@code ../data} fits {@code bootRun} and tests, which run in {@code
 * server/}).
 */
@ConfigurationProperties("app")
public record AppProperties(@DefaultValue("../data") String dataDir) {

  /**
   * The folder as a file system path, relative to the working directory. Bound as a string: in a
   * web application Spring would resolve a {@code Path} against the servlet context instead.
   */
  public Path dataPath() {
    return Path.of(dataDir);
  }
}
