package io.github.ericmschoi.theworldthatday.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Application settings. {@code dataDir} is the curated data folder ({@code data/} of the
 * repository; the default {@code ../data} fits {@code bootRun} and tests, which run in {@code
 * server/}). {@code webOrigin} is the address the web app is served from (scheme, host and optional
 * port, no path, query or fragment); it is the {@code origin} of embedded players (plan M3a5). A
 * malformed value fails startup with the setting's name in the message.
 */
@ConfigurationProperties("app")
public record AppProperties(
    @DefaultValue("../data") String dataDir,
    @DefaultValue("http://localhost:5173") String webOrigin) {

  @ConstructorBinding
  public AppProperties {
    if (!isOrigin(webOrigin)) {
      throw new IllegalArgumentException(
          "app.web-origin must be http(s)://host[:port] without a path, got: " + webOrigin);
    }
  }

  /** The data folder with the default web origin. */
  public AppProperties(String dataDir) {
    this(dataDir, "http://localhost:5173");
  }

  private static boolean isOrigin(String value) {
    if (value == null || value.isEmpty()) {
      return false;
    }
    try {
      URI uri = new URI(value);
      return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
          && uri.getHost() != null
          && uri.getUserInfo() == null
          && (uri.getRawPath() == null || uri.getRawPath().isEmpty())
          && uri.getRawQuery() == null
          && uri.getRawFragment() == null;
    } catch (URISyntaxException e) {
      return false;
    }
  }

  /**
   * The folder as a file system path, relative to the working directory. Bound as a string: in a
   * web application Spring would resolve a {@code Path} against the servlet context instead.
   */
  public Path dataPath() {
    return Path.of(dataDir);
  }
}
