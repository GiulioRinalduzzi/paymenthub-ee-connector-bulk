package org.mifos.connector.phee.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CsvMapper extends ObjectMapper. Declaring it as a bean makes Spring Boot's ObjectMapper auto-configuration back off,
 * so without an explicit JSON mapper every {@code @Autowired ObjectMapper} in this connector receives the CSV one and
 * serialising a Map produces a CsvWriteException instead of JSON.
 *
 * <p>
 * The first test shows that failure with only the CsvMapper declared; the second shows the application's real
 * configuration getting JSON. Together they are the reason the {@code new ObjectMapper()} calls could be removed.
 */
class ObjectMapperBeanTest {

    private static final Map<String, Object> BODY = new LinkedHashMap<>(Map.of("payee", "0495822412"));

    @Configuration(proxyBeanMethods = false)
    static class OnlyCsvMapper {

        @Bean
        CsvMapper csvMapper() {
            return new CsvMapper();
        }
    }

    @Test
    void withoutAnExplicitJsonMapperTheInjectedOneWritesCsv() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withUserConfiguration(OnlyCsvMapper.class).run(context -> {
                    assertThat(context.getBean(ObjectMapper.class)).isInstanceOf(CsvMapper.class);
                });
    }

    @Test
    void theConnectorDeclaresAJsonMapperAndItIsTheOneInjected() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withUserConfiguration(JacksonConfig.class).run(context -> {
                    ObjectMapper injected = context.getBean(ObjectMapper.class);
                    assertThat(injected).isNotInstanceOf(CsvMapper.class);
                    assertThat(injected.writeValueAsString(BODY)).isEqualTo("{\"payee\":\"0495822412\"}");
                    assertThat(context.getBean(CsvMapper.class)).isNotNull();
                });
    }
}
