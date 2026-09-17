package org.mifos.connector.phee.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * The first tests in this repository.
 *
 * <p>
 * They cover the thing the properties records are for: the connector must refuse to start when an outbound address is
 * missing, instead of starting healthy and failing later during a batch. They also pin the property names, because the
 * operator sets them as environment variables and a rename would silently break a deployment.
 */
class ConfigurationPropertiesTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({ OperationsAppProperties.class, MockPaymentSchemaProperties.class,
            BulkProcessorProperties.class, ChannelProperties.class })
    static class TestConfig {}

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class)).withUserConfiguration(TestConfig.class);

    /** The values application.yaml ships with. */
    private static String[] validConfig() {
        return new String[] { "operations-app.contactpoint=https://ops-bk.mifos.gazelle.test",
            "operations-app.endpoints.auth=/oauth/token", "operations-app.endpoints.batch-summary=/api/v1/batch",
            "operations-app.endpoints.batch-detail=/api/v1/batch/detail",
            "mock-payment-schema.contactpoint=http://paymenthub-ee-connector-mock-payment-schema:8080",
            "mock-payment-schema.endpoints.batch-summary=/mockapi/v1/batch/summary",
            "mock-payment-schema.endpoints.batch-detail=/mockapi/v1/batch/detail",
            "bulk-processor.contactpoint=https://paymenthub-ee-bulk-processor:8443",
            "bulk-processor.endpoints.batch-transaction=/batchtransactions",
            "bulk-processor.endpoints.batch-execution=/batchtransactions/execution",
            "channel.contactpoint=https://paymenthub-ee-connector-channel:8443", "channel.endpoints.transfer=/channel/transfer" };
    }

    @Test
    void urlsAreComposedTheWayTheOldPostConstructDidIt() {
        runner.withPropertyValues(validConfig()).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(OperationsAppProperties.class).batchSummaryUrl())
                    .isEqualTo("https://ops-bk.mifos.gazelle.test/api/v1/batch");
            assertThat(context.getBean(OperationsAppProperties.class).batchDetailUrl())
                    .isEqualTo("https://ops-bk.mifos.gazelle.test/api/v1/batch/detail");
            assertThat(context.getBean(MockPaymentSchemaProperties.class).batchDetailUrl())
                    .isEqualTo("http://paymenthub-ee-connector-mock-payment-schema:8080/mockapi/v1/batch/detail");
            assertThat(context.getBean(BulkProcessorProperties.class).batchTransactionUrl())
                    .isEqualTo("https://paymenthub-ee-bulk-processor:8443/batchtransactions");
            assertThat(context.getBean(BulkProcessorProperties.class).batchExecutionUrl())
                    .isEqualTo("https://paymenthub-ee-bulk-processor:8443/batchtransactions/execution");
            assertThat(context.getBean(ChannelProperties.class).transferUrl())
                    .isEqualTo("https://paymenthub-ee-connector-channel:8443/channel/transfer");
        });
    }

    @Test
    void anEmptyOutboundHostStopsStartupAndNamesTheProperty() {
        runner.withPropertyValues(validConfig()).withPropertyValues("bulk-processor.contactpoint=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("bulk-processor.contactpoint must be set");
        });
    }

    @Test
    void aDeletedSectionStopsStartupInsteadOfLeavingTheGroupNull() {
        // every channel.* property removed: without @DefaultValue on the nested group this is a
        // NullPointerException somewhere later instead of a message naming the property
        runner.withPropertyValues("operations-app.contactpoint=https://ops-bk.mifos.gazelle.test",
                "operations-app.endpoints.auth=/oauth/token", "operations-app.endpoints.batch-summary=/api/v1/batch",
                "operations-app.endpoints.batch-detail=/api/v1/batch/detail",
                "mock-payment-schema.contactpoint=http://mock:8080",
                "mock-payment-schema.endpoints.batch-summary=/s", "mock-payment-schema.endpoints.batch-detail=/d",
                "bulk-processor.contactpoint=https://bp:8443", "bulk-processor.endpoints.batch-transaction=/t",
                "bulk-processor.endpoints.batch-execution=/e").run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("channel.contactpoint must be set");
                });
    }

    @Test
    void theEnvironmentVariableSpellingTheOperatorUsesStillBinds() {
        // the deployment sets OPERATIONS_APP_CONTACTPOINT and OPERATIONS_APP_ENDPOINTS_BATCH_SUMMARY as real
        // environment variables, so the binding has to go through a SystemEnvironmentPropertySource to mean anything
        Map<String, Object> fromTheOperator = new HashMap<>();
        fromTheOperator.put("OPERATIONS_APP_CONTACTPOINT", "https://from-the-operator:80");
        fromTheOperator.put("OPERATIONS_APP_ENDPOINTS_BATCH_SUMMARY", "/api/v2/batch");

        runner.withPropertyValues(validConfig())
                .withInitializer(context -> context.getEnvironment().getPropertySources()
                        .addFirst(new SystemEnvironmentPropertySource(
                                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, fromTheOperator)))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    OperationsAppProperties properties = context.getBean(OperationsAppProperties.class);
                    assertThat(properties.contactpoint()).isEqualTo("https://from-the-operator:80");
                    assertThat(properties.batchSummaryUrl()).isEqualTo("https://from-the-operator:80/api/v2/batch");
                });
    }
}
