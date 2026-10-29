-- ============================================================
-- AVATAR: Parçalama Tablosu Bağımsız Hayvan Türü ve animal_id Desteği
-- ============================================================

-- butchery_operations tablosunda animal_type sütununu ekle
ALTER TABLE butchery_operations
ADD COLUMN IF NOT EXISTS animal_type VARCHAR(50) NOT NULL DEFAULT 'koyun' COMMENT 'Hayvan türü (inek, koyun, tavuk)';

-- Hayvan girişi bağımlılığı kaldırıldığı için animal_id alanını opsiyonel yap
ALTER TABLE butchery_operations
MODIFY COLUMN animal_id INT UNSIGNED NULL;
