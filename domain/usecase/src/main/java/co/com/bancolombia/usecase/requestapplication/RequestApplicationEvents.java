package co.com.bancolombia.usecase.requestapplication;

import co.com.bancolombia.model.common.CustomPageResponseReport;
import co.com.bancolombia.model.common.PageResponse;
import co.com.bancolombia.model.requestapplication.PageRequest;
import co.com.bancolombia.model.requestapplication.RequestApplication;
import co.com.bancolombia.model.requestapplication.RequestReportResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;



public interface RequestApplicationEvents {
    Mono<RequestApplication> applySave(RequestApplication requestApplication);
    Mono<CustomPageResponseReport<RequestReportResponse>> applyFilterByStatus(PageRequest pageable, Long statusId, String token);
}
