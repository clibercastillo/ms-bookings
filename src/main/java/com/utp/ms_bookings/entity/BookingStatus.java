package com.utp.ms_bookings.entity;

/**
 * Estados posibles que puede tomar una reserva dentro del flujo de negocio.
 * Desde la creación hasta la finalización o cancelación.
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}
