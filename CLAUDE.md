# Interview Training

Repo de prácticas de código para entrevistas técnicas (live coding).

## Ejercicios

### Motor de Amortización BNPL — Aplazo
Implementación en Java del ejercicio descrito en `aplazo_live_coding_challenge.md`: motor de cálculo puro (stateless, sin I/O) que genera el plan de pagos quincenales de una compra Buy Now, Pay Later.
Código fuente: `src/main/java/mx/aplazo/bnpl/schedule/`

Reglas de negocio clave:
- Totales con `BigDecimal` a 2 decimales exactos (sin `float`/`double`); el residuo de centavos se suma siempre a la cuota 1.
- Cuota 1 vence el día de la compra; cada cuota siguiente vence 15 días calendario después, con `java.time.LocalDate`.
- Cuota 1 nace `PAID`; cuotas `2..N` nacen `PENDING`.
- Entradas inválidas lanzan `ScheduleValidationException` con un `ErrorCode` específico.

## Requisitos
- Java 17
- Maven

## Build y pruebas
`mvn test` — pruebas en `src/test/java/mx/aplazo/bnpl/schedule/PaymentScheduleGeneratorTest.java`
