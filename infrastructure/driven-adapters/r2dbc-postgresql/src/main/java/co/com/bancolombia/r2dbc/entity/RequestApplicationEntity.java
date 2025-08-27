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
@Table("request_application")
public class RequestApplicationEntity {

    @Id
    private Long id;

    @Column("document_number")
    private String documentNumber;

    @Column("amount")
    private BigDecimal amount;

    @Column("term")
    private Integer term;

    @Column("email")
    private String email;

    // FKs (sin relaciones directas en R2DBC)
    @Column("loan_type_id")
    private Long loanTypeId;

    @Column("status_id")
    private Long statusId;
}