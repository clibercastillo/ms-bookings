package com.utp.ms_bookings.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${services.stadium.url}")
    private String stadiumUrl;

    @Bean
    public RestClient stadiumRestClient() {
        return RestClient.builder()
                .baseUrl(stadiumUrl)
                .build();
    }
}