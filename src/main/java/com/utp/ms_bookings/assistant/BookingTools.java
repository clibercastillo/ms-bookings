package com.utp.ms_bookings.assistant;

import com.utp.ms_bookings.dto.BookingRequest;
import com.utp.ms_bookings.dto.BookingResponse;
import com.utp.ms_bookings.entity.BookingStatus;
import com.utp.ms_bookings.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Herramientas que el modelo puede invocar. Reutilizan BookingService, así que
 * pasan por las mismas validaciones que el endpoint normal (cancha habilitada, solapes, etc.).
 * El token y el usuario viajan en el ToolContext: el modelo nunca los ve.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingTools {

    static final String TOKEN_KEY = "token";
    static final String AUTH_KEY = "auth";
    static final String STATE_KEY = "state";

    private static final ZoneId ZONE = ZoneId.of("America/Lima");
    private static final LocalTime OPEN = LocalTime.of(7, 0);
    private static final LocalTime CLOSE = LocalTime.of(23, 0);
    private static final long MAX_MINUTES = 180;
    private static final int MAX_DAYS_AHEAD = 30;
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final BookingService bookingService;

    /** Estado de UNA petición: permite saber si el asistente llegó a crear una reserva. */
    public static final class CallState {
        final AtomicBoolean bookingCreated = new AtomicBoolean(false);
    }

    public record BookingResult(boolean success, String message, Long bookingId, String date,
                                String startTime, String endTime, Double totalPrice, String status) {
        static BookingResult error(String message) {
            return new BookingResult(false, message, null, null, null, null, null, null);
        }
    }

    public record DayAvailability(String date, String openingHours, List<String> occupiedRanges, String note) {}

    public record MyBooking(Long id, Long stadiumId, String date, String startTime, String endTime,
                            Double totalPrice, String status) {}

    @Tool(description = "Crea una reserva de cancha (queda PENDING) para el usuario actual. "
            + "Llamar solo cuando ya se conocen cancha, fecha, hora de inicio y hora de fin.")
    public BookingResult createBooking(
            @ToolParam(description = "Id de la cancha") Long stadiumId,
            @ToolParam(description = "Fecha en formato yyyy-MM-dd") String date,
            @ToolParam(description = "Hora de inicio en formato HH:mm de 24 horas, ej. 19:00") String startTime,
            @ToolParam(description = "Hora de fin en formato HH:mm de 24 horas, ej. 20:00") String endTime,
            ToolContext toolContext) {

        LocalDate day;
        LocalTime start;
        LocalTime end;
        try {
            day = LocalDate.parse(date.trim());
            start = LocalTime.parse(startTime.trim());
            end = LocalTime.parse(endTime.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            return BookingResult.error("Formato inválido: usa fecha yyyy-MM-dd y horas HH:mm (24h).");
        }

        // Estas reglas hoy solo existen en el frontend (home.ts); aquí se aplican también para el chat
        if (!end.isAfter(start)) {
            return BookingResult.error("La hora de fin debe ser mayor a la hora de inicio.");
        }
        if (start.isBefore(OPEN) || end.isAfter(CLOSE)) {
            return BookingResult.error("Atendemos de 07:00 a 23:00.");
        }
        if (start.getMinute() % 30 != 0 || end.getMinute() % 30 != 0) {
            return BookingResult.error("Los horarios van en bloques de 30 minutos (ej. 19:00, 19:30).");
        }
        if (Duration.between(start, end).toMinutes() > MAX_MINUTES) {
            return BookingResult.error("El máximo es 3 horas por reserva.");
        }
        LocalDateTime now = LocalDateTime.now(ZONE);
        if (!LocalDateTime.of(day, start).isAfter(now)) {
            return BookingResult.error("Ese horario ya pasó.");
        }
        if (day.isAfter(now.toLocalDate().plusDays(MAX_DAYS_AHEAD))) {
            return BookingResult.error("Solo se puede reservar hasta " + MAX_DAYS_AHEAD + " días adelante.");
        }

        BookingRequest request = new BookingRequest();
        request.setStadiumId(stadiumId);
        request.setBookingDate(day);
        request.setStartTime(start);
        request.setEndTime(end);

        try {
            BookingResponse saved = asUser(toolContext,
                    () -> bookingService.create(request, (String) toolContext.getContext().get(TOKEN_KEY)));

            Object state = toolContext.getContext().get(STATE_KEY);
            if (state instanceof CallState s) s.bookingCreated.set(true);

            return new BookingResult(true,
                    "Reserva creada en estado PENDING. Debe confirmarla pagando desde 'Mis reservas'.",
                    saved.getId(), saved.getBookingDate().toString(),
                    saved.getStartTime().format(HHMM), saved.getEndTime().format(HHMM),
                    saved.getTotalPrice(), saved.getStatus().name());

        } catch (IllegalArgumentException | IllegalStateException e) {
            // "Ya existe una reserva en ese horario", "La cancha no está disponible", etc.
            return BookingResult.error(e.getMessage());
        } catch (RestClientException e) {
            log.warn("ms-stadium no respondió al crear la reserva", e);
            return BookingResult.error("No pude consultar la cancha en este momento. Intenta de nuevo.");
        } catch (RuntimeException e) {
            log.error("Error inesperado creando reserva desde el asistente", e);
            return BookingResult.error("Ocurrió un error inesperado al crear la reserva.");
        }
    }

    @Tool(description = "Devuelve los horarios YA OCUPADOS de una cancha en una fecha (ignora canceladas). "
            + "Sirve para decir qué horarios hay libres o sugerir alternativas.")
    public DayAvailability getBookedSlots(
            @ToolParam(description = "Id de la cancha") Long stadiumId,
            @ToolParam(description = "Fecha en formato yyyy-MM-dd") String date,
            ToolContext toolContext) {

        String hours = OPEN.format(HHMM) + "-" + CLOSE.format(HHMM);
        LocalDate day;
        try {
            day = LocalDate.parse(date.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            return new DayAvailability(date, hours, List.of(), "Fecha inválida, usa yyyy-MM-dd.");
        }

        List<String> ranges = bookingService.findByStadiumAndDate(stadiumId, day).stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .sorted(Comparator.comparing(BookingResponse::getStartTime))
                .map(b -> b.getStartTime().format(HHMM) + "-" + b.getEndTime().format(HHMM))
                .toList();

        return new DayAvailability(day.toString(), hours, ranges,
                ranges.isEmpty() ? "Todo el día está libre." : "Fuera de esos rangos está libre.");
    }

    @Tool(description = "Lista las últimas reservas activas (no canceladas) del usuario actual.")
    public List<MyBooking> listMyBookings(ToolContext toolContext) {
        return asUser(toolContext, () -> bookingService.findMyBookings().stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .sorted(Comparator.comparing(BookingResponse::getBookingDate)
                        .thenComparing(BookingResponse::getStartTime).reversed())
                .limit(10)
                .map(b -> new MyBooking(b.getId(), b.getStadiumId(), b.getBookingDate().toString(),
                        b.getStartTime().format(HHMM), b.getEndTime().format(HHMM),
                        b.getTotalPrice(), b.getStatus().name()))
                .toList());
    }

    /**
     * BookingService lee el usuario desde SecurityContextHolder (ThreadLocal). Si Spring AI ejecutara la
     * herramienta en otro hilo, quedaría vacío: aquí se restaura el usuario que vino en el ToolContext.
     */
    private <T> T asUser(ToolContext toolContext, Supplier<T> action) {
        Authentication auth = (Authentication) toolContext.getContext().get(AUTH_KEY);
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext fresh = SecurityContextHolder.createEmptyContext();
        fresh.setAuthentication(auth);
        SecurityContextHolder.setContext(fresh);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}