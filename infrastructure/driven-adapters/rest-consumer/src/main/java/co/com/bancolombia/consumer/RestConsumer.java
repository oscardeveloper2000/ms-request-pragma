package co.com.bancolombia.consumer;


import co.com.bancolombia.model.common.gateways.LoggerPort;
import co.com.bancolombia.model.external.rest.user.dto.User;

import co.com.bancolombia.model.external.rest.user.dto.UserBasicInfo;
import co.com.bancolombia.model.external.rest.user.gateways.UserRepository;
import co.com.bancolombia.security.adapter.TokenGatewayAdapter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestConsumer implements UserRepository/* implements Gateway from domain */{
    private final WebClient client;
    private final LoggerPort logger;
    private final TokenGatewayAdapter tokenGateway;



@Override
@CircuitBreaker(name = "findByDocumentNumber")
public Mono<User> findByDocumentNumber(String documentNumber) {
    return Mono.defer(() -> {
        long start = System.nanoTime();
        logger.info("UserRepository.findByDocumentNumber: GET /api/v1/users/documentNumber/{} - request started", documentNumber);

        // ✅ Validación de null safety
        if (tokenGateway == null) {
            logger.error("UserRepository.findByDocumentNumber: tokenGateway is null");
            return Mono.error(new IllegalStateException("TokenGateway not properly injected"));
        }

        return Mono.fromCallable(() -> tokenGateway.getToken())
                .flatMap(tokenMono -> {
                    if (tokenMono == null) {
                        logger.error("UserRepository.findByDocumentNumber: getToken() returned null");
                        return Mono.error(new IllegalStateException("TokenGateway.getToken() returned null"));
                    }

                    return tokenMono.flatMap(tokenContext -> {
                        logger.info("UserRepository.findByDocumentNumber: tokenContexthi={}", tokenContext);
                        return client.get()
                                .uri(uriBuilder -> uriBuilder
                                        .path("/api/v1/users/document/{documentNumber}")
                                        .build(documentNumber))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenContext)
                                .exchangeToMono(response -> {
                                    if (response.statusCode() == HttpStatus.OK) {
                                        logger.info("UserRepository.findByDocumentNumber: user found for documentNumber={}", documentNumber);
                                        return response.bodyToMono(User.class);
                                    } else if (response.statusCode() == HttpStatus.NOT_FOUND) {
                                        logger.warn("UserRepository.findByDocumentNumber: user not found for documentNumber={}", documentNumber);
                                        return Mono.empty();
                                    } else {
                                        logger.error("UserRepository.findByDocumentNumber: error response status={} for documentNumber={}",
                                                   response.statusCode(), documentNumber);
                                        return Mono.error(new RuntimeException("Error fetching user: " + response.statusCode()));
                                    }
                                })
                                .doOnTerminate(() -> {
                                    long durationMs = (System.nanoTime() - start) / 1_000_000;
                                    logger.info("UserRepository.findByDocumentNumber: finished documentNumber={}, durationMs={}",
                                              documentNumber, durationMs);
                                });
                    });
                });
    })
    .doOnError(e -> logger.error("UserRepository.findByDocumentNumber: failed documentNumber={}", documentNumber, e));
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


}
