package com.utp.ms_bookings.controller;

import com.utp.ms_bookings.dto.*;
import com.utp.ms_bookings.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}