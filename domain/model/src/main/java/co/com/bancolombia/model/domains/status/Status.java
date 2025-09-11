package co.com.bancolombia.model.domains.status;
import lombok.*;
//import lombok.NoArgsConstructor;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Status {
    private Long id;
    private String name;
    private String description;
}
