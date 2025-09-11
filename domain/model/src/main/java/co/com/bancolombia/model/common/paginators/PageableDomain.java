package co.com.bancolombia.model.common.paginators;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class PageableDomain {
    private int page;
    private int size;
}
