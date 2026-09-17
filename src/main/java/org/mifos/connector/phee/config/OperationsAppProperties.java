package org.mifos.connector.phee.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Everything under {@code operations-app}.
 *
 * <p>
 * The property names are exactly the ones that were on the {@code @Value} annotations before, because the operator sets
 * them as environment variables ({@code OPERATIONS_APP_CONTACTPOINT},
 * {@code OPERATIONS_APP_ENDPOINTS_BATCH_SUMMARY} and so on). Renaming one would silently break a deployment.
 */
@ConfigurationProperties(prefix = "operations-app")
public record OperationsAppProperties(String contactpoint,
        String username, String password, @DefaultValue Endpoints endpoints) {

    public record Endpoints(String auth,
            String batchSummary,
            String batchDetail) {}

    public String batchSummaryUrl() {
        return contactpoint + endpoints.batchSummary();
    }

    public String batchDetailUrl() {
        return contactpoint + endpoints.batchDetail();
    }

    public String authUrl() {
        return contactpoint + endpoints.auth();
    }
}
