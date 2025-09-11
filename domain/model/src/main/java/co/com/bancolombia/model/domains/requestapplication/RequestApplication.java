package co.com.bancolombia.model.domains.requestapplication;
import lombok.*;
//import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RequestApplication {
    private Long id;
    private String documentNumber;
    private BigDecimal amount;
    private Integer term;
    private String email;
    private Long loanTypeId;
    private Long statusId;
}
