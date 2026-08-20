package com.utp.ms_bookings.repository;

import com.utp.ms_bookings.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserEmail(String userEmail);

    List<Booking> findByStadiumIdAndBookingDate(Long stadiumId, LocalDate bookingDate);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.stadiumId = :stadiumId
        AND b.bookingDate = :date
        AND b.status <> 'CANCELLED'
        AND (:start < b.endTime AND :end > b.startTime)
    """)
    List<Booking> findOverlapping(
            @Param("stadiumId") Long stadiumId,
            @Param("date") LocalDate date,
            @Param("start") LocalTime start,
            @Param("end") LocalTime end
    );
}