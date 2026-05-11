package org.example.goldenheartrestaurant.modules.inventory.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class IngredientBranchCompatibilityRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("ingredients")) {
            return;
        }

        ensureBranchColumnExists();
        dropLegacyNameUniqueIndexes();
        migrateLegacyGlobalIngredients();
        ensureCompositeUniqueIndexExists();
    }

    private void ensureBranchColumnExists() {
        if (!columnExists("ingredients", "branch_id")) {
            jdbcTemplate.execute("ALTER TABLE ingredients ADD COLUMN branch_id INT NULL");
            log.info("Added ingredients.branch_id for branch-scoped ingredient migration");
        }
    }

    private void dropLegacyNameUniqueIndexes() {
        List<String> legacyIndexes = jdbcTemplate.queryForList("""
                select index_name
                from information_schema.statistics
                where table_schema = database()
                  and table_name = 'ingredients'
                  and non_unique = 0
                group by index_name
                having count(*) = 1
                   and max(case when column_name = 'name' then 1 else 0 end) = 1
                """, String.class);

        for (String indexName : legacyIndexes) {
            if ("PRIMARY".equalsIgnoreCase(indexName) || "uk_ingredients_branch_name".equalsIgnoreCase(indexName)) {
                continue;
            }
            jdbcTemplate.execute("ALTER TABLE ingredients DROP INDEX " + indexName);
            log.info("Dropped legacy ingredient unique index {}", indexName);
        }
    }

    private void migrateLegacyGlobalIngredients() {
        List<Map<String, Object>> legacyIngredients = jdbcTemplate.queryForList("""
                select id, name, unit_id, unit, description
                from ingredients
                where branch_id is null
                order by id
                """);

        for (Map<String, Object> row : legacyIngredients) {
            Integer legacyIngredientId = asInteger(row.get("id"));
            String ingredientName = asString(row.get("name"));
            Integer unitId = asInteger(row.get("unit_id"));
            String legacyUnit = asString(row.get("unit"));
            String description = asString(row.get("description"));

            List<Integer> branchIds = loadRelatedBranchIds(legacyIngredientId);
            if (branchIds.isEmpty()) {
                continue;
            }

            Integer reusableLegacyBranchId = null;

            for (Integer branchId : branchIds) {
                Integer targetIngredientId = findBranchScopedIngredientId(branchId, ingredientName);

                if (targetIngredientId == null) {
                    if (reusableLegacyBranchId == null) {
                        jdbcTemplate.update("""
                                update ingredients
                                set branch_id = ?
                                where id = ?
                                  and branch_id is null
                                """, branchId, legacyIngredientId);
                        reusableLegacyBranchId = branchId;
                        targetIngredientId = legacyIngredientId;
                    } else {
                        targetIngredientId = cloneIngredient(branchId, ingredientName, unitId, legacyUnit, description);
                    }
                }

                remapIngredientReferencesForBranch(legacyIngredientId, targetIngredientId, branchId);
            }
        }
    }

    private List<Integer> loadRelatedBranchIds(Integer ingredientId) {
        LinkedHashSet<Integer> branchIds = new LinkedHashSet<>();

        branchIds.addAll(jdbcTemplate.queryForList("""
                select distinct branch_id
                from inventory
                where ingredient_id = ?
                  and branch_id is not null
                """, Integer.class, ingredientId));

        branchIds.addAll(jdbcTemplate.queryForList("""
                select distinct branch_id
                from stock_movements
                where ingredient_id = ?
                  and branch_id is not null
                """, Integer.class, ingredientId));

        branchIds.addAll(jdbcTemplate.queryForList("""
                select distinct gr.branch_id
                from goods_receipt_items gri
                join goods_receipts gr on gr.id = gri.goods_receipt_id
                where gri.ingredient_id = ?
                  and gr.branch_id is not null
                """, Integer.class, ingredientId));

        branchIds.addAll(jdbcTemplate.queryForList("""
                select distinct ia.branch_id
                from inventory_adjustment_items iai
                join inventory_adjustments ia on ia.id = iai.inventory_adjustment_id
                where iai.ingredient_id = ?
                  and ia.branch_id is not null
                """, Integer.class, ingredientId));

        branchIds.addAll(jdbcTemplate.queryForList("""
                select distinct st.branch_id
                from stocktake_items sti
                join stocktakes st on st.id = sti.stocktake_id
                where sti.ingredient_id = ?
                  and st.branch_id is not null
                """, Integer.class, ingredientId));

        branchIds.addAll(jdbcTemplate.queryForList("""
                select distinct mi.branch_id
                from recipes r
                join menu_items mi on mi.id = r.menu_item_id
                where r.ingredient_id = ?
                  and mi.branch_id is not null
                """, Integer.class, ingredientId));

        return branchIds.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private Integer findBranchScopedIngredientId(Integer branchId, String ingredientName) {
        List<Integer> ids = jdbcTemplate.queryForList("""
                select id
                from ingredients
                where branch_id = ?
                  and lower(name) = lower(?)
                order by id
                limit 1
                """, Integer.class, branchId, ingredientName);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private Integer cloneIngredient(Integer branchId,
                                    String ingredientName,
                                    Integer unitId,
                                    String legacyUnit,
                                    String description) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into ingredients (name, branch_id, unit_id, unit, description)
                    values (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, ingredientName);
            statement.setInt(2, branchId);
            if (unitId != null) {
                statement.setInt(3, unitId);
            } else {
                statement.setNull(3, java.sql.Types.INTEGER);
            }
            statement.setString(4, legacyUnit);
            statement.setString(5, description);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to clone ingredient for branch migration: " + ingredientName);
        }
        return key.intValue();
    }

    private void remapIngredientReferencesForBranch(Integer legacyIngredientId,
                                                    Integer targetIngredientId,
                                                    Integer branchId) {
        if (Objects.equals(legacyIngredientId, targetIngredientId)) {
            return;
        }

        jdbcTemplate.update("""
                update inventory
                set ingredient_id = ?
                where ingredient_id = ?
                  and branch_id = ?
                """, targetIngredientId, legacyIngredientId, branchId);

        jdbcTemplate.update("""
                update stock_movements
                set ingredient_id = ?
                where ingredient_id = ?
                  and branch_id = ?
                """, targetIngredientId, legacyIngredientId, branchId);

        jdbcTemplate.update("""
                update goods_receipt_items gri
                join goods_receipts gr on gr.id = gri.goods_receipt_id
                set gri.ingredient_id = ?
                where gri.ingredient_id = ?
                  and gr.branch_id = ?
                """, targetIngredientId, legacyIngredientId, branchId);

        jdbcTemplate.update("""
                update inventory_adjustment_items iai
                join inventory_adjustments ia on ia.id = iai.inventory_adjustment_id
                set iai.ingredient_id = ?
                where iai.ingredient_id = ?
                  and ia.branch_id = ?
                """, targetIngredientId, legacyIngredientId, branchId);

        jdbcTemplate.update("""
                update stocktake_items sti
                join stocktakes st on st.id = sti.stocktake_id
                set sti.ingredient_id = ?
                where sti.ingredient_id = ?
                  and st.branch_id = ?
                """, targetIngredientId, legacyIngredientId, branchId);

        jdbcTemplate.update("""
                update recipes r
                join menu_items mi on mi.id = r.menu_item_id
                set r.ingredient_id = ?
                where r.ingredient_id = ?
                  and mi.branch_id = ?
                """, targetIngredientId, legacyIngredientId, branchId);
    }

    private void ensureCompositeUniqueIndexExists() {
        Integer count = jdbcTemplate.queryForObject("""
                select count(1)
                from information_schema.statistics
                where table_schema = database()
                  and table_name = 'ingredients'
                  and index_name = 'uk_ingredients_branch_name'
                """, Integer.class);

        if (count == null || count == 0) {
            jdbcTemplate.execute("""
                    ALTER TABLE ingredients
                    ADD CONSTRAINT uk_ingredients_branch_name UNIQUE (branch_id, name)
                    """);
            log.info("Created composite unique index uk_ingredients_branch_name on ingredients(branch_id, name)");
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

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null ? null : Integer.parseInt(value.toString());
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
