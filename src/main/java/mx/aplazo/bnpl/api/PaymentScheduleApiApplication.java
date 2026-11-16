package mx.aplazo.bnpl.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "mx.aplazo.bnpl")
public class PaymentScheduleApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentScheduleApiApplication.class, args);
    }
}
