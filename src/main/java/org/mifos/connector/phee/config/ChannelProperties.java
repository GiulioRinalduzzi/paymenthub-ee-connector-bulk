package org.mifos.connector.phee.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Everything under {@code channel}. Same property names as before. */
@ConfigurationProperties(prefix = "channel")
public record ChannelProperties(String contactpoint,
        @DefaultValue Endpoints endpoints) {

    public record Endpoints(String transfer) {}

    public String transferUrl() {
        return contactpoint + endpoints.transfer();
    }
}
