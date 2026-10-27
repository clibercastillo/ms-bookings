package com.utp.ms_bookings.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.ms_bookings.dto.BookingRequest;
import com.utp.ms_bookings.dto.BookingResponse;
import com.utp.ms_bookings.entity.BookingStatus;
import com.utp.ms_bookings.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookingController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @Test
    void shouldCreateBooking() throws Exception {
        BookingRequest request = new BookingRequest();
        request.setStadiumId(10L);
        request.setBookingDate(LocalDate.now().plusDays(1));
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));

        BookingResponse response = new BookingResponse(
                1L,
                10L,
                "usuario@utp.edu.pe",
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime(),
                250.0,
                BookingStatus.PENDING
        );

        when(bookingService.create(any(BookingRequest.class), eq("token-123"))).thenReturn(response);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer token-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stadiumId").value(10))
                .andExpect(jsonPath("$.userEmail").value("usuario@utp.edu.pe"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(bookingService).create(any(BookingRequest.class), eq("token-123"));
    }

    @Test
    void shouldListMyBookings() throws Exception {
        BookingResponse response = new BookingResponse(
                2L,
                5L,
                "usuario@utp.edu.pe",
                LocalDate.now().plusDays(2),
                LocalTime.of(18, 0),
                LocalTime.of(19, 30),
                375.0,
                BookingStatus.CONFIRMED
        );

        when(bookingService.findMyBookings()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/bookings/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].stadiumId").value(5))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));

        verify(bookingService).findMyBookings();
    }

    @Test
    void shouldListBookingsByStadiumAndDate() throws Exception {
        LocalDate date = LocalDate.now().plusDays(3);
        BookingResponse response = new BookingResponse(
                3L,
                8L,
                "admin@utp.edu.pe",
                date,
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                180.0,
                BookingStatus.PENDING
        );

        when(bookingService.findByStadiumAndDate(8L, date)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/bookings/stadium/{stadiumId}", 8L)
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].stadiumId").value(8))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(bookingService).findByStadiumAndDate(8L, date);
    }

    @Test
    void shouldConfirmBooking() throws Exception {
        BookingResponse response = new BookingResponse(
                4L,
                12L,
                "usuario@utp.edu.pe",
                LocalDate.now().plusDays(4),
                LocalTime.of(15, 0),
                LocalTime.of(16, 0),
                200.0,
                BookingStatus.CONFIRMED
        );

        when(bookingService.confirm(4L)).thenReturn(response);

        mockMvc.perform(patch("/api/bookings/{id}/confirm", 4L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(bookingService).confirm(4L);
    }
}
