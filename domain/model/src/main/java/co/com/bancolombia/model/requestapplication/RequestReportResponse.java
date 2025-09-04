package co.com.bancolombia.model.requestapplication;

import co.com.bancolombia.model.status.Status;
import co.com.bancolombia.model.typeloan.TypeLoan;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RequestReportResponse {
    private BigDecimal amount;
    private Integer term;
    private String email;
    private String firstName;
    private String lastName;
    private String loanTypeName;
    private String stateName;
    private Double interestRate;
//    private TypeLoan typeLoan;
//    private Status status;
    private BigDecimal baseSalary;
}
