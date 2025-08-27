package co.com.bancolombia.r2dbc.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table("type_loan")
public class TypeLoanEntity {

    @Id
    private Long id;

    @Column("name")
    private String name;

    @Column("min_amount")
    private BigDecimal minAmount;

    @Column("max_amount")
    private BigDecimal maxAmount;

    @Column("interest_rate")
    private Double interestRate;

    @Column("automatic_validation")
    private Boolean automaticValidation;
}