package com.utp.ms_bookings;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal del microservicio de reservas.
 * Arranca la aplicación Spring Boot y habilita la configuración de la API
 * para gestionar reservas, reseñas y asistentes de IA.
 */
@SpringBootApplication
public class MsBookingsApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsBookingsApplication.class, args);
	}

}
