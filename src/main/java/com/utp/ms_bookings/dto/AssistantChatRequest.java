package com.utp.ms_bookings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Solicitud de conversación con el asistente de IA.
 * Contiene el texto del mensaje y un identificador único para mantener el hilo.
 */
@Getter
@Setter
public class AssistantChatRequest {

    @NotBlank
    @Size(max = 500)
    private String message;

    @NotBlank
    @Size(max = 100)
    private String conversationId;
}
