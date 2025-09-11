package co.com.bancolombia.model.external.rest.user.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserBasicInfo {
    private String email;
    private String firstName;
    private String lastName;
    private BigDecimal baseSalary;
}