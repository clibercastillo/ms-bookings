package com.utp.ms_bookings.dto;

import com.utp.ms_bookings.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private Long stadiumId;
    private String userEmail;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double totalPrice;
    private BookingStatus status;
}