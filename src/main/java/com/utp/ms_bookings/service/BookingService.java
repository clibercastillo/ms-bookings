package com.utp.ms_bookings.service;

import com.utp.ms_bookings.client.StadiumClient;
import com.utp.ms_bookings.dto.*;
import com.utp.ms_bookings.entity.Booking;
import com.utp.ms_bookings.entity.BookingStatus;
import com.utp.ms_bookings.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final StadiumClient stadiumClient;
    private final BookingEventPublisher eventPublisher;

    public BookingResponse create(BookingRequest request, String token) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("La hora de fin debe ser mayor a la hora de inicio");
        }

        var stadium = stadiumClient.getStadium(request.getStadiumId(), token);
        if (!stadium.isEnabled()) {
            throw new IllegalArgumentException("La cancha no está disponible");
        }

        List<Booking> overlapping = bookingRepository.findOverlapping(
                request.getStadiumId(), request.getBookingDate(),
                request.getStartTime(), request.getEndTime()
        );
        if (!overlapping.isEmpty()) {
            throw new IllegalStateException("Ya existe una reserva en ese horario");
        }

        double hours = Duration.between(request.getStartTime(), request.getEndTime()).toMinutes() / 60.0;
        double total = hours * stadium.getPricePerHour();

        Booking booking = Booking.builder()
                .stadiumId(request.getStadiumId())
                .userEmail(userEmail)
                .bookingDate(request.getBookingDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .totalPrice(total)
                .status(BookingStatus.PENDING)
                .build();

        Booking saved = bookingRepository.save(booking);
        eventPublisher.publish(saved);
        return toResponse(saved);
    }

    public List<BookingResponse> findMyBookings() {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return bookingRepository.findByUserEmail(userEmail).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public BookingResponse confirm(Long id) {
        return changeStatus(id, BookingStatus.PENDING, BookingStatus.CONFIRMED);
    }

    public BookingResponse cancel(Long id) {
        Booking booking = getOwnedBooking(id);
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("No se puede cancelar una reserva completada");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);
        eventPublisher.publish(saved);
        return toResponse(saved);
    }

    public BookingResponse complete(Long id) {
        return changeStatus(id, BookingStatus.CONFIRMED, BookingStatus.COMPLETED);
    }

    private BookingResponse changeStatus(Long id, BookingStatus expected, BookingStatus next) {
        Booking booking = getOwnedBooking(id);
        if (booking.getStatus() != expected) {
            throw new IllegalStateException(
                    "No se puede pasar de " + booking.getStatus() + " a " + next);
        }
        booking.setStatus(next);
        Booking saved = bookingRepository.save(booking);
        eventPublisher.publish(saved);
        return toResponse(saved);
    }

    private Booking getOwnedBooking(Long id) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
        if (!booking.getUserEmail().equals(userEmail)) {
            throw new IllegalStateException("No tienes permiso sobre esta reserva");
        }
        return booking;
    }

    private BookingResponse toResponse(Booking b) {
        return new BookingResponse(
                b.getId(), b.getStadiumId(), b.getUserEmail(), b.getBookingDate(),
                b.getStartTime(), b.getEndTime(), b.getTotalPrice(), b.getStatus()
        );
    }

    public List<BookingResponse> findByStadiumAndDate(Long stadiumId, LocalDate date) {
        return bookingRepository.findByStadiumIdAndBookingDate(stadiumId, date).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<BookingResponse> findAllForAdmin(LocalDate from, LocalDate to, Long stadiumId) {
        List<Booking> bookings = (stadiumId != null)
                ? bookingRepository.findByBookingDateBetweenAndStadiumId(from, to, stadiumId)
                : bookingRepository.findByBookingDateBetween(from, to);

        return bookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}