package co.com.bancolombia.usecase.requestapplication;


import co.com.bancolombia.model.common.paginators.CustomPageResponseReport;
import co.com.bancolombia.model.common.paginators.PageableDomain;
import co.com.bancolombia.model.domains.requestapplication.RequestApplication;
import co.com.bancolombia.model.domains.requestapplication.dto.RequestReportResponse;
import reactor.core.publisher.Mono;



public interface RequestApplicationEvents {
    Mono<RequestApplication> applySave(RequestApplication requestApplication, String emailAuth);
    Mono<CustomPageResponseReport<RequestReportResponse>> applyFilterByStatus(PageableDomain pageable, Long statusId);
}
