package co.com.bancolombia.r2dbc.mapper;

import co.com.bancolombia.model.requestapplication.RequestApplication;
import co.com.bancolombia.r2dbc.entity.RequestApplicationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RequestApplicationEntityMapper {
    RequestApplicationEntity toEntity(RequestApplication model);
    RequestApplication toModel(RequestApplicationEntity entity);
}