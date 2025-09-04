package co.com.bancolombia.model.user.gateways;

import co.com.bancolombia.model.user.User;
import co.com.bancolombia.model.user.UserBasicInfo;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UserRepository {
   Mono<User> findByDocumentNumber(String documentNumber, String token);
   Mono<List<UserBasicInfo>> findUsersByEmails(List<String> emails, String token);
}
