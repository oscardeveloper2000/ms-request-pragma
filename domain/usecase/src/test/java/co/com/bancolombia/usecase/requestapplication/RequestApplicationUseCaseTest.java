package co.com.bancolombia.usecase.requestapplication;

import co.com.bancolombia.model.common.LoggerPort;
import co.com.bancolombia.model.requestapplication.RequestApplication;
import co.com.bancolombia.model.requestapplication.gateways.RequestApplicationRepository;
import co.com.bancolombia.model.status.Status;
import co.com.bancolombia.model.status.StatusCode;
import co.com.bancolombia.model.status.gateways.StatusRepository;
import co.com.bancolombia.model.typeloan.TypeLoan;
import co.com.bancolombia.model.typeloan.gateways.TypeLoanRepository;
import co.com.bancolombia.model.user.User;
import co.com.bancolombia.model.user.gateways.UserRepository;
import co.com.bancolombia.usecase.requestapplication.commom.DomainValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class RequestApplicationUseCaseTest {

    private RequestApplicationRepository repository;
    private StatusRepository statusRepository;
    private TypeLoanRepository typeLoanRepository;
    private UserRepository userRepository;
    private LoggerPort logger;
    private RequestApplicationUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = mock(RequestApplicationRepository.class);
        statusRepository = mock(StatusRepository.class);
        typeLoanRepository = mock(TypeLoanRepository.class);
        userRepository = mock(UserRepository.class);
        logger = mock(LoggerPort.class);

        useCase = new RequestApplicationUseCase(repository, statusRepository, typeLoanRepository, userRepository, logger);
    }

    private RequestApplication buildRequest() {
        return RequestApplication.builder()
                .id(1L)
                .documentNumber("12345")
                .loanTypeId(10L)
                .amount(BigDecimal.valueOf(5000))
                .build();
    }

    // ✅ Caso feliz
    @Test
    void shouldSaveRequestSuccessfully() {
        var request = buildRequest();
        var user = User.builder().id(99L).email("user@test.com").build();
        var status = Status.builder().id(StatusCode.PENDING.id()).description("PENDING").build();
        var typeLoan = TypeLoan.builder().id(10L).minAmount(BigDecimal.valueOf(1000)).maxAmount(BigDecimal.valueOf(10000)).build();

        when(userRepository.findByDocumentNumber("12345")).thenReturn(Mono.just(user));
        when(statusRepository.findById(StatusCode.PENDING.id())).thenReturn(Mono.just(status));
        when(typeLoanRepository.findById(10L)).thenReturn(Mono.just(typeLoan));
        when(repository.save(any(RequestApplication.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.applySave(request))
                .expectNextMatches(saved ->
                        saved.getEmail().equals("user@test.com")
                                && saved.getStatusId().equals(StatusCode.PENDING.id())
                )
                .verifyComplete();

        verify(repository).save(any(RequestApplication.class));
    }

    // ❌ Usuario no existe
    @Test
    void shouldThrowException_WhenUserNotFound() {
        var request = buildRequest();

        when(userRepository.findByDocumentNumber("12345")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.applySave(request))
                .expectErrorMatches(ex -> ex instanceof DomainValidationException
                        && ex.getMessage().equals("User not found in ms-auth"))
                .verify();
    }

    // ❌ Status no existe
    @Test
    void shouldThrowException_WhenStatusNotFound() {
        var request = buildRequest();
        var user = User.builder().id(99L).email("user@test.com").build();
        var typeLoan = TypeLoan.builder().id(10L).minAmount(BigDecimal.valueOf(1000)).maxAmount(BigDecimal.valueOf(10000)).build();

        when(userRepository.findByDocumentNumber("12345")).thenReturn(Mono.just(user));
        when(statusRepository.findById(StatusCode.PENDING.id())).thenReturn(Mono.empty()); // simula que no existe
        when(typeLoanRepository.findById(10L)).thenReturn(Mono.just(typeLoan));

        StepVerifier.create(useCase.applySave(request))
                .expectErrorMatches(ex -> ex instanceof DomainValidationException
                        && ex.getMessage().equals("Status not found"))
                .verify();
    }

    // ❌ TypeLoan no existe
    @Test
    void shouldThrowException_WhenTypeLoanNotFound() {
        var request = buildRequest();
        var user = User.builder().id(99L).email("user@test.com").build();
        var status = Status.builder().id(StatusCode.PENDING.id()).description("PENDING").build();

        when(userRepository.findByDocumentNumber("12345")).thenReturn(Mono.just(user));
        when(statusRepository.findById(StatusCode.PENDING.id())).thenReturn(Mono.just(status));
        when(typeLoanRepository.findById(10L)).thenReturn(Mono.empty()); // simula que no existe

        StepVerifier.create(useCase.applySave(request))
                .expectErrorMatches(ex -> ex instanceof DomainValidationException
                        && ex.getMessage().equals("TypeLoan not found"))
                .verify();
    }

    // ❌ Monto por debajo del mínimo
    @Test
    void shouldThrowException_WhenAmountBelowMin() {
        var request = buildRequest().toBuilder().amount(BigDecimal.valueOf(500)).build();
        var user = User.builder().id(99L).email("user@test.com").build();
        var status = Status.builder().id(StatusCode.PENDING.id()).description("PENDING").build();
        var typeLoan = TypeLoan.builder().id(10L).minAmount(BigDecimal.valueOf(1000)).maxAmount(BigDecimal.valueOf(10000)).build();

        when(userRepository.findByDocumentNumber("12345")).thenReturn(Mono.just(user));
        when(statusRepository.findById(StatusCode.PENDING.id())).thenReturn(Mono.just(status));
        when(typeLoanRepository.findById(10L)).thenReturn(Mono.just(typeLoan));

        StepVerifier.create(useCase.applySave(request))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().contains("below minimum"))
                .verify();
    }

    // ❌ Monto por encima del máximo
    @Test
    void shouldThrowException_WhenAmountAboveMax() {
        var request = buildRequest().toBuilder().amount(BigDecimal.valueOf(20000)).build();
        var user = User.builder().id(99L).email("user@test.com").build();
        var status = Status.builder().id(StatusCode.PENDING.id()).description("PENDING").build();
        var typeLoan = TypeLoan.builder().id(10L).minAmount(BigDecimal.valueOf(1000)).maxAmount(BigDecimal.valueOf(10000)).build();

        when(userRepository.findByDocumentNumber("12345")).thenReturn(Mono.just(user));
        when(statusRepository.findById(StatusCode.PENDING.id())).thenReturn(Mono.just(status));
        when(typeLoanRepository.findById(10L)).thenReturn(Mono.just(typeLoan));

        StepVerifier.create(useCase.applySave(request))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().contains("above maximum"))
                .verify();
    }

    // ❌ Amount nulo
    @Test
    void shouldThrowException_WhenAmountIsNull() {
        var request = buildRequest().toBuilder().amount(null).build();
        var user = User.builder().id(99L).email("user@test.com").build();
        var status = Status.builder().id(StatusCode.PENDING.id()).description("PENDING").build();
        var typeLoan = TypeLoan.builder().id(10L).minAmount(BigDecimal.valueOf(1000)).maxAmount(BigDecimal.valueOf(10000)).build();

        when(userRepository.findByDocumentNumber("12345")).thenReturn(Mono.just(user));
        when(statusRepository.findById(StatusCode.PENDING.id())).thenReturn(Mono.just(status));
        when(typeLoanRepository.findById(10L)).thenReturn(Mono.just(typeLoan));

        StepVerifier.create(useCase.applySave(request))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().equals("Amount is required"))
                .verify();
    }
}
