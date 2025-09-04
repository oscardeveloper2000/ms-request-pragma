package co.com.bancolombia.consumer;

import co.com.bancolombia.model.common.LoggerPort;
import co.com.bancolombia.model.user.User;

import co.com.bancolombia.model.user.UserBasicInfo;
import co.com.bancolombia.model.user.gateways.UserRepository;
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




    @Override
    @CircuitBreaker(name = "findByDocumentNumber") // opcional: protege la llamada externa
    public Mono<User> findByDocumentNumber(String documentNumber, String token) {
        return Mono.defer(() -> {
                    long start = System.nanoTime();
                    logger.info("UserRepository.findByDocumentNumber: GET /api/v1/users/documentNumber/{} - request started", documentNumber);

                    return client.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/api/v1/users/document/{documentNumber}")
                                    .build(documentNumber))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .exchangeToMono(response -> {
                                HttpStatus status = (HttpStatus) response.statusCode();
                                logger.info("UserRepository.findByDocumentNumber: response status={} for documentNumber={}", status.value(), documentNumber);

                                if (status.equals(HttpStatus.OK)) {
                                    return response.bodyToMono(User.class)
                                            .doOnNext(user -> logger.info("UserRepository.findByDocumentNumber: user found documentNumber={}, userId={}",
                                                    documentNumber, user.getId()));
                                }
                                if (status.equals(HttpStatus.NOT_FOUND)) {
                                    logger.warn("UserRepository.findByDocumentNumber: user not found documentNumber={}", documentNumber);
                                    return Mono.empty();
                                }
                                return response.createException().flatMap(ex -> {
                                    logger.error("UserRepository.findByDocumentNumber: error response for documentNumber={}", documentNumber, ex);
                                    return Mono.error(ex);
                                });
                            })
                            .doOnTerminate(() -> {
                                long durationMs = (System.nanoTime() - start) / 1_000_000;
                                logger.info("UserRepository.findByDocumentNumber: finished documentNumber={}, durationMs={}", documentNumber, durationMs);
                            });
                })
                .doOnError(e -> logger.error("UserRepository.findByDocumentNumber: failed documentNumber={}", documentNumber, e));
    }

    public Mono<List<UserBasicInfo>> findUsersByEmails(List<String> emails, String token) {
        return Mono.defer(() -> {
            long start = System.nanoTime();
            logger.info("UserRepository.findUsersByEmails: POST /api/v1/users/emails - request started");

            return client.post()
                    .uri("/api/v1/users/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .bodyValue(emails)
                    .retrieve()
                    .bodyToFlux(UserBasicInfo.class)
                    .collectList()
                    .doOnNext(list -> logger.info("UserRepository.findUsersByEmails: found {} users", list.size()))
                    .doOnTerminate(() -> {
                        long durationMs = (System.nanoTime() - start) / 1_000_000;
                        logger.info("UserRepository.findUsersByEmails: finished, durationMs={}", durationMs);
                    });
        }).doOnError(e -> logger.error("UserRepository.findUsersByEmails: failed", e));
    }


}
