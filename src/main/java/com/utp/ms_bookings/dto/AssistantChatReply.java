package com.utp.ms_bookings.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AssistantChatReply {
    private String reply;
    private boolean bookingCreated;
}