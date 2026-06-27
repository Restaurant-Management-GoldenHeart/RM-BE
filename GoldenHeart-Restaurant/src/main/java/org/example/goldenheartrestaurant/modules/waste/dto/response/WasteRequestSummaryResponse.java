package org.example.goldenheartrestaurant.modules.waste.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class WasteRequestSummaryResponse {
    private Integer id;
    private Integer branchId;
    private String branchName;
    private Integer requestedById;
    private String requestedByName;
    private String status;
    private String note;
    private LocalDateTime createdAt;
}
