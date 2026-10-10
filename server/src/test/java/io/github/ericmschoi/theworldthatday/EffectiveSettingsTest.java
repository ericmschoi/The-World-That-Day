package io.github.ericmschoi.theworldthatday;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.web.filter.AbstractRequestLoggingFilter;

/** Settings as the running application sees them, after Spring Boot applies application.yml. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EffectiveSettingsTest {

  @Autowired private EntityManagerFactory entityManagerFactory;
  @Autowired private ApplicationContext context;

  @Test
  void hibernateOnlyValidatesSchema() {
    assertThat(entityManagerFactory.getProperties())
        .containsEntry("hibernate.hbm2ddl.auto", "validate");
  }

  @Test
  void noRequestLoggingFilterIsRegistered() {
    // Test policy 5: no filter that logs request bodies or query strings.
    assertThat(context.getBeansOfType(AbstractRequestLoggingFilter.class)).isEmpty();
  }
}
