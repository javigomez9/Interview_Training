package mx.aplazo.bnpl.api;

import jakarta.validation.Valid;
import mx.aplazo.bnpl.api.dto.GeneratePaymentScheduleRequest;
import mx.aplazo.bnpl.api.dto.PaymentScheduleResponse;
import mx.aplazo.bnpl.schedule.PaymentSchedule;
import mx.aplazo.bnpl.schedule.PaymentScheduleGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment-schedule")
public class PaymentScheduleController {

    @PostMapping("/generate")
    public ResponseEntity<PaymentScheduleResponse> generate(@Valid @RequestBody GeneratePaymentScheduleRequest request) {
        PaymentSchedule result = PaymentScheduleGenerator.generatePaymentSchedule(
                request.amount(),
                request.installments(),
                request.commissionRate(),
                request.purchaseDate());

        return ResponseEntity.ok(PaymentScheduleResponse.from(result));
    }
}
