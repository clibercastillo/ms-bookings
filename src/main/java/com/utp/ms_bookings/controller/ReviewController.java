package com.utp.ms_bookings.controller;

import com.utp.ms_bookings.dto.ReviewRequest;
import com.utp.ms_bookings.dto.ReviewResponse;
import com.utp.ms_bookings.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Reseñas de canchas")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/{bookingId}/review")
    @Operation(summary = "Crear reseña de una reserva completada")
    public ResponseEntity<ReviewResponse> create(@PathVariable Long bookingId,
                                                 @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.create(bookingId, request));
    }

    @GetMapping("/reviews/stadium/{stadiumId}")
    @Operation(summary = "Listar reseñas de una cancha (público)")
    public ResponseEntity<List<ReviewResponse>> byStadium(@PathVariable Long stadiumId) {
        return ResponseEntity.ok(reviewService.findByStadium(stadiumId));
    }
}