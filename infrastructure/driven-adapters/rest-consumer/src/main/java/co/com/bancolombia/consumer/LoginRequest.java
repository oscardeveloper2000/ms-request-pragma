package co.com.bancolombia.consumer;

public record LoginRequest(
        String email,
        String password
) {
}
