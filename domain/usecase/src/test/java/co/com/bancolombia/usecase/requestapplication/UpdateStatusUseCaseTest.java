package co.com.bancolombia.usecase.requestapplication;

import co.com.bancolombia.model.auth.TokenGateway;
import co.com.bancolombia.model.common.gateways.LoggerPort;
import co.com.bancolombia.model.external.messaging.gateway.MessagePublisherGateway;
import co.com.bancolombia.model.notification.NotificationGateway;
import co.com.bancolombia.model.domains.requestapplication.RequestApplication;
import co.com.bancolombia.model.domains.requestapplication.gateways.RequestApplicationRepository;
import co.com.bancolombia.model.domains.status.StatusCode;
import co.com.bancolombia.model.domains.status.gateways.StatusRepository;
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

    @Mock
    private TokenGateway tokenGateway;

    private UpdateStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateStatusUseCase(
            requestLoanRepository,
            statusRepository,
            typeLoanRepository,
            userRepository,
            logger,
                messagePublisherGateway,
            tokenGateway
        );
    }

    @Test
    void shouldUpdateStatusToApproved_WhenValidRequest() {
        var requestId = 1L;
        var newStatus = "APPROVED";
        var request = buildPendingRequest(requestId);
        var updatedRequest = request.toBuilder().statusId(StatusCode.APPROVED.id()).build();
        var user = buildUser();
        var token = "mockToken";

        when(requestLoanRepository.findById(requestId)).thenReturn(Mono.just(request));
        when(requestLoanRepository.save(any())).thenReturn(Mono.just(updatedRequest));
        when(tokenGateway.getToken()).thenReturn(Mono.just(token));
        when(userRepository.findByDocumentNumber(request.getDocumentNumber(), token)).thenReturn(Mono.just(user));
        when(messagePublisherGateway.publishLoanNotificationEmail(anyString())).thenReturn(Mono.just("message-id-123"));

        StepVerifier.create(useCase.updateStatus(requestId, newStatus))
                .expectNextMatches(response ->
                    response.getRequestId().equals(requestId) &&
                    response.getStatusName().equals(newStatus))
                .verifyComplete();

        verify(requestLoanRepository).save(argThat(req -> req.getStatusId().equals(StatusCode.APPROVED.id())));
        verify(messagePublisherGateway).publishLoanNotificationEmail(contains("APPROVED"));
    }

    @Test
    void shouldUpdateStatusToRejected_WhenValidRequest() {
        var requestId = 1L;
        var newStatus = "REJECTED";
        var request = buildPendingRequest(requestId);
        var updatedRequest = request.toBuilder().statusId(StatusCode.REJECTED.id()).build();
        var user = buildUser();
        var token = "mockToken";

        when(requestLoanRepository.findById(requestId)).thenReturn(Mono.just(request));
        when(requestLoanRepository.save(any())).thenReturn(Mono.just(updatedRequest));
        when(tokenGateway.getToken()).thenReturn(Mono.just(token));
        when(userRepository.findByDocumentNumber(request.getDocumentNumber(), token)).thenReturn(Mono.just(user));
        when(messagePublisherGateway.publishLoanNotificationEmail(anyString())).thenReturn(Mono.just("message-id-123"));

        StepVerifier.create(useCase.updateStatus(requestId, newStatus))
                .expectNextMatches(response ->
                    response.getRequestId().equals(requestId) &&
                    response.getStatusName().equals(newStatus))
                .verifyComplete();

        verify(requestLoanRepository).save(argThat(req -> req.getStatusId().equals(StatusCode.REJECTED.id())));
        verify(messagePublisherGateway).publishLoanNotificationEmail(contains("REJECTED"));
    }

//    @Test
//    void shouldThrowException_WhenInvalidStatus() {
//        var requestId = 1L;
//        var invalidStatus = "PENDING";
//
//        StepVerifier.create(useCase.updateStatus(requestId, invalidStatus))
//                .expectErrorMatches(ex -> ex instanceof DomainValidationException &&
//                    ex.getMessage().equals("Invalid status. Only APPROVED or REJECTED allowed"))
//                .verify();
//
//        verifyNoInteractions(requestLoanRepository);
//    }

    @Test
    void shouldThrowException_WhenRequestNotFound() {
        var requestId = 999L;
        var newStatus = "APPROVED";

        when(requestLoanRepository.findById(requestId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.updateStatus(requestId, newStatus))
                .expectErrorMatches(ex -> ex instanceof DomainValidationException &&
                    ex.getMessage().equals("Request not found"))
                .verify();
    }

//@Test
//void shouldThrowException_WhenRequestAlreadyHasTargetStatus() {
//    var requestId = 1L;
//    var newStatus = "APPROVED";
//    var request = buildRequest(requestId, StatusCode.APPROVED.id());
//
//    when(requestLoanRepository.findById(requestId)).thenReturn(Mono.just(request));
//    // No configuramos save() porque no debería llegar a ese punto
//
//    StepVerifier.create(useCase.updateStatus(requestId, newStatus))
//            .expectErrorMatches(ex -> ex instanceof DomainValidationException &&
//                ex.getMessage().equals("Request already has status: APPROVED"))
//            .verify();
//
//    verify(requestLoanRepository, never()).save(any());
//}



@Test
void shouldThrowException_WhenTokenRetrievalFailsAfterStatusUpdate() {
    var requestId = 1L;
    var newStatus = "APPROVED";
    var request = buildPendingRequest(requestId);
    var updatedRequest = request.toBuilder().statusId(StatusCode.APPROVED.id()).build();

    when(requestLoanRepository.findById(requestId)).thenReturn(Mono.just(request));
    when(requestLoanRepository.save(any())).thenReturn(Mono.just(updatedRequest), Mono.just(request));
    when(tokenGateway.getToken()).thenReturn(Mono.error(new RuntimeException("Token error")));

    StepVerifier.create(useCase.updateStatus(requestId, newStatus))
            .expectErrorMatches(ex -> ex instanceof DomainValidationException &&
                ex.getMessage().equals("Status updated but notification failed. Changes reverted."))
            .verify();
}

@Test
void shouldThrowException_WhenUserNotFound() {
    var requestId = 1L;
    var newStatus = "APPROVED";
    var request = buildPendingRequest(requestId);
    var token = "mockToken";

    when(requestLoanRepository.findById(requestId)).thenReturn(Mono.just(request));
    when(requestLoanRepository.save(any())).thenReturn(Mono.just(request.toBuilder().statusId(StatusCode.APPROVED.id()).build()));
    when(tokenGateway.getToken()).thenReturn(Mono.just(token));
    when(userRepository.findByDocumentNumber(request.getDocumentNumber(), token)).thenReturn(Mono.empty());
    when(requestLoanRepository.save(request)).thenReturn(Mono.just(request));

    StepVerifier.create(useCase.updateStatus(requestId, newStatus))
            .expectErrorMatches(ex -> ex instanceof DomainValidationException &&
                ex.getMessage().equals("Status updated but notification failed. Changes reverted."))
            .verify();
}

    @Test
    void shouldRevertChanges_WhenNotificationFails() {
        var requestId = 1L;
        var newStatus = "APPROVED";
        var request = buildPendingRequest(requestId);
        var updatedRequest = request.toBuilder().statusId(StatusCode.APPROVED.id()).build();
        var user = buildUser();
        var token = "mockToken";

        when(requestLoanRepository.findById(requestId)).thenReturn(Mono.just(request));
        when(requestLoanRepository.save(any())).thenReturn(Mono.just(updatedRequest), Mono.just(request));
        when(tokenGateway.getToken()).thenReturn(Mono.just(token));
        when(userRepository.findByDocumentNumber(request.getDocumentNumber(), token)).thenReturn(Mono.just(user));
        when(messagePublisherGateway.publishLoanNotificationEmail(anyString())).thenReturn(Mono.error(new RuntimeException("SQS error")));

        StepVerifier.create(useCase.updateStatus(requestId, newStatus))
                .expectErrorMatches(ex -> ex instanceof DomainValidationException &&
                    ex.getMessage().equals("Status updated but notification failed. Changes reverted."))
                .verify();

        verify(requestLoanRepository, times(2)).save(any());
    }

    @Test
    void shouldBuildCorrectNotificationMessage_WhenProcessingApproval() {
        var requestId = 1L;
        var newStatus = "APPROVED";
        var request = buildPendingRequest(requestId);
        var updatedRequest = request.toBuilder().statusId(StatusCode.APPROVED.id()).build();
        var user = buildUser();
        var token = "mockToken";

        when(requestLoanRepository.findById(requestId)).thenReturn(Mono.just(request));
        when(requestLoanRepository.save(any())).thenReturn(Mono.just(updatedRequest));
        when(tokenGateway.getToken()).thenReturn(Mono.just(token));
        when(userRepository.findByDocumentNumber(request.getDocumentNumber(), token)).thenReturn(Mono.just(user));
        when(messagePublisherGateway.publishLoanNotificationEmail(anyString())).thenReturn(Mono.just("message-id-123"));

        StepVerifier.create(useCase.updateStatus(requestId, newStatus))
                .expectNextCount(1)
                .verifyComplete();

        verify(messagePublisherGateway).publishLoanNotificationEmail(argThat(message ->
            message.contains("\"requestId\": 1") &&
            message.contains("\"status\": \"APPROVED\"") &&
            message.contains("\"userClient\": \"Juan Pérez\"") &&
            message.contains("\"emailClient\": \"juan@test.com\"")
        ));
    }

    @Test
    void shouldAcceptCaseInsensitiveStatus_WhenValidStatus() {
        var requestId = 1L;
        var newStatus = "approved";
        var request = buildPendingRequest(requestId);
        var updatedRequest = request.toBuilder().statusId(StatusCode.APPROVED.id()).build();
        var user = buildUser();
        var token = "mockToken";

        when(requestLoanRepository.findById(requestId)).thenReturn(Mono.just(request));
        when(requestLoanRepository.save(any())).thenReturn(Mono.just(updatedRequest));
        when(tokenGateway.getToken()).thenReturn(Mono.just(token));
        when(userRepository.findByDocumentNumber(request.getDocumentNumber(), token)).thenReturn(Mono.just(user));
        when(messagePublisherGateway.publishLoanNotificationEmail(anyString())).thenReturn(Mono.just("message-id-123"));

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
}