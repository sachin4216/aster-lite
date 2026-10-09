package com.asterlite.appointment_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    // Plain builder, no load balancing. @Primary: anything that asks for "a RestClient.Builder"
    // without a qualifier gets this one. The Eureka client does exactly that for its own calls
    // to localhost:8761, and those must NOT go through the load balancer.
    @Bean
    @Primary
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    // @LoadBalanced tells Spring Cloud to add an interceptor to this builder. The interceptor treats
    // the host of every URL as a service name, asks Eureka for its instances and picks one.
    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    // The client PatientClient uses. @LoadBalanced on the parameter selects the second builder.
    // "patient-service" is the Eureka name, not a host: no host or port appears anywhere (criterion 2).
    // Both timeouts come from appointment-service.yml (APT-6 criterion 7).
    @Bean
    RestClient patientRestClient(@LoadBalanced RestClient.Builder builder,
                                 @Value("${patient-client.connect-timeout}") Duration connectTimeout,
                                 @Value("${patient-client.read-timeout}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // Maximum time to open the TCP connection.
        requestFactory.setConnectTimeout(connectTimeout);
        // Maximum time to wait for the response once connected. This is the 2 seconds of criterion 1.
        requestFactory.setReadTimeout(readTimeout);

        return builder.baseUrl("http://patient-service")
                .requestFactory(requestFactory)
                .build();
    }
}
