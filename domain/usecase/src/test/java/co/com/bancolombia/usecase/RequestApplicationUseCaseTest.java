package co.com.bancolombia.usecase;

import co.com.bancolombia.model.auth.TokenGateway;
import co.com.bancolombia.model.common.LoggerPort;
import co.com.bancolombia.model.common.CustomPageResponseReport;
import co.com.bancolombia.model.requestapplication.PageRequest;
import co.com.bancolombia.model.requestapplication.RequestApplication;
import co.com.bancolombia.model.requestapplication.RequestReportResponse;
import co.com.bancolombia.model.requestapplication.gateways.RequestApplicationRepository;
import co.com.bancolombia.model.status.Status;
import co.com.bancolombia.model.status.StatusCode;
import co.com.bancolombia.model.status.gateways.StatusRepository;
import co.com.bancolombia.model.typeloan.TypeLoan;
import co.com.bancolombia.model.typeloan.gateways.TypeLoanRepository;
import co.com.bancolombia.model.user.User;
import co.com.bancolombia.model.user.UserBasicInfo;
import co.com.bancolombia.model.user.gateways.UserRepository;
import co.com.bancolombia.usecase.commom.DomainValidationException;
import co.com.bancolombia.usecase.requestapplication.RequestApplicationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RequestApplicationUseCaseTest {

    private RequestApplicationRepository repository;
    private StatusRepository statusRepository;
    private TypeLoanRepository typeLoanRepository;
    private UserRepository userRepository;
    private LoggerPort logger;
    private TokenGateway tokenGateway;
    private RequestApplicationUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = mock(RequestApplicationRepository.class);
        statusRepository = mock(StatusRepository.class);
        typeLoanRepository = mock(TypeLoanRepository.class);
        userRepository = mock(UserRepository.class);
        logger = mock(LoggerPort.class);
        tokenGateway = mock(TokenGateway.class);

        useCase = new RequestApplicationUseCase(repository, statusRepository, typeLoanRepository, userRepository, logger, tokenGateway);
    }

    private RequestApplication buildRequest() {
        return RequestApplication.builder()
                .id(1L)
                .documentNumber("12345")
                .loanTypeId(10L)
                .amount(BigDecimal.valueOf(5000))
                .build();
    }

    // ✅ Caso feliz con validación de token
    @Test
    void shouldSaveRequestSuccessfully_WithValidToken() {
        var request = buildRequest();
        var user = User.builder().id(99L).email("user@test.com").build();
        var status = Status.builder().id(StatusCode.PENDING.id()).description("PENDING").build();
        var typeLoan = TypeLoan.builder().id(10L).minAmount(BigDecimal.valueOf(1000)).maxAmount(BigDecimal.valueOf(10000)).build();

        when(tokenGateway.getToken()).thenReturn(Mono.just("mockToken"));
        when(tokenGateway.getEmailFromToken()).thenReturn(Mono.just("user@test.com"));
        when(userRepository.findByDocumentNumber("12345", "mockToken")).thenReturn(Mono.just(user));
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

        when(tokenGateway.getToken()).thenReturn(Mono.just("mockToken"));
        when(tokenGateway.getEmailFromToken()).thenReturn(Mono.just("user@test.com"));
        when(userRepository.findByDocumentNumber("12345", "mockToken")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.applySave(request))
                .expectErrorMatches(ex -> ex instanceof DomainValidationException
                        && ex.getMessage().equals("User not found in ms-auth"))
                .verify();
    }

    // ❌ Usuario no tiene permisos
    @Test
    void shouldThrowException_WhenUserHasNoPermissions() {
        var request = buildRequest();
        var user = User.builder().id(99L).email("different@test.com").build();

        when(tokenGateway.getToken()).thenReturn(Mono.just("mockToken"));
        when(tokenGateway.getEmailFromToken()).thenReturn(Mono.just("user@test.com"));
        when(userRepository.findByDocumentNumber("12345", "mockToken")).thenReturn(Mono.just(user));

        StepVerifier.create(useCase.applySave(request))
                .expectErrorMatches(ex -> ex instanceof DomainValidationException
                        && ex.getMessage().contains("does not have permissions"))
                .verify();
    }

    // ❌ Status no existe
    @Test
    void shouldThrowException_WhenStatusNotFound() {
        var request = buildRequest();
        var user = User.builder().id(99L).email("user@test.com").build();
        var typeLoan = TypeLoan.builder().id(10L).minAmount(BigDecimal.valueOf(1000)).maxAmount(BigDecimal.valueOf(10000)).build();

        when(tokenGateway.getToken()).thenReturn(Mono.just("mockToken"));
        when(tokenGateway.getEmailFromToken()).thenReturn(Mono.just("user@test.com"));
        when(userRepository.findByDocumentNumber("12345", "mockToken")).thenReturn(Mono.just(user));
        when(statusRepository.findById(StatusCode.PENDING.id())).thenReturn(Mono.empty());
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

        when(tokenGateway.getToken()).thenReturn(Mono.just("mockToken"));
        when(tokenGateway.getEmailFromToken()).thenReturn(Mono.just("user@test.com"));
        when(userRepository.findByDocumentNumber("12345", "mockToken")).thenReturn(Mono.just(user));
        when(statusRepository.findById(StatusCode.PENDING.id())).thenReturn(Mono.just(status));
        when(typeLoanRepository.findById(10L)).thenReturn(Mono.empty());

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

        when(tokenGateway.getToken()).thenReturn(Mono.just("mockToken"));
        when(tokenGateway.getEmailFromToken()).thenReturn(Mono.just("user@test.com"));
        when(userRepository.findByDocumentNumber("12345", "mockToken")).thenReturn(Mono.just(user));
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

        when(tokenGateway.getToken()).thenReturn(Mono.just("mockToken"));
        when(tokenGateway.getEmailFromToken()).thenReturn(Mono.just("user@test.com"));
        when(userRepository.findByDocumentNumber("12345", "mockToken")).thenReturn(Mono.just(user));
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

        when(tokenGateway.getToken()).thenReturn(Mono.just("mockToken"));
        when(tokenGateway.getEmailFromToken()).thenReturn(Mono.just("user@test.com"));
        when(userRepository.findByDocumentNumber("12345", "mockToken")).thenReturn(Mono.just(user));
        when(statusRepository.findById(StatusCode.PENDING.id())).thenReturn(Mono.just(status));
        when(typeLoanRepository.findById(10L)).thenReturn(Mono.just(typeLoan));

        StepVerifier.create(useCase.applySave(request))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().equals("Amount is required"))
                .verify();
    }

    // ✅ Caso: min es null (no debe lanzar excepción, siempre pasa)
    @Test
    void shouldPass_WhenMinIsNull() {
        var request = buildRequest().toBuilder().amount(BigDecimal.valueOf(5000)).build();
        var user = User.builder().id(99L).email("user@test.com").build();
        var status = Status.builder().id(StatusCode.PENDING.id()).description("PENDING").build();
        var typeLoan = TypeLoan.builder()
                .id(10L)
                .minAmount(null)
                .maxAmount(BigDecimal.valueOf(10000))
                .build();

        when(tokenGateway.getToken()).thenReturn(Mono.just("mockToken"));
        when(tokenGateway.getEmailFromToken()).thenReturn(Mono.just("user@test.com"));
        when(userRepository.findByDocumentNumber("12345", "mockToken")).thenReturn(Mono.just(user));
        when(statusRepository.findById(StatusCode.PENDING.id())).thenReturn(Mono.just(status));
        when(typeLoanRepository.findById(10L)).thenReturn(Mono.just(typeLoan));
        when(repository.save(any(RequestApplication.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.applySave(request))
                .expectNextMatches(saved -> saved.getAmount().equals(BigDecimal.valueOf(5000)))
                .verifyComplete();
    }

    // ✅ Caso: max es null (no debe lanzar excepción, siempre pasa)
    @Test
    void shouldPass_WhenMaxIsNull() {
        var request = buildRequest().toBuilder().amount(BigDecimal.valueOf(5000)).build();
        var user = User.builder().id(99L).email("user@test.com").build();
        var status = Status.builder().id(StatusCode.PENDING.id()).description("PENDING").build();
        var typeLoan = TypeLoan.builder()
                .id(10L)
                .minAmount(BigDecimal.valueOf(1000))
                .maxAmount(null)
                .build();

        when(tokenGateway.getToken()).thenReturn(Mono.just("mockToken"));
        when(tokenGateway.getEmailFromToken()).thenReturn(Mono.just("user@test.com"));
        when(userRepository.findByDocumentNumber("12345", "mockToken")).thenReturn(Mono.just(user));
        when(statusRepository.findById(StatusCode.PENDING.id())).thenReturn(Mono.just(status));
        when(typeLoanRepository.findById(10L)).thenReturn(Mono.just(typeLoan));
        when(repository.save(any(RequestApplication.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.applySave(request))
                .expectNextMatches(saved -> saved.getAmount().equals(BigDecimal.valueOf(5000)))
                .verifyComplete();
    }

    // ✅ applyFilterByStatus: caso feliz
    @Test
    void shouldReturnFilteredRequestsByStatusSuccessfully() {
        var pageable = new PageRequest(0, 10);
        var statusId = StatusCode.PENDING.id();
        var token = "mockToken";
        var requestApp = RequestApplication.builder()
                .id(1L)
                .email("user@test.com")
                .amount(BigDecimal.valueOf(5000))
                .term(12)
                .loanTypeId(10L)
                .statusId(statusId)
                .build();
        var approvedApp = RequestApplication.builder()
                .id(2L)
                .email("user@test.com")
                .amount(BigDecimal.valueOf(1000))
                .term(12)
                .loanTypeId(10L)
                .statusId(StatusCode.APPROVED.id())
                .build();
        var typeLoan = TypeLoan.builder().id(10L).name("Personal").interestRate(0.12).minAmount(BigDecimal.valueOf(1000)).maxAmount(BigDecimal.valueOf(10000)).build();
        var status = Status.builder().id(statusId).name("PENDING").build();
        var userInfo = UserBasicInfo.builder().email("user@test.com").firstName("Oscar").lastName("Dev").baseSalary(BigDecimal.valueOf(3000)).build();

        when(repository.countByStatusId(statusId)).thenReturn(Mono.just(1L));
        when(repository.findAllByStatusIdWithPageable(statusId, pageable)).thenReturn(Flux.just(requestApp));
        when(repository.findAllByStatusId(StatusCode.APPROVED.id())).thenReturn(Flux.just(approvedApp));
        when(typeLoanRepository.findById(10L)).thenReturn(Mono.just(typeLoan));
        when(statusRepository.findById(statusId)).thenReturn(Mono.just(status));
        when(userRepository.findUsersByEmails(List.of("user@test.com"), token)).thenReturn(Mono.just(List.of(userInfo)));

        StepVerifier.create(useCase.applyFilterByStatus(pageable, statusId, token))
                .assertNext(response -> {
                    assertThat(response.getContent()).hasSize(1);
                    assertThat(response.getContent().get(0).getEmail()).isEqualTo("user@test.com");
                    assertThat(response.getTotalMonthlyDebtOfApprovedLoans()).isNotNull();
                    assertThat(response.isFirst()).isTrue();
                    assertThat(response.isLast()).isTrue();
                })
                .verifyComplete();
    }

    // ✅ applyFilterByStatus: sin resultados
//    @Test
//    void shouldReturnEmptyContent_WhenNoRequestsFound() {
//        var pageable = new PageRequest(0, 10);
//        var statusId = StatusCode.PENDING.id();
//        var token = "mockToken";
//
//        when(repository.countByStatusId(statusId)).thenReturn(Mono.just(0L));
//        when(repository.findAllByStatusIdWithPageable(statusId, pageable)).thenReturn(Flux.empty());
//        when(repository.findAllByStatusId(StatusCode.APPROVED.id())).thenReturn(Flux.empty());
//        when(userRepository.findUsersByEmails(List.of(), token)).thenReturn(Mono.just(List.of()));
//
//        StepVerifier.create(useCase.applyFilterByStatus(pageable, statusId, token))
//                .assertNext(response -> {
//                    assertThat(response.getContent()).isEmpty();
//                    assertThat(response.getTotalMonthlyDebtOfApprovedLoans()).isEqualTo(BigDecimal.ZERO);
//                    assertThat(response.isFirst()).isTrue();
//                    assertThat(response.isLast()).isTrue();
//                })
//                .verifyComplete();
//    }
}