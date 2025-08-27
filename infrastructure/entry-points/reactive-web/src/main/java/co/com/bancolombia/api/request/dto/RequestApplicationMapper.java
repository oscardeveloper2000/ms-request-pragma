package co.com.bancolombia.api.request.dto;

import co.com.bancolombia.model.requestapplication.RequestApplication;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface RequestApplicationMapper {
    RequestApplication toModel(RequestApplicationRecord userDTO);
    RequestApplicationRecord toDTO(RequestApplication requestApplication);
}
