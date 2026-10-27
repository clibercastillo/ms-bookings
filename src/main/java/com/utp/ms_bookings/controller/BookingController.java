package com.utp.ms_bookings.controller;

import com.utp.ms_bookings.dto.*;
import com.utp.ms_bookings.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST para gestionar reservas de canchas.
 * Expone operaciones para crear, confirmar, cancelar, completar y consultar
 * reservas por usuario y por canal de administración.
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Reservas de canchas")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Crear reserva")
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request,
                                                    HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization").substring(7);
        return ResponseEntity.ok(bookingService.create(request, token));
    }

    @GetMapping("/mine")
    @Operation(summary = "Listar mis reservas")
    public ResponseEntity<List<BookingResponse>> myBookings() {
        return ResponseEntity.ok(bookingService.findMyBookings());
    }

    @GetMapping("/all")
    @Operation(summary = "Listar todas las reservas")
    public ResponseEntity<List<BookingResponse>> allBookings() {
        return ResponseEntity.ok(bookingService.findAll());
    }

    @GetMapping("/stadium/{stadiumId}")
    @Operation(summary = "Listar reservas de una cancha en una fecha (para calendario admin)")
    public ResponseEntity<List<BookingResponse>> byStadiumAndDate(
            @PathVariable Long stadiumId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(bookingService.findByStadiumAndDate(stadiumId, date));
    }

    @PatchMapping("/{id}/confirm")
    @Operation(summary = "Confirmar reserva (PENDING → CONFIRMED)")
    public ResponseEntity<BookingResponse> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.confirm(id));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancelar reserva")
    public ResponseEntity<BookingResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancel(id));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Completar reserva (CONFIRMED → COMPLETED)")
    public ResponseEntity<BookingResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.complete(id));
    }

    @GetMapping("/admin")
    @Operation(summary = "Listar reservas (PENDING/CONFIRMED/COMPLETED) por rango de fechas — solo ADMIN")
    public ResponseEntity<List<BookingResponse>> adminFindAll(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long stadiumId) {
        return ResponseEntity.ok(bookingService.findAllForAdmin(from, to, stadiumId));
    }

    @GetMapping("/admin/export")
    @Operation(summary = "Exportar reservas a Excel por rango de fechas")
    public ResponseEntity<byte[]> exportAdminExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long stadiumId) {

        byte[] excel = bookingService.exportAllBookingsToExcel(from, to, stadiumId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(org.springframework.http.ContentDisposition
                .attachment()
                .filename("reservas.xlsx")
                .build());

        return new ResponseEntity<>(excel, headers, HttpStatus.OK);
    }
}

