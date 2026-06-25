package org.example.goldenheartrestaurant.modules.inventory.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class InventoryPurchaseUnitCompatibilityRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (tableExists("ingredients")) {
            ensureColumn("ingredients", "default_purchase_unit_id", "ALTER TABLE ingredients ADD COLUMN default_purchase_unit_id INT NULL");
            ensureColumn("ingredients", "default_purchase_to_base_rate", "ALTER TABLE ingredients ADD COLUMN default_purchase_to_base_rate DECIMAL(12,6) NULL");
            ensureIndex("ingredients", "idx_ingredients_default_purchase_unit", "ALTER TABLE ingredients ADD INDEX idx_ingredients_default_purchase_unit (default_purchase_unit_id)");
        }

        if (tableExists("goods_receipt_items")) {
            ensureColumn("goods_receipt_items", "purchase_quantity", "ALTER TABLE goods_receipt_items ADD COLUMN purchase_quantity DECIMAL(12,2) NULL");
            ensureColumn("goods_receipt_items", "purchase_unit_id", "ALTER TABLE goods_receipt_items ADD COLUMN purchase_unit_id INT NULL");
            ensureColumn("goods_receipt_items", "conversion_rate", "ALTER TABLE goods_receipt_items ADD COLUMN conversion_rate DECIMAL(12,6) NULL");
            ensureColumn("goods_receipt_items", "converted_quantity", "ALTER TABLE goods_receipt_items ADD COLUMN converted_quantity DECIMAL(12,2) NULL");
            ensureColumn("goods_receipt_items", "base_unit_id", "ALTER TABLE goods_receipt_items ADD COLUMN base_unit_id INT NULL");
            ensureIndex("goods_receipt_items", "idx_goods_receipt_items_purchase_unit", "ALTER TABLE goods_receipt_items ADD INDEX idx_goods_receipt_items_purchase_unit (purchase_unit_id)");
            ensureIndex("goods_receipt_items", "idx_goods_receipt_items_base_unit", "ALTER TABLE goods_receipt_items ADD INDEX idx_goods_receipt_items_base_unit (base_unit_id)");
            backfillLegacyReceiptConversionColumns();
        }
    }

    private void backfillLegacyReceiptConversionColumns() {
        jdbcTemplate.update("""
                update goods_receipt_items gri
                join ingredients i on i.id = gri.ingredient_id
                set gri.purchase_quantity = coalesce(gri.purchase_quantity, gri.quantity),
                    gri.purchase_unit_id = coalesce(gri.purchase_unit_id, i.unit_id),
                    gri.conversion_rate = coalesce(gri.conversion_rate, 1),
                    gri.converted_quantity = coalesce(gri.converted_quantity, gri.quantity),
                    gri.base_unit_id = coalesce(gri.base_unit_id, i.unit_id)
                where gri.quantity is not null
                """);
    }

    private void ensureColumn(String tableName, String columnName, String ddl) {
        if (!columnExists(tableName, columnName)) {
            jdbcTemplate.execute(ddl);
            log.info("Added {}.{}", tableName, columnName);
        }
    }

    private void ensureIndex(String tableName, String indexName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(1)
                from information_schema.statistics
                where table_schema = database()
                  and table_name = ?
                  and index_name = ?
                """, Integer.class, tableName, indexName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
            log.info("Added index {} on {}", indexName, tableName);
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(1)
                from information_schema.tables
                where table_schema = database()
                  and table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(1)
                from information_schema.columns
                where table_schema = database()
                  and table_name = ?
                  and column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }
}
