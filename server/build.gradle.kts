plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.10.4"
}

group = "io.github.ericmschoi"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    // CSV for the place dictionary (plan C2): the builder reads the SGC and CGNDB files, and C2b
    // reads data/places/. Boot 4.1 manages the version (Jackson 3 BOM).
    implementation("tools.jackson.dataformat:jackson-dataformat-csv")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    // Shaded standalone jar: its Jetty and Jackson 2 stay private, so nothing clashes with
    // Boot 4.1's Tomcat and Jackson 3.
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
    // JSON Schema 2020-12 validation of data/ files against data/schema/ (plan M3a2 T4, reused by
    // M3a3). 3.0.6 is the newest release built on Jackson 3.1 (tools.jackson), the line Boot 4.1
    // manages; 3.0.7+ need Jackson 3.2.
    testImplementation("com.networknt:json-schema-validator:3.0.6")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // An exported YOUTUBE_API_KEY would override the empty test key (relaxed binding).
    environment.remove("YOUTUBE_API_KEY")
}

// Plan C2: writes regions.csv and neighbourhoods.csv into -PplacesOut (which holds the curated
// overrides.csv) from the downloaded source files, never committed. Paths are relative to server/.
tasks.register<JavaExec>("buildPlaceDictionary") {
    description = "Builds data/places/ from the SGC 2021 and CGNDB CSV files (reads files only)."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "io.github.ericmschoi.theworldthatday.place.PlaceDictionaryBuilder"
    val names = listOf("sgcStructure", "sgcElements", "cgndb", "placesOut")
    val paths = names.map { providers.gradleProperty(it) }
    argumentProviders.add(CommandLineArgumentProvider {
        names.zip(paths).map { (name, path) -> file(path.orNull ?: throw GradleException("Missing -P$name")).path }
    })
}

spotless {
    java {
        // Spotless 8.10.4 default. 1.37.0 fails with this Spotless version (NoSuchMethodError).
        googleJavaFormat("1.36.1")
    }
    kotlinGradle {
        target("*.gradle.kts")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
