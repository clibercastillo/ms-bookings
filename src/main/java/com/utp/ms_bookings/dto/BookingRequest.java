package com.utp.ms_bookings.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Solicitud utilizada para crear una nueva reserva de cancha.
 * Define los datos mínimos necesarios para validar disponibilidad y costo.
 */
@Getter
@Setter
public class BookingRequest {

    @NotNull
    private Long stadiumId;

    @NotNull
    @FutureOrPresent
    private LocalDate bookingDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;
}
