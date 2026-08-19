package com.utp.ms_bookings.service;

import com.utp.ms_bookings.config.RabbitConfig;
import com.utp.ms_bookings.dto.BookingEvent;
import com.utp.ms_bookings.entity.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(Booking booking) {
        BookingEvent event = new BookingEvent(
                booking.getId(), booking.getUserEmail(), booking.getStadiumId(),
                booking.getBookingDate(), booking.getStartTime(), booking.getStatus()
        );
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, event);
    }
}