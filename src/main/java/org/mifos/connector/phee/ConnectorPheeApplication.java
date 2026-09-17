package org.mifos.connector.phee;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.mifos.connector.phee.camel.config.HttpClientConfigurerTrustAllCACerts;
import org.springframework.boot.SpringApplication;
import org.mifos.connector.phee.config.BulkProcessorProperties;
import org.mifos.connector.phee.config.ChannelProperties;
import org.mifos.connector.phee.config.MockPaymentSchemaProperties;
import org.mifos.connector.phee.config.OperationsAppProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties({ OperationsAppProperties.class, MockPaymentSchemaProperties.class,
		BulkProcessorProperties.class, ChannelProperties.class })
public class ConnectorPheeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConnectorPheeApplication.class, args);
	}

	@Bean
	public CsvMapper csvMapper() {
		return new CsvMapper();
	}

	@Bean
	public HttpClientConfigurerTrustAllCACerts httpClientConfigurer() {
		return new HttpClientConfigurerTrustAllCACerts();
	}

}
