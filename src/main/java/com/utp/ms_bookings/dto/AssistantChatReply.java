package com.utp.ms_bookings.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Respuesta generada por el asistente de IA.
 * Incluye el texto final y un indicador para saber si creó una reserva.
 */
@Getter
@AllArgsConstructor
public class AssistantChatReply {
    private String reply;
    private boolean bookingCreated;
}
