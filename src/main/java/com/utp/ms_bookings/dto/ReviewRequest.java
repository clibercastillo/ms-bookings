package com.utp.ms_bookings.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Solicitud para registrar una reseña sobre una reserva completada.
 * Incluye la valoración, un comentario opcional y el nombre visible del usuario.
 */
@Getter
@Setter
public class ReviewRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 500)
    private String comment;

    @Size(max = 100)
    private String userName;
}
