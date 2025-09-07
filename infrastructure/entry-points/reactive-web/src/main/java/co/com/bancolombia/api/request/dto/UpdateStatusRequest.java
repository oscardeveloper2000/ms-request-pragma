
package co.com.bancolombia.api.request.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateStatusRequest(
    @JsonProperty("newStatus")
    String newStatus
) {
}