package co.com.bancolombia.config;


import co.com.bancolombia.model.common.gateways.LoggerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {

    @Bean
    public LoggerPort loggerPort() {
        return new Slf4jLoggerAdapter();
    }
}

