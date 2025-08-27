package co.com.bancolombia.model.user;
import lombok.*;
//import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class User {
    private Long id;
    private String documentNumber;
    private String firstName;
    private String lastName;
    private Timestamp birthDate;
    private String address;
    private String phone;
    private String email;
    private BigDecimal baseSalary;
}
