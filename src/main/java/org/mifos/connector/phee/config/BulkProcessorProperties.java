package org.mifos.connector.phee.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Everything under {@code bulk-processor}. Same property names as before. */
@ConfigurationProperties(prefix = "bulk-processor")
public record BulkProcessorProperties(String contactpoint,
        @DefaultValue Endpoints endpoints) {

    public record Endpoints(String batchTransaction,
            String batchExecution) {}

    public String batchTransactionUrl() {
        return contactpoint + endpoints.batchTransaction();
    }

    public String batchExecutionUrl() {
        return contactpoint + endpoints.batchExecution();
    }
}
