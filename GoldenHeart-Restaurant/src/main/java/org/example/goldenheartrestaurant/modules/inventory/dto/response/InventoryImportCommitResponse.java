package org.example.goldenheartrestaurant.modules.inventory.dto.response;

public record InventoryImportCommitResponse(
        boolean committed,
        Integer goodsReceiptId,
        String receiptCode,
        InventoryImportPreviewResponse preview
) {
}