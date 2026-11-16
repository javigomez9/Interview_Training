package mx.aplazo.bnpl.api.config;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    public SpanExporter otelLoggingSpanExporter() {
        return LoggingSpanExporter.create();
    }
}
