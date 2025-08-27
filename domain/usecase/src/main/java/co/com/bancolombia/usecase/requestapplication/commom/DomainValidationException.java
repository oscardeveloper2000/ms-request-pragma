package co.com.bancolombia.usecase.requestapplication.commom;

public class DomainValidationException extends RuntimeException {
    public DomainValidationException(String message) {
        super(message);
    }
}