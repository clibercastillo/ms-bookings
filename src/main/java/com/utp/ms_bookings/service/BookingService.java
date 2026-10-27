package com.utp.ms_bookings.service;

import com.utp.ms_bookings.client.StadiumClient;
import com.utp.ms_bookings.dto.*;
import com.utp.ms_bookings.entity.Booking;
import com.utp.ms_bookings.entity.BookingStatus;
import com.utp.ms_bookings.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio principal de negocio para la gestión de reservas.
 * Valida permisos, calcula precios, evita solapamientos y publica eventos del flujo de reservas.
 */
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

    public List<BookingResponse> findAll() {
        return bookingRepository.findAll().stream()
                .filter(booking -> booking.getStatus() != BookingStatus.CANCELLED)
                .map(this::toResponse)
                .collect(Collectors.toList());
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
                .filter(booking -> booking.getStatus() != BookingStatus.CANCELLED)
                .map(this::toResponse)
                .collect(Collectors.toList());
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

    public byte[] exportAllBookingsToExcel() {
        return exportBookingsToExcel(findAll());
    }

    public byte[] exportAllBookingsToExcel(LocalDate from, LocalDate to, Long stadiumId) {
        return exportBookingsToExcel(findAllForAdmin(from, to, stadiumId));
    }

    private byte[] exportBookingsToExcel(List<BookingResponse> bookings) {
        String[] headers = {
                "ID", "Cancha", "Usuario", "Fecha", "Hora inicio", "Hora fin",
                "Precio total", "Estado"
        };

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Reservas");

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            int rowIndex = 1;
            for (BookingResponse booking : bookings) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(booking.getId() != null ? booking.getId() : 0);
                row.createCell(1).setCellValue(booking.getStadiumId() != null ? booking.getStadiumId() : 0);
                row.createCell(2).setCellValue(booking.getUserEmail() != null ? booking.getUserEmail() : "");
                row.createCell(3).setCellValue(booking.getBookingDate() != null ? booking.getBookingDate().toString() : "");
                row.createCell(4).setCellValue(booking.getStartTime() != null ? booking.getStartTime().toString() : "");
                row.createCell(5).setCellValue(booking.getEndTime() != null ? booking.getEndTime().toString() : "");
                row.createCell(6).setCellValue(booking.getTotalPrice() != null ? booking.getTotalPrice() : 0.0);
                row.createCell(7).setCellValue(booking.getStatus() != null ? booking.getStatus().name() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el reporte Excel de reservas", e);
        }
    }
}

