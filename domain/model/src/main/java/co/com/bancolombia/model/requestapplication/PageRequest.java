package co.com.bancolombia.model.requestapplication;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class PageRequest {
    private int page;
    private int size;
}
