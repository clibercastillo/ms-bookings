package com.utp.ms_bookings.repository;

import com.utp.ms_bookings.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByStadiumIdOrderByCreatedAtDesc(Long stadiumId);

    boolean existsByBookingId(Long bookingId);
}