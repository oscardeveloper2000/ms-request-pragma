//package co.com.bancolombia.model.auth;
//
//import reactor.core.publisher.Mono;
//
///**
// * Gateway interface for accessing authentication token information.
// */
//public interface TokenGateway {
//
//  /**
//   * Retrieves the current authentication token.
//   *
//   * @return a Mono emitting the token string
//   */
//  Mono<String> getToken();
//
//  /**
//   * Extracts the email from the subject of the current authentication token.
//   *
//   * @return a Mono emitting the email contained in the token
//   */
//  Mono<String> getEmailFromToken();
//}
