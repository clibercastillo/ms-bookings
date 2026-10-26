package com.utp.ms_bookings.controller;

import com.utp.ms_bookings.assistant.AssistantService;
import com.utp.ms_bookings.dto.AssistantChatReply;
import com.utp.ms_bookings.dto.AssistantChatRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint para interactuar con el asistente de reservas con IA.
 * Permite enviar mensajes al modelo y recibir respuestas contextualizadas
 * con la lógica de negocio del sistema de reservas.
 */
@RestController
@RequestMapping("/api/bookings/assistant")
@RequiredArgsConstructor
@Tag(name = "Assistant", description = "Asistente de reservas con IA")
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/chat")
    @Operation(summary = "Conversar con el asistente (puede crear reservas)")
    public ResponseEntity<AssistantChatReply> chat(@Valid @RequestBody AssistantChatRequest request,
                                                   HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization").substring(7);
        return ResponseEntity.ok(assistantService.chat(request, token));
    }
}
