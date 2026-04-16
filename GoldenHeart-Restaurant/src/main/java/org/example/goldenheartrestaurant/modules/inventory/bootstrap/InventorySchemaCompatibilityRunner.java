//package org.example.goldenheartrestaurant.modules.inventory.bootstrap;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//
///**
// * Dọn phần schema cũ của bảng inventory để tương thích với cơ chế soft delete hiện tại.
// *
// * Vì dự án trước đây từng dùng unique cũ theo cặp (branch_id, ingredient_id),
// * nên khi chuyển sang soft delete + active_record_key, Hibernate update sẽ không tự xóa index cũ.
// * Hậu quả là:
// * - bản ghi inventory đã xóa mềm vẫn chặn việc tạo lại cùng branch + ingredient
// * - API create inventory trả DataIntegrityViolation dù logic service là đúng
// *
// * Runner này chỉ làm 3 việc an toàn:
// * 1. Xóa unique index cũ nếu nó còn tồn tại
// * 2. Đồng bộ active_record_key theo deleted_at cho dữ liệu cũ
// * 3. Tạo unique index mới nếu DB cũ chưa có
// */
//@Slf4j
//@Component
//@Order(0)
//@RequiredArgsConstructor
//public class InventorySchemaCompatibilityRunner implements ApplicationRunner {
//
//    private final JdbcTemplate jdbcTemplate;
//
//    @Override
//    public void run(ApplicationArguments args) {
//        dropLegacyInventoryUniqueIndexIfExists();
//        syncActiveRecordKeyForLegacyRows();
//        ensureActiveRecordUniqueIndexExists();
//    }
//
//    private void dropLegacyInventoryUniqueIndexIfExists() {
//        Integer count = jdbcTemplate.queryForObject("""
//                select count(1)
//                from information_schema.statistics
//                where table_schema = database()
//                  and table_name = 'inventory'
//                  and index_name = 'uk_inventory_branch_ingredient'
//                """, Integer.class);
//
//        if (count != null && count > 0) {
//            jdbcTemplate.execute("ALTER TABLE inventory DROP INDEX uk_inventory_branch_ingredient");
//            log.info("Da xoa unique index cu uk_inventory_branch_ingredient de soft delete inventory hoat dong dung");
//        }
//    }
//
//    private void syncActiveRecordKeyForLegacyRows() {
//        jdbcTemplate.update("""
//                update inventory
//                set active_record_key = 'ACTIVE'
//                where deleted_at is null
//                  and active_record_key is null
//                """);
//
//        jdbcTemplate.update("""
//                update inventory
//                set active_record_key = null
//                where deleted_at is not null
//                  and active_record_key is not null
//                """);
//    }
//
//    private void ensureActiveRecordUniqueIndexExists() {
//        Integer count = jdbcTemplate.queryForObject("""
//                select count(1)
//                from information_schema.statistics
//                where table_schema = database()
//                  and table_name = 'inventory'
//                  and index_name = 'uk_inventory_branch_ingredient_active'
//                """, Integer.class);
//
//        if (count == null || count == 0) {
//            jdbcTemplate.execute("""
//                    ALTER TABLE inventory
//                    ADD CONSTRAINT uk_inventory_branch_ingredient_active
//                    UNIQUE (branch_id, ingredient_id, active_record_key)
//                    """);
//            log.info("Da tao unique index moi uk_inventory_branch_ingredient_active cho inventory");
//        }
//    }
//}
