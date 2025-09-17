package co.com.bancolombia.usecase.requestapplication;

import co.com.bancolombia.model.common.gateways.LoggerPort;
import co.com.bancolombia.model.external.messaging.gateway.MessagePublisherGateway;
import co.com.bancolombia.model.domains.requestapplication.RequestApplication;
import co.com.bancolombia.model.domains.requestapplication.gateways.RequestApplicationRepository;
import co.com.bancolombia.model.domains.status.Status;
import co.com.bancolombia.model.domains.status.StatusCode;
import co.com.bancolombia.model.domains.status.gateways.StatusRepository;
import co.com.bancolombia.model.domains.typeloan.TypeLoan;
import co.com.bancolombia.model.domains.typeloan.gateways.TypeLoanRepository;
import co.com.bancolombia.model.external.rest.user.dto.User;
import co.com.bancolombia.model.external.rest.user.gateways.UserRepository;
import co.com.bancolombia.usecase.commom.DomainValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateStatusUseCaseTest {

    @Mock
    private RequestApplicationRepository requestLoanRepository;

    @Mock
    private StatusRepository statusRepository;

    @Mock
    private TypeLoanRepository typeLoanRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoggerPort logger;

    @Mock
    private MessagePublisherGateway messagePublisherGateway;

    private UpdateStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateStatusUseCase(
            requestLoanRepository,
            statusRepository,
            typeLoanRepository,
            userRepository,
            logger,
            messagePublisherGateway
        );
    }

    @Test
    void shouldUpdateStatusToApproved_WhenValidRequest() {
        var requestId = 1L;
        var newStatus = "APPROVED";
        var request = buildPendingRequest(requestId);
        var updatedRequest = request.toBuilder().statusId(StatusCode.APPROVED.id()).build();
        var user = buildUser();
        var status = buildStatus(StatusCode.APPROVED.id(), "APPROVED");
        var typeLoan = buildTypeLoan();

        when(statusRepository.findById(StatusCode.APPROVED.id())).thenReturn(Mono.just(status));
        when(requestLoanRepository.findById(requestId)).thenReturn(Mono.just(request));
        when(requestLoanRepository.save(any())).thenReturn(Mono.just(updatedRequest));
        when(userRepository.findByDocumentNumber(request.getDocumentNumber())).thenReturn(Mono.just(user));
        when(typeLoanRepository.findById(request.getLoanTypeId())).thenReturn(Mono.just(typeLoan));
        when(messagePublisherGateway.publishLoanNotificationEmail(anyString())).thenReturn(Mono.just("message-id-123"));
        when(messagePublisherGateway.publishReportLoan(anyString())).thenReturn(Mono.just("report-id-123"));

        StepVerifier.create(useCase.updateStatus(requestId, newStatus))
                .expectNextMatches(response ->
                    response.getRequestId().equals(requestId) &&
                    response.getStatusName().equals(newStatus))
                .verifyComplete();

        verify(requestLoanRepository).save(argThat(req -> req.getStatusId().equals(StatusCode.APPROVED.id())));
        verify(messagePublisherGateway).publishLoanNotificationEmail(anyString());
        verify(messagePublisherGateway).publishReportLoan(anyString());
    }

    @Test
    void shouldUpdateStatusToRejected_WhenValidRequest() {
        var requestId = 1L;
        var newStatus = "REJECTED";
        var request = buildPendingRequest(requestId);
        var updatedRequest = request.toBuilder().statusId(StatusCode.REJECTED.id()).build();
        var user = buildUser();
        var status = buildStatus(StatusCode.REJECTED.id(), "REJECTED");

        when(statusRepository.findById(StatusCode.REJECTED.id())).thenReturn(Mono.just(status));
        when(requestLoanRepository.findById(requestId)).thenReturn(Mono.just(request));
        when(requestLoanRepository.save(any())).thenReturn(Mono.just(updatedRequest));
        when(userRepository.findByDocumentNumber(request.getDocumentNumber())).thenReturn(Mono.just(user));
        when(messagePublisherGateway.publishLoanNotificationEmail(anyString())).thenReturn(Mono.just("message-id-123"));

        StepVerifier.create(useCase.updateStatus(requestId, newStatus))
                .expectNextMatches(response ->
                    response.getRequestId().equals(requestId) &&
                    response.getStatusName().equals(newStatus))
                .verifyComplete();

        verify(requestLoanRepository).save(argThat(req -> req.getStatusId().equals(StatusCode.REJECTED.id())));
        verify(messagePublisherGateway).publishLoanNotificationEmail(anyString());
        verify(messagePublisherGateway, never()).publishReportLoan(anyString());
    }

    @Test
    void shouldThrowException_WhenInvalidStatus() {
        var requestId = 1L;
        var newStatus = "INVALID_STATUS";

        StepVerifier.create(useCase.updateStatus(requestId, newStatus))
                .expectErrorMatches(ex -> ex instanceof DomainValidationException &&
                    ex.getMessage().equals("Invalid status: INVALID_STATUS"))
                .verify();

        verify(requestLoanRepository, never()).findById(any());
    }

    @Test
    void shouldThrowException_WhenStatusNotFound() {
        var requestId = 1L;
        var newStatus = "APPROVED";

        when(statusRepository.findById(StatusCode.APPROVED.id())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.updateStatus(requestId, newStatus))
                .expectErrorMatches(ex -> ex instanceof DomainValidationException &&
                    ex.getMessage().equals("Status not found: APPROVED"))
                .verify();
    }

    @Test
    void shouldThrowException_WhenRequestNotFound() {
        var requestId = 999L;
        var newStatus = "APPROVED";
        var status = buildStatus(StatusCode.APPROVED.id(), "APPROVED");

        when(statusRepository.findById(StatusCode.APPROVED.id())).thenReturn(Mono.just(status));
        when(requestLoanRepository.findById(requestId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.updateStatus(requestId, newStatus))
                .expectErrorMatches(ex -> ex instanceof DomainValidationException &&
                    ex.getMessage().equals("Request not found with id: " + requestId))
                .verify();
    }

    @Test
    void shouldThrowException_WhenUserNotFound() {
        var requestId = 1L;
        var newStatus = "APPROVED";
        var request = buildPendingRequest(requestId);
        var updatedRequest = request.toBuilder().statusId(StatusCode.APPROVED.id()).build();
        var status = buildStatus(StatusCode.APPROVED.id(), "APPROVED");

        when(statusRepository.findById(StatusCode.APPROVED.id())).thenReturn(Mono.just(status));
        when(requestLoanRepository.findById(requestId)).thenReturn(Mono.just(request));
        when(requestLoanRepository.save(any())).thenReturn(Mono.just(updatedRequest));
        when(userRepository.findByDocumentNumber(request.getDocumentNumber())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.updateStatus(requestId, newStatus))
                .expectErrorMatches(ex -> ex instanceof DomainValidationException &&
                    ex.getMessage().equals("User not found with document: " + request.getDocumentNumber()))
                .verify();
    }

    @Test
    void shouldThrowException_WhenTypeLoanNotFound() {
        var requestId = 1L;
        var newStatus = "APPROVED";
        var request = buildPendingRequest(requestId);
        var updatedRequest = request.toBuilder().statusId(StatusCode.APPROVED.id()).build();
        var user = buildUser();
        var status = buildStatus(StatusCode.APPROVED.id(), "APPROVED");

        when(statusRepository.findById(StatusCode.APPROVED.id())).thenReturn(Mono.just(status));
        when(requestLoanRepository.findById(requestId)).thenReturn(Mono.just(request));
        when(requestLoanRepository.save(any())).thenReturn(Mono.just(updatedRequest));
        when(userRepository.findByDocumentNumber(request.getDocumentNumber())).thenReturn(Mono.just(user));
        when(typeLoanRepository.findById(request.getLoanTypeId())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.updateStatus(requestId, newStatus))
                .expectErrorMatches(ex -> ex instanceof DomainValidationException &&
                    ex.getMessage().equals("Loan type not found with id: " + request.getLoanTypeId()))
                .verify();
    }

    @Test
    void shouldAcceptCaseInsensitiveStatus_WhenValidStatus() {
        var requestId = 1L;
        var newStatus = "approved";
        var request = buildPendingRequest(requestId);
        var updatedRequest = request.toBuilder().statusId(StatusCode.APPROVED.id()).build();
        var user = buildUser();
        var status = buildStatus(StatusCode.APPROVED.id(), "APPROVED");
        var typeLoan = buildTypeLoan();

        when(statusRepository.findById(StatusCode.APPROVED.id())).thenReturn(Mono.just(status));
        when(requestLoanRepository.findById(requestId)).thenReturn(Mono.just(request));
        when(requestLoanRepository.save(any())).thenReturn(Mono.just(updatedRequest));
        when(userRepository.findByDocumentNumber(request.getDocumentNumber())).thenReturn(Mono.just(user));
        when(typeLoanRepository.findById(request.getLoanTypeId())).thenReturn(Mono.just(typeLoan));
        when(messagePublisherGateway.publishLoanNotificationEmail(anyString())).thenReturn(Mono.just("message-id-123"));
        when(messagePublisherGateway.publishReportLoan(anyString())).thenReturn(Mono.just("report-id-123"));

        StepVerifier.create(useCase.updateStatus(requestId, newStatus))
                .expectNextMatches(response -> response.getStatusName().equals("approved"))
                .verifyComplete();
    }


    private RequestApplication buildPendingRequest(Long id) {
        return buildRequest(id, StatusCode.PENDING.id());
    }

    private RequestApplication buildRequest(Long id, Long statusId) {
        return RequestApplication.builder()
                .id(id)
                .email("juan@test.com")
                .documentNumber("12345678")
                .amount(BigDecimal.valueOf(50000))
                .term(12)
                .loanTypeId(1L)
                .statusId(statusId)
                .build();
    }

    private User buildUser() {
        return User.builder()
                .documentNumber("12345678")
                .firstName("Juan")
                .lastName("Pérez")
                .email("juan@test.com")
                .build();
    }

    private Status buildStatus(Long id, String name) {
        return Status.builder()
                .id(id)
                .name(name)
                .description(name)
                .build();
    }

    private TypeLoan buildTypeLoan() {
        return TypeLoan.builder()
                .id(1L)
                .name("Personal")
                .interestRate(0.12)
                .minAmount(BigDecimal.valueOf(1000))
                .maxAmount(BigDecimal.valueOf(100000))
                .build();
    }
}