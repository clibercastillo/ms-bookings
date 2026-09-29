package com.utp.ms_bookings.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = "booking_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "stadium_id", nullable = false)
    private Long stadiumId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "comment_text", length = 500)
    private String comment;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}