# ms-bookings

Microservicio de reservas de canchas sintéticas. Valida tokens JWT emitidos por `ms-auth` y consulta `ms-stadium` para verificar disponibilidad y precio.

## Stack

- Java 17 · Spring Boot 4.1.0
- Spring Security (validación de JWT)
- Spring Data JPA + PostgreSQL
- RestClient (comunicación síncrona con `ms-stadium`)
- Swagger / OpenAPI (springdoc 3.0.3)
- Gradle
- Docker

## Requisitos

- JDK 17
- Docker (Postgres compartido con `ms-auth` y `ms-stadium`)
- `ms-auth` corriendo, para generar el token de login
- `ms-stadium` corriendo, para validar canchas al reservar

## Levantar local

```bash
docker compose up -d
./gradlew bootRun
```

App disponible en `http://localhost:8082`
Swagger UI en `http://localhost:8082/swagger-ui.html`

## Estados de una reserva

```
PENDING → CONFIRMED → COMPLETED
   ↓
CANCELLED
```

## Endpoints principales

| Método | Ruta                        | Descripción                          | Auth |
|--------|-----------------------------|----------------------------------------|------|
| POST   | `/api/bookings`             | Crear reserva                          | Sí   |
| GET    | `/api/bookings/mine`        | Listar mis reservas                    | Sí   |
| PATCH  | `/api/bookings/{id}/confirm`| Confirmar reserva (PENDING → CONFIRMED)| Sí   |
| PATCH  | `/api/bookings/{id}/cancel` | Cancelar reserva                       | Sí   |
| PATCH  | `/api/bookings/{id}/complete`| Completar reserva (CONFIRMED → COMPLETED)| Sí |

Rutas protegidas requieren header:
```
Authorization: Bearer <token>
```
El token se obtiene desde `ms-auth` (`POST /api/auth/login`).

## Variables de entorno

| Variable                     | Descripción                              |
|-------------------------------|-------------------------------------------|
| `SPRING_DATASOURCE_URL`      | URL de conexión a PostgreSQL              |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la BD                          |
| `SPRING_DATASOURCE_PASSWORD` | Password de la BD                         |
| `JWT_SECRET`                 | Clave secreta para validar JWT — debe ser idéntica a la de `ms-auth` y `ms-stadium` |
| `SERVICES_STADIUM_URL`       | URL base de `ms-stadium` (default: `http://localhost:8081`) |

## Docker

```bash
docker build -t ms-bookings:local .
docker run -p 8082:8082 ms-bookings:local
```

## CI/CD

El pipeline en `.github/workflows/ci.yml` compila el proyecto y publica la imagen en GitHub Container Registry (`ghcr.io`) en cada push a `main` o `develop`.
