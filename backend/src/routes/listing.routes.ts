import { Router } from 'express';
import { authenticate, authorize } from '../middleware/auth.middleware';
import { sequelize } from '../config/database';
import { AuthRequest } from '../middleware/auth.middleware';
import { insertQuery, selectQuery } from '../utils/dbHelpers';

const router = Router();
router.use(authenticate);

// ── SUCUKÇU İLANLARI ─────────────────────────────────────────────────────────

// Sucukçunun kendi ilanlarını getir  ← /producer/:id'den ÖNCE tanımlanmalı
router.get('/producer/mine', authorize('sucukcu'), async (req: AuthRequest, res) => {
  try {
    const listings = await selectQuery(
      sequelize,
      `SELECT * FROM producer_listings WHERE producer_id = ? ORDER BY created_at DESC`,
      [req.user!.id]
    );
    res.json(listings);
  } catch {
    res.status(500).json({ error: 'İlanlar alınamadı' });
  }
});

// Tüm aktif ilanları getir (hayvancılar görür)
router.get('/producer', async (_req, res) => {
  try {
    const listings = await selectQuery(
      sequelize,
      `SELECT pl.*, u.name as producer_name, u.phone, u.address
       FROM producer_listings pl
       JOIN users u ON pl.producer_id = u.id
       WHERE pl.is_active = 1
       ORDER BY pl.created_at DESC`,
      []
    );
    res.json(listings);
  } catch {
    res.status(500).json({ error: 'İlanlar alınamadı' });
  }
});

// Yeni ilan oluştur
router.post('/producer', authorize('sucukcu'), async (req: AuthRequest, res) => {
  const { product_name, description, service_price, min_order_qty, required_materials, currency } = req.body;
  const curr = currency ? String(currency).trim().toUpperCase() : 'EUR';
  try {
    const listingId = await insertQuery(
      sequelize,
      `INSERT INTO producer_listings (producer_id, product_name, description, service_price, service_unit, min_order_qty, required_materials, currency)
       VALUES (?, ?, ?, ?, 'paket', ?, ?, ?)`,
      [req.user!.id, product_name, description || null, service_price, min_order_qty || null, required_materials ? JSON.stringify(required_materials) : null, curr]
    );
    res.status(201).json({ message: 'İlan oluşturuldu', listingId });
  } catch (err) {
    console.error('Producer ilan oluşturma hatası:', err);
    res.status(500).json({ error: 'İlan oluşturulamadı' });
  }
});

// İlan güncelle
router.put('/producer/:id', authorize('sucukcu'), async (req: AuthRequest, res) => {
  const { description, service_price, servicePrice, min_order_qty, minOrderQty, required_materials, currency } = req.body;
  const price = service_price !== undefined ? service_price : servicePrice;
  const minQty = min_order_qty !== undefined ? min_order_qty : minOrderQty;
  const curr = currency ? String(currency).trim().toUpperCase() : null;
  try {
    await sequelize.query(
      `UPDATE producer_listings
       SET description = COALESCE(?, description),
           service_price = COALESCE(?, service_price),
           currency = COALESCE(?, currency),
           min_order_qty = COALESCE(?, min_order_qty),
           service_unit = 'paket',
           required_materials = CASE WHEN ? IS NOT NULL THEN ? ELSE required_materials END,
           updated_at = NOW()
       WHERE id = ? AND producer_id = ?`,
      { replacements: [
        description !== undefined ? description : null,
        price !== undefined ? price : null,
        curr,
        minQty !== undefined ? minQty : null,
        required_materials !== undefined ? 'yes' : null,
        required_materials ? JSON.stringify(required_materials) : null,
        req.params.id,
        req.user!.id,
      ]}
    );
    res.json({ message: 'İlan güncellendi' });
  } catch (err) {
    console.error('Producer ilan güncelleme hatası:', err);
    res.status(500).json({ error: 'İlan güncellenemedi' });
  }
});

// Aktif / Pasif toggle
const toggleProducer = async (req: AuthRequest, res: any) => {
  try {
    await sequelize.query(
      `UPDATE producer_listings SET is_active = CASE WHEN is_active = 1 THEN 0 ELSE 1 END, updated_at = NOW() WHERE id = ? AND producer_id = ?`,
      { replacements: [req.params.id, req.user!.id] }
    );
    res.json({ message: 'İlan durumu güncellendi' });
  } catch (e) {
    console.error('Producer ilan toggle hatası:', e);
    res.status(500).json({ error: 'Durum güncellenemedi' });
  }
};
router.patch('/producer/:id/toggle', authorize('sucukcu'), toggleProducer);
router.post('/producer/:id/toggle',  authorize('sucukcu'), toggleProducer);

// İlan sil
router.delete('/producer/:id', authorize('sucukcu'), async (req: AuthRequest, res) => {
  try {
    await sequelize.query(
      `DELETE FROM producer_listings WHERE id = ? AND producer_id = ?`,
      { replacements: [req.params.id, req.user!.id] }
    );
    res.json({ message: 'İlan silindi' });
  } catch {
    res.status(500).json({ error: 'İlan silinemedi' });
  }
});

// ── HAYVANCı İLANLARI ────────────────────────────────────────────────────────

// Hayvancının kendi ilanları (aktif + pasif)
router.get('/farmer/mine', authorize('hayvanci'), async (req: AuthRequest, res) => {
  try {
    const listings = await selectQuery(
      sequelize,
      `SELECT fl.*, l.lot_number, l.quantity as lot_quantity, l.status as lot_status, l.lot_type
       FROM farmer_listings fl
       JOIN lots l ON fl.lot_id = l.id
       WHERE fl.farmer_id = ? AND l.lot_type = 'mamul'
       ORDER BY fl.created_at DESC`,
      [req.user!.id]
    );
    res.json(listings);
  } catch {
    res.status(500).json({ error: 'İlanlar alınamadı' });
  }
});

// Tüm aktif hayvancı ilanları (marketler görür)
router.get('/farmer', async (_req, res) => {
  try {
    const listings = await selectQuery(
      sequelize,
      `SELECT fl.*, u.name as farmer_name, u.phone, l.lot_number, l.status as lot_status, l.lot_type
       FROM farmer_listings fl
       JOIN users u ON fl.farmer_id = u.id
       JOIN lots l ON fl.lot_id = l.id
       WHERE fl.is_active = 1 AND l.status = 'active' AND l.lot_type = 'mamul'
       ORDER BY fl.created_at DESC`,
      []
    );
    res.json(listings);
  } catch {
    res.status(500).json({ error: 'İlanlar alınamadı' });
  }
});

// Yeni hayvancı ilanı oluştur
router.post('/farmer', authorize('hayvanci'), async (req: AuthRequest, res) => {
  const { lot_id, product_name, quantity, unit, unit_price, currency, description } = req.body;
  const curr = currency ? String(currency).trim().toUpperCase() : 'EUR';
  try {
    const lotRows = await selectQuery<{ id: number; lot_type: string; status: string; quantity: number }>(
      sequelize,
      `SELECT id, lot_type, status, quantity FROM lots WHERE id = ? AND owner_id = ?`,
      [lot_id, req.user!.id]
    );

    if (lotRows.length === 0) {
      res.status(404).json({ error: 'Seçilen lot bulunamadı veya size ait değil' });
      return;
    }

    if (lotRows[0].lot_type !== 'mamul') {
      res.status(400).json({
        error: 'Satış sadece sucuk, salam, sosis, pastırma gibi fason mamul ürünler için yapılabilir. Çiğ et parçaları ilana koyulamaz.'
      });
      return;
    }

    const listingId = await insertQuery(
      sequelize,
      `INSERT INTO farmer_listings (farmer_id, lot_id, product_name, quantity, unit, unit_price, currency, description)
       VALUES (?, ?, ?, ?, 'paket', ?, ?, ?)`,
      [req.user!.id, lot_id, product_name, quantity, unit_price, curr, description || null]
    );
    res.status(201).json({ message: 'İlan oluşturuldu', listingId });
  } catch {
    res.status(500).json({ error: 'İlan oluşturulamadı' });
  }
});

// Hayvancı ilanı güncelle
router.put('/farmer/:id', authorize('hayvanci'), async (req: AuthRequest, res) => {
  const { unit_price, unitPrice, quantity, currency, description } = req.body;
  const price = unit_price !== undefined ? unit_price : unitPrice;
  const curr = currency ? String(currency).trim().toUpperCase() : null;
  try {
    await sequelize.query(
      `UPDATE farmer_listings 
       SET unit_price = COALESCE(?, unit_price), 
           currency = COALESCE(?, currency),
           quantity = COALESCE(?, quantity), 
           description = COALESCE(?, description), 
           updated_at = NOW()
       WHERE id = ? AND farmer_id = ?`,
      { replacements: [price, curr, quantity, description !== undefined ? description : null, req.params.id, req.user!.id] }
    );
    res.json({ message: 'İlan güncellendi' });
  } catch (e) {
    console.error('İlan güncelleme hatası:', e);
    res.status(500).json({ error: 'İlan güncellenemedi' });
  }
});

// Hayvancı ilanı aktif/pasif toggle
const toggleFarmer = async (req: AuthRequest, res: any) => {
  try {
    await sequelize.query(
      `UPDATE farmer_listings SET is_active = CASE WHEN is_active = 1 THEN 0 ELSE 1 END, updated_at = NOW()
       WHERE id = ? AND farmer_id = ?`,
      { replacements: [req.params.id, req.user!.id] }
    );
    res.json({ message: 'İlan durumu güncellendi' });
  } catch (e) {
    console.error('İlan toggle hatası:', e);
    res.status(500).json({ error: 'Durum güncellenemedi' });
  }
};
router.patch('/farmer/:id/toggle', authorize('hayvanci'), toggleFarmer);
router.post('/farmer/:id/toggle',  authorize('hayvanci'), toggleFarmer);

export default router;
