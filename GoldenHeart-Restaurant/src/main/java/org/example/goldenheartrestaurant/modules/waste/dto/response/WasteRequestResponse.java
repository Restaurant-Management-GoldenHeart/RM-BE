package org.example.goldenheartrestaurant.modules.waste.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class WasteRequestResponse {
    private Integer id;
    private Integer branchId;
    private String branchName;
    private Integer requestedById;
    private String requestedByName;
    private Integer reviewedById;
    private String reviewedByName;
    private String status;
    private String note;
    private String reviewNote;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private List<String> imageUrls;
    private List<WasteRequestItemResponse> items;
}
