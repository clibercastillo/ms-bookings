package com.utp.ms_bookings.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Respuesta que expone una reseña publicada por un cliente del sistema.
 */
@Getter
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long bookingId;
    private Long stadiumId;
    private String userName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
