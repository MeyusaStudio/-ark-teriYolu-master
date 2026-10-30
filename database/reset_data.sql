-- ============================================================
-- AVATAR PLATFORMU - TEST VERİLERİNİ SIFIRLAMA SCRIPTI
-- Tablo yapılarına (şemalara) dokunmadan tüm verileri temizler.
-- ID sayaçlarını (AUTO_INCREMENT) 1'e sıfırlar.
-- ============================================================

-- 1. Yabancı Anahtar (Foreign Key) kontrollerini geçici olarak kapat
SET FOREIGN_KEY_CHECKS = 0;

-- 2. Faturalar ve Bildirimler
TRUNCATE TABLE invoices;
TRUNCATE TABLE notifications;

-- 3. Geri Çağırma (Recall) Tabloları
TRUNCATE TABLE recall_affected_markets;
TRUNCATE TABLE recalls;

-- 4. Pazar ve İlan Tabloları
TRUNCATE TABLE market_purchases;
TRUNCATE TABLE farmer_listings;
TRUNCATE TABLE producer_listings;

-- 5. Üretim ve Sipariş Tabloları
TRUNCATE TABLE production_finished_lots;
TRUNCATE TABLE production_raw_materials;
TRUNCATE TABLE production_orders;
TRUNCATE TABLE order_raw_materials;
TRUNCATE TABLE orders;

-- 6. Hayvan Parçalama ve Giriş Tabloları
TRUNCATE TABLE butchery_parts;
TRUNCATE TABLE butchery_operations;
TRUNCATE TABLE animals;

-- 7. Stok, Lot ve Hareket Tabloları
TRUNCATE TABLE lot_movements;
TRUNCATE TABLE raw_materials;
TRUNCATE TABLE lots;

-- 8. Yabancı Anahtar kontrollerini tekrar aç (Veri bütünlüğü için)
SET FOREIGN_KEY_CHECKS = 1;
