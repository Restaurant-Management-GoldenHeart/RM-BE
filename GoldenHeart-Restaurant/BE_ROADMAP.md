# Backend Roadmap

## Muc tieu

Backend nen di theo thu tu phu thuoc nghiep vu, khong nen di theo thu tu ticket thuan tuy. Muc tieu la ra duoc mot flow van hanh nha hang hoan chinh som nhat:

1. dang nhap va phan quyen
2. cau hinh menu va so do quan
3. mo ban va tao order
4. bep xu ly mon
5. thanh toan va dong ban
6. kho, kiem ke va bao cao

## Thu tu uu tien de lam BE

### Phase 1: Nen tang he thong

1. `QLNH-15` Quan ly va chinh sua thong tin nhan vien
2. `Auth / Role / Permission` dang nhap, refresh token, phan quyen
3. `QLNH-16` Cai dat menu, gia va recipe
4. `QLNH-17` Cau hinh so do quan o muc master data

### Phase 2: Van hanh tai quan

1. `QLNH-1` Quan ly so do ban va mo ban
2. `QLNH-2` Tao don, goi mon
3. `QLNH-3` Gui order xuong bep
4. `QLNH-4` Dau bep xem danh sach mon che bien
5. `QLNH-8` Cap nhat trang thai mon
6. `QLNH-9` Huy mon va bao cao ly do
7. `QLNH-5` Xac nhan bung mon
8. `QLNH-6` In phieu tam tinh va xu ly thanh toan
9. `QLNH-10` Dong ban va ban giao ca

### Phase 3: Kho va gia von

1. `QLNH-12` Nhap hang vao kho
2. `QLNH-13` Kiem ke kho va xu ly chenh lech
3. `QLNH-14` Canh bao ton kho thap

### Phase 4: Quan tri va bao cao

1. `QLNH-18` Bao cao doanh thu, loi nhuan, kho
2. toi uu hoa quy trinh bep va van hanh

## Mapping module MVC

| Module | Nhiem vu chinh | Bang lien quan |
| --- | --- | --- |
| `auth` | dang ky, dang nhap, refresh, logout | `users`, `roles`, `refresh_tokens` |
| `identity` | nhan vien, ho so, phan quyen | `users`, `user_profiles`, `roles` |
| `restaurant` | chi nhanh, khu vuc, ban | `restaurants`, `branches`, `dining_areas`, `tables` |
| `menu` | danh muc, mon, recipe | `categories`, `menu_items`, `recipes` |
| `order` | mo ban, tao don, bep, bung mon | `orders`, `order_items`, `customers` |
| `billing` | tam tinh, thanh toan, dong ca tien mat | `bills`, `payments`, `cash_sessions`, `shift_handovers` |
| `inventory` | nhap kho, ton kho, kiem ke, dieu chinh | `ingredients`, `inventory`, `goods_receipts`, `stocktakes`, `inventory_adjustments`, `stock_movements` |
| `operations` | ca lam, ban giao ca | `work_shifts`, `cash_sessions`, `shift_handovers` |

## Data foundation can bo sung

### QLNH-17 Cau hinh so do quan

- `dining_areas`: khu vuc ban theo chi nhanh
- `tables`: bo sung `area_id`, `pos_x`, `pos_y`, `width`, `height`, `display_order`

### QLNH-10 Ban giao ca

- `work_shifts`: ca lam viec tai chi nhanh
- `cash_sessions`: phien mo ket tien mat
- `shift_handovers`: bien ban ban giao ca

### QLNH-12 Nhap hang vao kho

- `suppliers`: nha cung cap
- `goods_receipts`: phieu nhap kho
- `goods_receipt_items`: chi tiet nhap kho
- `stock_movements`: so cai bien dong kho

### QLNH-13 Kiem ke kho va xu ly chenh lech

- `stocktakes`: dot kiem ke
- `stocktake_items`: chi tiet kiem ke
- `inventory_adjustments`: phieu dieu chinh kho
- `inventory_adjustment_items`: chi tiet dieu chinh

### QLNH-14 Canh bao ton kho thap

- bo sung `min_stock_level`, `reorder_level` vao `inventory`

### QLNH-18 Bao cao loi nhuan

Khong nen luu bang report tong hop ngay tu dau. Bao cao loi nhuan nen duoc tinh tu:

- doanh thu tu `bills`, `payments`
- gia von tu `goods_receipt_items`, `stock_movements`
- snapshot nhanh cho bill tu `bills.cost_of_goods_sold`, `bills.gross_profit`

## Thu tu implement ky thuat

1. Hoan thien module `auth` va `identity`
2. Hoan thien `restaurant` + `menu`
3. Lam flow `order` + `billing`
4. Bo sung `operations`
5. Bo sung `inventory`
6. Cuoi cung moi lam dashboard va report query
