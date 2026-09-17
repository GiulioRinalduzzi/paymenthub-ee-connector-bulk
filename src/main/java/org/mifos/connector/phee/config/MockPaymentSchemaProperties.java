package org.mifos.connector.phee.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Everything under {@code mock-payment-schema}. Same property names as before. */
@ConfigurationProperties(prefix = "mock-payment-schema")
public record MockPaymentSchemaProperties(
        String contactpoint,
        @DefaultValue Endpoints endpoints) {

    public record Endpoints(String batchSummary,
            String batchDetail) {}

    public String batchSummaryUrl() {
        return contactpoint + endpoints.batchSummary();
    }

    public String batchDetailUrl() {
        return contactpoint + endpoints.batchDetail();
    }
}
