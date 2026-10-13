package io.github.ericmschoi.theworldthatday.data;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import java.nio.file.Path;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Validates curated {@code data/} files against their JSON Schema 2020-12 in {@code data/schema/}
 * (plan M3a2 T4; M3a3 reuses it for the media files). Offline: the 2020-12 meta-schema comes from
 * the validator's own classpath copy, remote schema fetching is off, and {@code format} is asserted
 * (for example {@code date} and {@code uri}), not only annotated. Tests run in {@code server/}, so
 * the data folder is {@code ../data}.
 */
public final class DataSchemas {

  public static final Path DATA = Path.of("..", "data");
  public static final String META_SCHEMA = "https://json-schema.org/draft/2020-12/schema";

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final SchemaRegistry REGISTRY =
      SchemaRegistry.withDefaultDialect(
          SpecificationVersion.DRAFT_2020_12,
          builder ->
              builder
                  .schemaRegistryConfig(
                      SchemaRegistryConfig.builder().formatAssertionsEnabled(true).build())
                  .schemaLoader(loader -> loader.fetchRemoteResources(false)));

  private DataSchemas() {}

  /** A file under {@code data/}, e.g. {@code curation/event-tone.json}. */
  public static JsonNode read(String relative) {
    return JSON.readTree(DATA.resolve(relative).toFile());
  }

  /** The schema {@code data/schema/<name>}. */
  public static Schema schema(String name) {
    return REGISTRY.getSchema(read("schema/" + name));
  }

  /** Validation errors of {@code document}; empty when it conforms. */
  public static List<Error> errors(Schema schema, JsonNode document) {
    return schema.validate(document);
  }

  /** Validation errors of a schema document against the 2020-12 meta-schema. */
  public static List<Error> metaSchemaErrors(JsonNode schemaDocument) {
    return REGISTRY.getSchema(SchemaLocation.of(META_SCHEMA)).validate(schemaDocument);
  }
}
