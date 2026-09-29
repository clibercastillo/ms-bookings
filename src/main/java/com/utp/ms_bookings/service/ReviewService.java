package com.utp.ms_bookings.service;

import com.utp.ms_bookings.dto.ReviewRequest;
import com.utp.ms_bookings.dto.ReviewResponse;
import com.utp.ms_bookings.entity.Booking;
import com.utp.ms_bookings.entity.BookingStatus;
import com.utp.ms_bookings.entity.Review;
import com.utp.ms_bookings.repository.BookingRepository;
import com.utp.ms_bookings.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    public ReviewResponse create(Long bookingId, ReviewRequest request) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        if (!booking.getUserEmail().equals(userEmail)) {
            throw new IllegalStateException("No tienes permiso sobre esta reserva");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Solo puedes reseñar reservas completadas");
        }
        if (reviewRepository.existsByBookingId(bookingId)) {
            throw new IllegalStateException("Esta reserva ya tiene una reseña");
        }

        String name = (request.getUserName() == null || request.getUserName().isBlank())
                ? userEmail.split("@")[0]
                : request.getUserName().trim();

        String comment = request.getComment() == null ? "" : request.getComment().trim();

        Review saved = reviewRepository.save(Review.builder()
                .bookingId(bookingId)
                .stadiumId(booking.getStadiumId())
                .userEmail(userEmail)
                .userName(name)
                .rating(request.getRating())
                .comment(comment.isEmpty() ? null : comment)
                .build());

        return toResponse(saved);
    }

    public List<ReviewResponse> findByStadium(Long stadiumId) {
        return reviewRepository.findByStadiumIdOrderByCreatedAtDesc(stadiumId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(r.getId(), r.getBookingId(), r.getStadiumId(),
                r.getUserName(), r.getRating(), r.getComment(), r.getCreatedAt());
    }
}