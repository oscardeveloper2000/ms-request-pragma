package co.com.bancolombia.api.request;

import co.com.bancolombia.api.config.RequestAppPath;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@RequiredArgsConstructor
public class RouterApplicationRest {
    private final RequestAppPath path;
    private final RequestApplicationHandler handler;
    @Bean
    public RouterFunction<ServerResponse> routerFunction(RequestApplicationHandler handler) {
        return route(POST(path.getRequest()), handler::listenPOSTUseCase)
                .andRoute(GET(path.getRequest()), handler::listenFilterByStatusId)
                .andRoute(PUT(path.getRequest() + "/{requestId}/status"), handler::listenPUTUpdateStatus);
    }
}
