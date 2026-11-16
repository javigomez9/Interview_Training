# Interview Training

Repo de prácticas de código para entrevistas técnicas (live coding).

## Ejercicios

### Motor de Amortización BNPL — Aplazo

Implementación en Java del ejercicio descrito en [aplazo_live_coding_challenge.md](aplazo_live_coding_challenge.md): un motor de cálculo puro (stateless, sin I/O) que genera el plan de pagos quincenales de una compra **Buy Now, Pay Later**.

Código fuente: [src/main/java/mx/aplazo/bnpl/schedule/](src/main/java/mx/aplazo/bnpl/schedule/)

#### API

```java
PaymentSchedule result = PaymentScheduleGenerator.generatePaymentSchedule(
        amount,           // BigDecimal, > 0
        installments,     // int, entre 2 y 12
        commissionRate,   // BigDecimal, >= 0 (ej. 0.10 = 10%)
        purchaseDate      // String, formato estricto "YYYY-MM-DD"
);
```

| Parámetro | Tipo | Regla |
| :--- | :--- | :--- |
| `amount` | `BigDecimal` | Debe ser `> 0`. |
| `installments` | `int` | Entero entre `2` y `12` inclusive. |
| `commissionRate` | `BigDecimal` | Debe ser `>= 0`. |
| `purchaseDate` | `String` | `YYYY-MM-DD` estricto y fecha de calendario válida (rechaza `2026-02-30`). |

Entradas inválidas lanzan `ScheduleValidationException`, con un `ErrorCode` (`INVALID_AMOUNT`, `INVALID_INSTALLMENTS`, `INVALID_COMMISSION_RATE`, `INVALID_PURCHASE_DATE`) que identifica la causa.

#### Reglas de negocio implementadas

- **Totales:** `totalCommission = amount * commissionRate`, `totalAmount = amount + totalCommission`, todo con 2 decimales exactos (`BigDecimal`, sin `float`/`double`).
- **Ajuste de centavos:** el residuo de dividir `totalAmount` entre `installments` se suma siempre a la cuota 1, de forma que las cuotas `2..N` sean idénticas y la suma total sea exactamente `totalAmount`.
- **Fechas:** la cuota 1 vence el día de la compra; cada cuota siguiente vence 15 días calendario después de la anterior, calculado con `java.time.LocalDate` (sin componente de hora/timezone).
- **Estados:** la cuota 1 nace `PAID`; las cuotas `2..N` nacen `PENDING`.

#### Modelo de salida

- `PaymentSchedule` — envelope con `totalAmount`, `totalCommission`, `installmentsCount` y la lista `schedule`.
- `Installment` — cada cuota, con `installmentNumber`, `dueDate`, `amount` y `status` (`InstallmentStatus`).

## Requisitos

- Java 17
- Maven

## Build y pruebas

```bash
mvn test
```

Las pruebas unitarias están en [src/test/java/mx/aplazo/bnpl/schedule/PaymentScheduleGeneratorTest.java](src/test/java/mx/aplazo/bnpl/schedule/PaymentScheduleGeneratorTest.java) y cubren división exacta, ajuste de centavos, comisiones con redondeo, cruces de mes/año en fechas y los casos de error por parámetros inválidos.
