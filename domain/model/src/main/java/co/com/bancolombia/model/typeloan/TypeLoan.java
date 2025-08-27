package co.com.bancolombia.model.typeloan;
import lombok.*;
//import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TypeLoan {
    private Long id;
    private String name;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Double interestRate;
    private Boolean automaticValidation;

}
