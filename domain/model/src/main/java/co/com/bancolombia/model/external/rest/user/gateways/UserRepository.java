package co.com.bancolombia.model.external.rest.user.gateways;


import co.com.bancolombia.model.external.rest.user.dto.User;
import co.com.bancolombia.model.external.rest.user.dto.UserBasicInfo;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UserRepository {
   Mono<User> findByDocumentNumber(String documentNumber);
   Mono<List<UserBasicInfo>> findUsersByEmails(List<String> emails);
}
