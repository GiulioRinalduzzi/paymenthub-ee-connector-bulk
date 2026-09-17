package org.mifos.connector.phee.config;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * One HTTP client per kind, built at startup instead of inside every Zeebe job.
 *
 * <p>
 * The workers used to build a RestTemplate - and two of them a whole pooled CloseableHttpClient - on every job, and
 * never closed them. batchSummary is a polling worker, so that repeated for every poll of every batch.
 *
 * <p>
 * There are deliberately two beans, so that each call site keeps exactly the client it had before. The workers that
 * talk to bulk-processor and the channel connector over https used a client that trusts every certificate and skips
 * hostname verification; batchDetails used a plain RestTemplate. Whether the trust-all default is right at all is a
 * separate question for the team - this change does not answer it, it only stops rebuilding the client per job.
 */
@Configuration
public class HttpClientConfig {

    /** What BatchDetailWorker used: a plain RestTemplate, default trust store. */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * The pool is shared now, so its limits have to be raised to match what the per-call clients did.
     *
     * <p>
     * A fresh pool per call meant the pool never limited anything: every call got its own connection. One shared pool
     * on the HttpClient 5 defaults would allow 5 connections per host and 25 in total, which is a limit that did not
     * exist before. The ceiling below is the Zeebe execution-thread count from application.yaml, so the pool stays out
     * of the way exactly as it did before, and the jobs remain the only thing bounding concurrency.
     */
    private static final int MAX_CONNECTIONS = 100;

    /** What BatchSummaryWorker and BatchTransferWorker used: trust every certificate, no hostname check. */
    @Bean("trustAllRestTemplate")
    public RestTemplate trustAllRestTemplate() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        CloseableHttpClient httpClient = HttpClients.custom()
                // HttpClient 5: TLS config moved onto the connection manager
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(new SSLConnectionSocketFactory(
                                new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build(),
                                NoopHostnameVerifier.INSTANCE))
                        .setMaxConnTotal(MAX_CONNECTIONS)
                        .setMaxConnPerRoute(MAX_CONNECTIONS)
                        .build())
                .build();
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
        return restTemplate;
    }
}
