package co.com.bancolombia.api.request.dto;

import co.com.bancolombia.model.domains.requestapplication.RequestApplication;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RequestApplicationMapper {
    RequestApplication fromRequest(RequestApplicationRecord requestDTO);
    RequestApplicationResponse toResponse(RequestApplication requestApplication);
}