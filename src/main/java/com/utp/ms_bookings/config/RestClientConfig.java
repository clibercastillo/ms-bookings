package com.utp.ms_bookings.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Define clientes HTTP reutilizables para integrar servicios externos,
 * como el microservicio de canchas, con configuración vía propiedades.
 */
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
