package mx.aplazo.bnpl.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class PaymentScheduleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generate_happyPath_returns200WithFullSchedule() throws Exception {
        mockMvc.perform(post("/payment-schedule/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1000.00,"installments":5,"commissionRate":0.10,"purchaseDate":"2026-01-01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(1100.00))
                .andExpect(jsonPath("$.totalCommission").value(100.00))
                .andExpect(jsonPath("$.installmentsCount").value(5))
                .andExpect(jsonPath("$.schedule.length()").value(5))
                .andExpect(jsonPath("$.schedule[0].installmentNumber").value(1))
                .andExpect(jsonPath("$.schedule[0].dueDate").value("2026-01-01"))
                .andExpect(jsonPath("$.schedule[0].status").value("PAID"))
                .andExpect(jsonPath("$.schedule[1].status").value("PENDING"));
    }

    @Test
    void generate_invalidAmount_returns400WithInvalidAmountCode() throws Exception {
        mockMvc.perform(post("/payment-schedule/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":0.001,"installments":5,"commissionRate":0.10,"purchaseDate":"2026-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_AMOUNT"))
                .andExpect(jsonPath("$.path").value("/payment-schedule/generate"));
    }

    @Test
    void generate_invalidInstallments_returns400WithInvalidInstallmentsCode() throws Exception {
        mockMvc.perform(post("/payment-schedule/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100,"installments":1,"commissionRate":0.10,"purchaseDate":"2026-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INSTALLMENTS"));
    }

    @Test
    void generate_invalidCommissionRate_returns400WithInvalidCommissionRateCode() throws Exception {
        mockMvc.perform(post("/payment-schedule/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100,"installments":5,"commissionRate":-0.5,"purchaseDate":"2026-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_COMMISSION_RATE"));
    }

    @Test
    void generate_invalidPurchaseDate_returns400WithInvalidPurchaseDateCode() throws Exception {
        mockMvc.perform(post("/payment-schedule/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":100,"installments":5,"commissionRate":0.10,"purchaseDate":"2026-02-30"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PURCHASE_DATE"));
    }

    @Test
    void generate_missingRequiredField_returns400WithValidationErrorCode() throws Exception {
        mockMvc.perform(post("/payment-schedule/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"installments":5,"commissionRate":0.10,"purchaseDate":"2026-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void generate_malformedJsonBody_returns400WithMalformedJsonCode() throws Exception {
        mockMvc.perform(post("/payment-schedule/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":"abc","installments":5,"commissionRate":0.10,"purchaseDate":"2026-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_JSON"));
    }

    @Test
    void actuatorHealth_returns200Up() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
