package co.com.bancolombia.model.common;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CustomPageResponseReport<T> extends PageResponse<T> {
    private BigDecimal totalMonthlyDebtOfApprovedLoans;

    @Builder(builderMethodName = "customBuilder")
    public CustomPageResponseReport(List<T> content, int page, int size, long totalElements, int totalPages, boolean first, boolean last, BigDecimal totalMonthlyDebtOfApprovedLoans) {
        super(content, page, size, totalElements, totalPages, first, last);
        this.totalMonthlyDebtOfApprovedLoans = totalMonthlyDebtOfApprovedLoans;
    }
}