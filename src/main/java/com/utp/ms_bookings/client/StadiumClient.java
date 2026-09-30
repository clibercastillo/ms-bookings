package com.utp.ms_bookings.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
@Component
@RequiredArgsConstructor
public class StadiumClient {

    private final RestClient stadiumRestClient;

    public StadiumInfo getStadium(Long stadiumId, String token) {
        try {
            return stadiumRestClient.get()
                    .uri("/api/stadiums/{id}", stadiumId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(StadiumInfo.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException("Cancha no encontrada: " + stadiumId);
        }
    }

    public List<StadiumInfo> listStadiums(String token) {
        List<StadiumInfo> list = stadiumRestClient.get()
                .uri("/api/stadiums")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<List<StadiumInfo>>() {});
        return list != null ? list : List.of();
    }

    @Getter
    public static class StadiumInfo {
        private Long id;
        private String name;
        private Double pricePerHour;
        private boolean enabled;
    }
}