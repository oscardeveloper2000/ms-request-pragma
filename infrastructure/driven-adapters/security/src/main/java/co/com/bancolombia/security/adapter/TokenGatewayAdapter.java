package co.com.bancolombia.security.adapter;



import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class TokenGatewayAdapter  {


  public Mono<String> getToken() {
    return ReactiveSecurityContextHolder.getContext()
            .doOnError(e -> log.info("TokenGatewayAdapter.getToken: context={}", e))
        .map(SecurityContext::getAuthentication)
            .doOnError(e -> log.error("TokenGatewayAdapter.getToken: error1", e))
        .map(Authentication::getPrincipal)
            .doOnError(e -> log.error("TokenGatewayAdapter.getToken: error2", e))
        .cast(Jwt.class)
        .map(Jwt::getTokenValue);
  }


  public Mono<String> getEmailFromToken() {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .map(Authentication::getPrincipal)
        .cast(Jwt.class)
        .map(JwtClaimAccessor::getSubject);
  }
}
