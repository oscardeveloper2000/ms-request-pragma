package co.com.bancolombia.usecase.requestapplication;

//import co.com.bancolombia.model.auth.TokenGateway;
import co.com.bancolombia.model.external.messaging.dto.CapacityValidationEventPublish;
import co.com.bancolombia.model.common.gateways.LoggerPort;
import co.com.bancolombia.model.common.paginators.CustomPageResponseReport;
import co.com.bancolombia.model.external.messaging.gateway.MessagePublisherGateway;
import co.com.bancolombia.model.common.paginators.PageableDomain;
import co.com.bancolombia.model.domains.requestapplication.RequestApplication;
import co.com.bancolombia.model.domains.requestapplication.dto.RequestReportResponse;
import co.com.bancolombia.model.domains.requestapplication.gateways.RequestApplicationRepository;
import co.com.bancolombia.model.domains.status.StatusCode;
import co.com.bancolombia.model.domains.status.gateways.StatusRepository;
import co.com.bancolombia.model.domains.typeloan.TypeLoan;
import co.com.bancolombia.model.domains.typeloan.gateways.TypeLoanRepository;
import co.com.bancolombia.model.external.rest.user.dto.User;
import co.com.bancolombia.model.external.rest.user.dto.UserBasicInfo;
import co.com.bancolombia.model.external.rest.user.gateways.UserRepository;
import co.com.bancolombia.usecase.commom.DomainValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RequestApplicationUseCase implements RequestApplicationEvents {
    private final RequestApplicationRepository requestLoanRepository;
    private final StatusRepository statusRepository;
    private final TypeLoanRepository typeLoanRepository;
    private final UserRepository userRepository;
    private final LoggerPort logger;
    private final MessagePublisherGateway messagePublisherGateway;

    @Override
  public Mono<RequestApplication> applySave(RequestApplication requestApplication, String emailAuth) {
      logger.info("applySave: inicio documentNumber={}, loanTypeId={}",
                  requestApplication.getDocumentNumber(), requestApplication.getLoanTypeId());
        logger.info("applySave: token obtenido exitosamente, email={}", emailAuth);
        String emailFromToken = emailAuth;
      // ✅ Validación inicial de entrada
      if (requestApplication == null) {
          logger.error("applySave: requestApplication es null");
          return Mono.error(new DomainValidationException("Request application is required"));
      }

      if (requestApplication.getDocumentNumber() == null || requestApplication.getDocumentNumber().trim().isEmpty()) {
          logger.error("applySave: documentNumber es null o vacío");
          return Mono.error(new DomainValidationException("Document number is required"));
      }

      if (requestApplication.getLoanTypeId() == null) {
          logger.error("applySave: loanTypeId es null");
          return Mono.error(new DomainValidationException("Loan type ID is required"));
      }

      return userRepository.findByDocumentNumber(requestApplication.getDocumentNumber())
                          .doOnError(e -> logger.error("applySave: error consultando usuario por documentNumber={}",
                                                     requestApplication.getDocumentNumber(), e))
                          .onErrorMap(e -> new DomainValidationException("Failed to fetch user information"))
                          .switchIfEmpty(Mono.defer(() -> {
                              logger.error("applySave: usuario no encontrado documentNumber={}",
                                         requestApplication.getDocumentNumber());
                              return Mono.error(new DomainValidationException("User not found in ms-auth"));
                          }))
                          .flatMap(user -> {
                              logger.info("applySave: usuario encontrado email={}", user.getEmail());
                              return validatePersonIdentity(emailFromToken, user.getEmail()).then(Mono.just(user));
                          })
                          .flatMap(user -> {
                              RequestApplication requestWithEmail = requestApplication.toBuilder()
                                      .email(user.getEmail())
                                      .statusId(StatusCode.PENDING.id())
                                      .build();

                              logger.info("applySave: iniciando validaciones de status y typeLoan");

                              return Mono.zip(
                                              statusRepository.findById(StatusCode.PENDING.id())
                                                      .doOnError(e -> logger.error("applySave: error consultando status PENDING", e))
                                                      .onErrorMap(e -> new DomainValidationException("Failed to fetch status information"))
                                                      .switchIfEmpty(Mono.defer(() -> {
                                                          logger.error("applySave: status PENDING no encontrado");
                                                          return Mono.error(new DomainValidationException("Status not found"));
                                                      })),
                                              typeLoanRepository.findById(requestWithEmail.getLoanTypeId())
                                                      .doOnError(e -> logger.error("applySave: error consultando typeLoan id={}",
                                                                                 requestWithEmail.getLoanTypeId(), e))
                                                      .onErrorMap(e -> new DomainValidationException("Failed to fetch loan type information"))
                                                      .switchIfEmpty(Mono.defer(() -> {
                                                          logger.error("applySave: typeLoan no encontrado id={}",
                                                                     requestWithEmail.getLoanTypeId());
                                                          return Mono.error(new DomainValidationException("TypeLoan not found"));
                                                      }))
                                      )
                                      .flatMap(tuple2 -> {
                                          var typeLoan = tuple2.getT2();
                                          logger.info("applySave: validando rango de monto amount={}, min={}, max={}",
                                                    requestWithEmail.getAmount(), typeLoan.getMinAmount(), typeLoan.getMaxAmount());

                                          try {
                                              validateAmountInRange(requestWithEmail.getAmount(), typeLoan.getMinAmount(), typeLoan.getMaxAmount());
                                          } catch (IllegalArgumentException e) {
                                              logger.error("applySave: validación de monto falló: {}", e.getMessage());
                                              return Mono.error(new DomainValidationException(e.getMessage()));
                                          }

                                          logger.info("applySave: guardando solicitud");
                                          return requestLoanRepository.save(requestWithEmail)
                                                  .doOnSuccess(saved -> logger.info("applySave: solicitud guardada exitosamente id={}", saved.getId()))
                                                  .doOnError(e -> logger.error("applySave: error guardando solicitud", e))
                                                  .onErrorMap(e -> new DomainValidationException("Failed to save request application"))
                                                  .flatMap(savedRequest -> {
                                                      if (Boolean.TRUE.equals(typeLoan.getAutomaticValidation())) {
                                                          logger.info("applySave: iniciando validación automática para solicitud id={}", savedRequest.getId());
                                                          return publishToCapacityValidationQueue(savedRequest, user, typeLoan)
                                                                  .then(Mono.just(savedRequest));
                                                      }
                                                      logger.info("applySave: validación manual requerida para solicitud id={}", savedRequest.getId());
                                                      return Mono.just(savedRequest);
                                                  });
                                      });
                          })

              .doOnSuccess(saved -> logger.info("applySave: proceso completado exitosamente id={}", saved.getId()))
              .doOnError(e -> logger.error("applySave: error final", e));
  }

  private Mono<Void> publishToCapacityValidationQueue(RequestApplication savedRequest, User user, TypeLoan typeLoan) {
      logger.info("publishToCapacityValidationQueue: inicio solicitudId={}, userEmail={}",
                 savedRequest.getId(), user.getEmail());

      // ✅ Validaciones de entrada
      if (savedRequest == null || savedRequest.getId() == null) {
          logger.error("publishToCapacityValidationQueue: savedRequest o su ID es null");
          return Mono.error(new DomainValidationException("Saved request is required"));
      }

      if (user == null || user.getBaseSalary() == null) {
          logger.error("publishToCapacityValidationQueue: user o baseSalary es null");
          return Mono.error(new DomainValidationException("User information with base salary is required"));
      }

      if (typeLoan == null || typeLoan.getInterestRate() == null) {
          logger.error("publishToCapacityValidationQueue: typeLoan o interestRate es null");
          return Mono.error(new DomainValidationException("Type loan information with interest rate is required"));
      }

      logger.info("publishToCapacityValidationQueue: consultando préstamos aprobados para email={}", savedRequest.getEmail());

      return requestLoanRepository.findByEmailAndStatusId(savedRequest.getEmail(), StatusCode.APPROVED.id())
              .doOnError(e -> logger.error("publishToCapacityValidationQueue: error consultando préstamos aprobados", e))
              .onErrorMap(e -> new DomainValidationException("Failed to fetch approved loans"))
              .collectList()
              .doOnSuccess(loans -> logger.info("publishToCapacityValidationQueue: préstamos aprobados encontrados={}", loans.size()))
              .flatMap(approvedLoans -> Flux.fromIterable(approvedLoans)
                      .flatMap(loan -> {
                          logger.debug("publishToCapacityValidationQueue: procesando préstamo aprobado id={}", loan.getId());
                          return typeLoanRepository.findById(loan.getLoanTypeId())
                                  .doOnError(e -> logger.error("publishToCapacityValidationQueue: error consultando typeLoan para préstamo id={}",
                                                             loan.getId(), e))
                                  .onErrorMap(e -> new DomainValidationException("Failed to fetch loan type for approved loan"))
                                  .switchIfEmpty(Mono.defer(() -> {
                                      logger.error("publishToCapacityValidationQueue: typeLoan no encontrado para préstamo id={}", loan.getId());
                                      return Mono.error(new DomainValidationException("Loan type not found for approved loan"));
                                  }))
                                  .map(loanType -> CapacityValidationEventPublish.ActiveLoan.builder()
                                          .id("LOAN-" + loan.getId())
                                          .amount(loan.getAmount())
                                          .interestRate(loanType.getInterestRate())
                                          .termMonths(loan.getTerm())
                                          .build());
                      })
                      .collectList()
                      .doOnSuccess(activeLoans -> logger.info("publishToCapacityValidationQueue: activeLoans procesados={}", activeLoans.size()))
                      .flatMap(activeLoans -> {
                          logger.info("publishToCapacityValidationQueue: creando evento de validación");
                          CapacityValidationEventPublish event = CapacityValidationEventPublish.builder()
                                  .solicitudId(savedRequest.getId())
                                  .totalIncome(user.getBaseSalary())
                                  .amount(savedRequest.getAmount())
                                  .termMonths(savedRequest.getTerm())
                                  .interestRate(typeLoan.getInterestRate())
                                  .applicantSalary(user.getBaseSalary())
                                  .activeLoans(activeLoans)
                                  .build();

                          logger.info("publishToCapacityValidationQueue: publicando evento a cola - solicitudId={}", event.getSolicitudId());
                          String message = buildNotificationMessage(event);
                          logger.debug("publishToCapacityValidationQueue: evento a publicar={}", message);
                          return messagePublisherGateway.publishLoanCalculateCapacity(message)
                                  .doOnSuccess(result -> logger.info("publishToCapacityValidationQueue: evento publicado exitosamente"))
                                  .doOnError(e -> logger.error("publishToCapacityValidationQueue: error publicando evento", e))
                                  .onErrorMap(e -> new DomainValidationException("Failed to publish capacity validation event"));
                      }))
              .doOnSuccess(v -> logger.info("publishToCapacityValidationQueue: proceso completado exitosamente"))
              .doOnError(e -> logger.error("publishToCapacityValidationQueue: error final", e));
  }

    private String buildNotificationMessage(CapacityValidationEventPublish event) {
        return String.format(
                "{\n" +
                        "  \"solicitudId\": \"%s\",\n" +
                        "  \"totalIncome\": %s,\n" +
                        "  \"amount\": %s,\n" +
                        "  \"termMonths\": %s,\n" +
                        "  \"interestRate\": %s,\n" +
                        "  \"applicantSalary\": %s,\n" +
                        "  \"activeLoans\": [%s]\n" +
                        "}",
                event.getSolicitudId(),
                event.getTotalIncome(),
                event.getAmount(),
                event.getTermMonths(),
                event.getInterestRate(),
                event.getApplicantSalary(),
                event.getActiveLoans() != null ? event.getActiveLoans().stream()
                        .map(loan -> String.format(
                                "{ \"id\": \"%s\", \"amount\": %s, \"interestRate\": %s, \"termMonths\": %s }",
                                loan.getId(),
                                loan.getAmount(),
                                loan.getInterestRate(),
                                loan.getTermMonths()
                        ))
                        .reduce((a, b) -> a + ", " + b).orElse("") : ""
        );
    }

@Override
public Mono<CustomPageResponseReport<RequestReportResponse>> applyFilterByStatus(PageableDomain pageable, Long statusId) {
    logger.info("applyFilterByStatus: inicio - statusId={}, page={}, size={}", statusId, pageable.getPage(), pageable.getSize());

    Mono<Long> totalElementsMono = requestLoanRepository.countByStatusId(statusId)
        .doOnSuccess(total -> logger.info("applyFilterByStatus: totalElements={}", total))
        .doOnError(e -> logger.error("applyFilterByStatus: error al contar elementos", e));

    Mono<List<RequestApplication>> paginatedLoansRequest = requestLoanRepository.findAllByStatusIdWithPageable(statusId, pageable)
        .collectList()
        .doOnSuccess(list -> logger.info("applyFilterByStatus: solicitudes encontradas={}", list.size()))
        .doOnError(e -> logger.error("applyFilterByStatus: error al obtener solicitudes", e));

    Mono<List<RequestApplication>> approvedLoansMono = requestLoanRepository.findAllByStatusId(StatusCode.APPROVED.id())
        .collectList()
        .doOnSuccess(list -> logger.info("applyFilterByStatus: préstamos aprobados encontrados={}", list.size()))
        .doOnError(e -> logger.error("applyFilterByStatus: error al obtener préstamos aprobados", e));

    return Mono.zip(totalElementsMono, paginatedLoansRequest, approvedLoansMono)
        .flatMap(tuple -> {
            long totalElements = tuple.getT1();
            List<RequestApplication> paginatedLoansRequestList = tuple.getT2();
            List<RequestApplication> approvedLoans = tuple.getT3();

            if (paginatedLoansRequestList == null) {
                logger.error("applyFilterByStatus: solicitudes es null");
                return Mono.error(new DomainValidationException("No se pudo obtener la lista de solicitudes"));
            }

            List<String> emails = paginatedLoansRequestList.stream().map(RequestApplication::getEmail).collect(Collectors.toList());
            logger.info("applyFilterByStatus: emails a consultar={}", emails);

            // Calcular cuota total mensual
            Mono<BigDecimal> totalMonthlyDebtMono = Flux.fromIterable(approvedLoans)
                .flatMap(loan -> typeLoanRepository.findById(loan.getLoanTypeId())
                    .switchIfEmpty(Mono.error(new DomainValidationException("TypeLoan no encontrado para préstamo aprobado id=" + loan.getId())))
                    .map(typeLoan -> {
                        BigDecimal amount = loan.getAmount();
                        int term = loan.getTerm();
                        BigDecimal interestRate = BigDecimal.valueOf(typeLoan.getInterestRate());
                        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), 10, BigDecimal.ROUND_HALF_UP);
                        BigDecimal onePlusRatePowTerm = BigDecimal.ONE.add(monthlyRate).pow(term);
                        BigDecimal cuota = amount.multiply(monthlyRate).multiply(onePlusRatePowTerm)
                            .divide(onePlusRatePowTerm.subtract(BigDecimal.ONE), 2, BigDecimal.ROUND_HALF_UP);
                        logger.debug("applyFilterByStatus: cuota calculada para préstamo id={}: {}", loan.getId(), cuota);
                        return cuota;
                    })
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doOnSuccess(total -> logger.info("applyFilterByStatus: totalMonthlyDebtOfApprovedLoans={}", total))
                .doOnError(e -> logger.error("applyFilterByStatus: error al calcular deuda mensual", e));

            return userRepository.findUsersByEmails(emails)
                .switchIfEmpty(Mono.error(new DomainValidationException("No se encontraron usuarios para los emails consultados")))
                .doOnSuccess(users -> logger.info("applyFilterByStatus: usuarios consultados={}", users.size()))
                .doOnError(e -> logger.error("applyFilterByStatus: error al consultar usuarios", e))
                .flatMap(users -> {
                    if (users == null) {
                        logger.error("applyFilterByStatus: users es null");
                        return Mono.error(new DomainValidationException("No se pudo obtener la lista de usuarios"));
                    }
                    Map<String, UserBasicInfo> userMap = users.stream()
                        .collect(Collectors.toMap(UserBasicInfo::getEmail, u -> u));

                    return Flux.fromIterable(paginatedLoansRequestList)
                        .flatMap(solicitud -> Mono.zip(
                            typeLoanRepository.findById(solicitud.getLoanTypeId())
                                .switchIfEmpty(Mono.error(new DomainValidationException("TypeLoan no encontrado para solicitud id=" + solicitud.getId()))),
                            statusRepository.findById(solicitud.getStatusId())
                                .switchIfEmpty(Mono.error(new DomainValidationException("Status no encontrado para solicitud id=" + solicitud.getId())))
                        ).map(tuple2 -> {
                            var typeLoan = tuple2.getT1();
                            var status = tuple2.getT2();
                            UserBasicInfo user = userMap.get(solicitud.getEmail());
                            logger.debug("applyFilterByStatus: armando respuesta para solicitud id={}", solicitud.getId());
                            return RequestReportResponse.builder()
                                .amount(solicitud.getAmount())
                                .term(solicitud.getTerm())
                                .email(solicitud.getEmail())
                                .firstName(user != null ? user.getFirstName() : null)
                                .lastName(user != null ? user.getLastName() : null)
                                .stateName(status.getName())
                                .loanTypeName(typeLoan.getName())
                                .interestRate(typeLoan.getInterestRate())
                                .baseSalary(user != null ? user.getBaseSalary() : null)
                                .build();
                        }))
                        .collectList()
                        .zipWith(totalMonthlyDebtMono)
                        .map(tuple3 -> {
                            List<RequestReportResponse> content = tuple3.getT1();
                            BigDecimal totalMonthlyDebt = tuple3.getT2();
                            int totalPages = (int) Math.ceil((double) totalElements / pageable.getSize());
                            boolean first = pageable.getPage() == 0;
                            boolean last = pageable.getPage() == (totalPages - 1);

                            logger.info("applyFilterByStatus: respuesta armada - content={}, totalPages={}, first={}, last={}",
                                content.size(), totalPages, first, last);

                            return CustomPageResponseReport.<RequestReportResponse>customBuilder()
                                .content(content)
                                .page(pageable.getPage())
                                .size(pageable.getSize())
                                .totalElements(totalElements)
                                .totalPages(totalPages)
                                .first(first)
                                .last(last)
                                .totalMonthlyDebtOfApprovedLoans(totalMonthlyDebt)
                                .build();
                        });
                });
        })
        .doOnError(e -> logger.error("applyFilterByStatus: error final", e));
}

    private void validateAmountInRange(BigDecimal amount, BigDecimal min, BigDecimal max) {
        if (amount == null) throw new IllegalArgumentException("Amount is required");
        if (min != null && amount.compareTo(min) < 0)
            throw new IllegalArgumentException("Amount below minimum: " + amount + " < " + min);
        if (max != null && amount.compareTo(max) > 0)
            throw new IllegalArgumentException("Amount above maximum: " + amount + " > " + max);
    }


    private Mono<Void> validatePersonIdentity(String emailFromToken, String emailFromUser) {
        if (!emailFromToken.equals(emailFromUser)) {
            return Mono.error(new DomainValidationException("The user does not have permissions to make this request for email: " + emailFromUser));
        }

        return Mono.empty();
    }

}