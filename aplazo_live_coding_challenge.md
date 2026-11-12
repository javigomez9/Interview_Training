# Ejercicio Técnico de Live Coding — Aplazo (BNPL)
## Motor de Amortización y Cálculo de Comisiones (`generatePaymentSchedule`)

Este documento consolida los requerimientos funcionales, reglas de negocio, definiciones arquitectónicas, manejo de errores y casos borde discutidos durante la sesión de preparación para la entrevista técnica en **Aplazo**.

---

## 1. Contexto del Problema y Arquitectura

En un modelo **Buy Now, Pay Later (BNPL)** como el de Aplazo, cuando un usuario realiza una compra en un comercio aliado, el monto total se divide en pagos quincenales (generalmente 5 pagos: 1 inicial al momento de la compra y 4 pagos posteriores cada 15 días).

### Límites de Responsabilidad del Módulo
* **Función Pura (Stateless):** `generatePaymentSchedule` es un motor de cálculo puro en memoria. No debe realizar llamadas HTTP, consultas a bases de datos ni aplicar caché.
* **Flujo en Producción:** 
  1. Un **Orquestador de Checkout / Loan Origination Service** consulta previamente el perfil crediticio del usuario (a través de un servicio interno de *Underwriting/Credit Risk* o burós externos en México como *Buró de Crédito* / *Círculo de Crédito*) y las reglas del comercio (*Merchant*).
  2. Una vez resuelta la oferta crediticia (`commissionRate` e `installments`), el orquestador invoca a `generatePaymentSchedule`.
  3. Solo cuando el usuario confirma la compra y se procesa el pago inicial, el orquestador persiste el préstamo y sus cuotas en una base de datos transaccional (ej. PostgreSQL) bajo una transacción ACID (usualmente empleando el patrón *Transactional Outbox* hacia Kafka/SQS).

---

## 2. Especificación Técnica

### 2.1. Parámetros de Entrada

| Parámetro | Tipo | Descripción | Restricciones / Validaciones |
| :--- | :--- | :--- | :--- |
| `amount` | `number` / `Decimal` | Monto original de la compra (ej. `1000.00`). | Debe ser un número finito estricto $> 0$. |
| `installments` | `integer` | Número total de pagos (ej. `5`). | Número entero entre `2` y `12` inclusive. |
| `commissionRate` | `number` / `Decimal` | Porcentaje de comisión total sobre el monto (ej. `0.10` para 10%, `0` sin intereses). | Número finito $\ge 0$. |
| `purchaseDate` | `string` | Fecha de la compra en formato ISO (`YYYY-MM-DD`). | Debe cumplir formato estricto `YYYY-MM-DD` y ser una fecha válida en calendario (ej. rechazar `2026-02-30`). |

---

### 2.2. Reglas de Negocio y Cálculo

1. **Cálculo de Totales:**
   * `totalCommission = amount * commissionRate`
   * `totalAmount = amount + totalCommission`
   * Todos los montos deben expresarse con **exactamente 2 decimales**.

2. **Aritmética Financiera (Prevención de Errores de Punto Flotante):**
   * **Nunca** operar dinero usando aritmética estándar de punto flotante binario (`float` / `double` / IEEE 754) sin control, debido a imprecisiones como `0.1 + 0.2 = 0.30000000000000004`.
   * **Estrategia recomendada:** Convertir los montos a **centavos enteros** (`Math.round(val * 100)`) o utilizar librerías/tipos decimales exactos (`Decimal` en Python, `BigDecimal` en Java, `decimal.js` / `big.js` en TypeScript/JavaScript).

3. **Regla de Redondeo y Ajuste de Centavos:**
   * Si la división de `totalAmount` entre `installments` no es exacta, **el residuo (centavos restantes) siempre se suma o resta en el primer pago (cuota 1)** para garantizar que:
     * Todos los pagos posteriores (`2..N`) sean idénticos.
     * La suma exacta de todas las cuotas sea estrictamente igual a `totalAmount`.
   * *Ejemplo:* Si `totalAmount = 100.00` y `installments = 3`:
     * Pago 1: `33.34`
     * Pago 2: `33.33`
     * Pago 3: `33.33`
     * Suma total: `100.00`

4. **Reglas de Fechas y Zonas Horarias:**
   * **Cuota 1:** Tiene como fecha de vencimiento (`dueDate`) el mismo día de la compra (`purchaseDate`).
   * **Cuotas siguientes (`2..N`):** Se calculan sumando exactamente **15 días calendario** respecto a la fecha de la cuota anterior.
   * **Cuidado con Timezones:** Parsear y manipular las fechas en **UTC** (o usando tipos de fecha pura sin hora como `datetime.date` en Python o `LocalDate` en Java) para evitar desfasajes de un día por cambios de horario (*Daylight Saving Time*) o zonas horarias locales.

5. **Estados Iniciales (`status`):**
   * **Cuota 1:** Nace con estado `"PAID"` (se cobra al momento de la compra).
   * **Cuotas `2..N`:** Nacen con estado `"PENDING"`.

---

## 3. Contrato de Salida (JSON)

La función debe retornar un **objeto raíz (envelope)** que contenga el resumen financiero y la lista de cuotas, facilitando la extensibilidad futura de la API sin introducir *breaking changes*.

```json
{
  "totalAmount": 1100.00,
  "totalCommission": 100.00,
  "installmentsCount": 3,
  "schedule": [
    {
      "installmentNumber": 1,
      "dueDate": "2026-11-11",
      "amount": 366.68,
      "status": "PAID"
    },
    {
      "installmentNumber": 2,
      "dueDate": "2026-11-26",
      "amount": 366.66,
      "status": "PENDING"
    },
    {
      "installmentNumber": 3,
      "dueDate": "2026-12-11",
      "amount": 366.66,
      "status": "PENDING"
    }
  ]
}
```

---

## 4. Manejo de Errores (Fail-Fast)

Aplicar validación temprana en la frontera de la función lanzando excepciones tipadas / descriptivas ante entradas inválidas:

* `amount <= 0` o no numérico / `NaN` / `Infinity`.
* `installments` no es entero, o está fuera del rango `[2, 12]`.
* `commissionRate < 0` o no numérico / `NaN` / `Infinity`.
* `purchaseDate` con formato distinto a `YYYY-MM-DD` o fecha inexistente (ej. `2026-02-29`, `2026-13-01`).

---

## 5. Checklist de Evaluación y Posibles Follow-Ups de Entrevista

Cuando termines de codificar tu solución en tu IDE, verifica los siguientes puntos que suelen evaluarse en el cierre de la entrevista:

- [ ] **Complejidad Algorítmica:** Tiempo $O(N)$ y Espacio $O(N)$ donde $N = 	ext{installments}$.
- [ ] **Invariante Financiera:** $\sum_{i=1}^{N} 	ext{schedule}[i].	ext{amount} === 	ext{totalAmount}$ en todos los casos (incluyendo comisiones con decimales periódicos como `100.00` con tasa `0.0333`).
- [ ] **Casos de Prueba Unitaria (Unit Tests) sugeridos:**
  1. División exacta sin comisión (`amount: 1000`, `installments: 5`, `commissionRate: 0`).
  2. División inexacta con ajuste de centavos (`amount: 100`, `installments: 3`, `commissionRate: 0`).
  3. Compra con comisión y redondeo en el cálculo de la comisión misma (`amount: 99.99`, `installments: 4`, `commissionRate: 0.15`).
  4. Cruce de fin de mes y año bisiesto en fechas (`purchaseDate: "2026-02-20"` + 15 días $ightarrow$ `"2026-03-07"`, y cruce de año `"2026-12-25"` + 15 días $ightarrow$ `"2027-01-09"`).
  5. Casos de error por parámetros inválidos (monto negativo, 1 cuota, 13 cuotas, fecha inválida `"2026-02-30"`).
- [ ] **Posible Follow-Up:** *¿Cómo adaptarías el diseño si Aplazo introdujera diferentes frecuencias de pago (semanal, quincenal, mensual) o promociones donde el primer pago se difiere 15 días (`status: "PENDING"`)?* (Pista: Patrón Strategy o configuración de reglas de producto).
