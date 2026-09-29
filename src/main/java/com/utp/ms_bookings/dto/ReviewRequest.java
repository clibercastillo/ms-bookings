package com.utp.ms_bookings.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

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