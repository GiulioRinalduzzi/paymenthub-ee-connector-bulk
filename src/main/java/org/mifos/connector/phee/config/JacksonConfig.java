package org.mifos.connector.phee.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * The two mappers this connector uses, declared in one place.
 *
 * <p>
 * CsvMapper extends ObjectMapper, so declaring it as a bean makes Spring Boot's own ObjectMapper auto-configuration
 * back off. Without the JSON mapper below, the only ObjectMapper in the context is the CSV one, and every
 * {@code @Autowired ObjectMapper} receives a mapper that writes CSV - serialising a Map then fails with
 * {@code CsvWriteException: Unrecognized column}. That is why the workers each built their own
 * {@code new ObjectMapper()}: it was a workaround, not duplication.
 *
 * <p>
 * Declaring the JSON mapper explicitly and marking it primary makes injection by type mean what it looks like it
 * means. The builder comes from Boot, so {@code spring.jackson.*} settings and registered modules still apply.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public CsvMapper csvMapper() {
        return new CsvMapper();
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.build();
    }
}
