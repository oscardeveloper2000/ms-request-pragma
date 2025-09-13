package co.com.bancolombia.consumer;


import co.com.bancolombia.model.common.gateways.LoggerPort;
import co.com.bancolombia.model.external.rest.user.dto.User;

import co.com.bancolombia.model.external.rest.user.dto.UserBasicInfo;
import co.com.bancolombia.model.external.rest.user.gateways.UserRepository;
import co.com.bancolombia.security.adapter.TokenGatewayAdapter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class RestConsumer implements UserRepository/* implements Gateway from domain */{
    private final WebClient client;
    private final LoggerPort logger;
    private final TokenGatewayAdapter tokenGateway;
    private final String serviceEmail;
    private final String servicePassword;

    public RestConsumer(WebClient client, LoggerPort logger, TokenGatewayAdapter tokenGateway, @Value("${adapter.restconsumer.email}") String serviceEmail,
                        @Value("${adapter.restconsumer.password}") String servicePassword) {
        this.client = client;
        this.tokenGateway = tokenGateway;
        this.serviceEmail = serviceEmail;
        this.servicePassword = servicePassword;
        this.logger = logger;
    }

@Override
@CircuitBreaker(name = "findByDocumentNumber", fallbackMethod = "fallbackFindUserByEmail")
public Mono<User> findByDocumentNumber(String documentNumber) {
    logger.info("UserRepository.findByDocumentNumber: GET /api/v1/users/documentNumber/{} - request started", documentNumber);
    return getToken()
            .doOnNext(tokenContext -> logger.info("UserRepository.findByDocumentNumber: tokenContext={}", tokenContext))
            .flatMap(tokenContext ->
        client.get()
            .uri("/api/v1/users/document/{documentNumber}", documentNumber)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenContext.token())
            .retrieve()
            .bodyToMono(User.class)
    );
}

    @CircuitBreaker(name = "getToken")
    public Mono<TokenResponse> getToken() {
        LoginRequest login = new LoginRequest(serviceEmail, servicePassword);
        return client
                .post()
                .uri("/api/v1/login")
                .bodyValue(login)
                .retrieve()
                .bodyToMono(TokenResponse.class);
    }



public Mono<List<UserBasicInfo>> findUsersByEmails(List<String> emails) {
        return Mono.defer(() -> {
            long start = System.nanoTime();
            logger.info("UserRepository.findUsersByEmails: POST /api/v1/users/emails - request started");
            // ✅ Validación de null safety
            if (tokenGateway == null) {
                logger.error("UserRepository.findByDocumentNumber: tokenGateway is null");
                return Mono.error(new IllegalStateException("TokenGateway not properly injected"));
            }
            return Mono.fromCallable(() -> tokenGateway.getToken())
                    .flatMap(tokenMono ->{
                        if (tokenMono == null) {
                            logger.error("UserRepository.findUsersByEmails: getToken() returned null");
                            return Mono.error(new IllegalStateException("TokenGateway.getToken() returned null"));
                        }
                        return tokenMono.flatMap(tokenContext -> {
                            logger.info("UserRepository.findUsersByEmails: tokenContext={}", tokenContext);
                            return client.post()
                                    .uri("/api/v1/users/emails")
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenContext)
                                    .bodyValue(emails)
                                    .retrieve()
                                    .bodyToFlux(UserBasicInfo.class)
                                    .collectList()
                                    .doOnNext(list -> logger.info("UserRepository.findUsersByEmails: found {} users", list.size()))
                                    .doOnTerminate(() -> {
                                        long durationMs = (System.nanoTime() - start) / 1_000_000;
                                        logger.info("UserRepository.findUsersByEmails: finished, durationMs={}", durationMs);
                                    });
                        });
                    });

        }).doOnError(e -> logger.error("UserRepository.findUsersByEmails: failed", e));
    }

    public Mono<User> fallbackFindUserByEmail(String mail, Exception ex) {
        return Mono.error(new RuntimeException("Fallback: Unable to fetch user by email " + mail, ex));
    }

}
