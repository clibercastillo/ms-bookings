package com.utp.ms_bookings.dto;

import com.utp.ms_bookings.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Evento publicado cuando cambia el estado de una reserva.
 * Se utiliza para notificar a otros componentes del sistema mediante RabbitMQ.
 */
@Getter
@AllArgsConstructor
public class BookingEvent {
    private Long bookingId;
    private String userEmail;
    private Long stadiumId;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private BookingStatus status;
}
