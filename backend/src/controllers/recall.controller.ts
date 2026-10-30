import { Response } from 'express';
import { sequelize } from '../config/database';
import { AuthRequest } from '../middleware/auth.middleware';
import { insertQuery, selectQuery } from '../utils/dbHelpers';

export async function initiateRecall(req: AuthRequest, res: Response): Promise<void> {
  const lotId = req.body.lotId ?? req.body.lot_id;
  const { reason } = req.body;

  if (!lotId || !reason) {
    res.status(400).json({ error: 'lotId ve reason zorunludur' });
    return;
  }

  const t = await sequelize.transaction();
  try {
    const lots = await selectQuery<{ id: number; quantity: number; unit: string; owner_id: number; status: string; lot_type: string }>(
      sequelize,
      `SELECT DISTINCT l.* FROM lots l
       LEFT JOIN market_purchases mp ON l.id = mp.lot_id
       WHERE l.id = ? AND (l.owner_id = ? OR mp.farmer_id = ?)`,
      [lotId, req.user!.id, req.user!.id],
      t
    );

    if (lots.length === 0) {
      await t.rollback();
      res.status(404).json({ error: 'Lot bulunamadı veya yetkiniz yok' });
      return;
    }

    if (lots[0].lot_type !== 'mamul') {
      await t.rollback();
      res.status(400).json({ error: 'Yalnızca marketlere satılan mamul ürünler geri çağrılabilir.' });
      return;
    }

    if (lots[0].status === 'recalled') {
      await t.rollback();
      res.status(400).json({ error: 'Bu lot zaten geri çağrılmış ve bloke edilmiştir.' });
      return;
    }

    const existingRecall = await selectQuery<{ id: number }>(
      sequelize,
      "SELECT id FROM recalls WHERE lot_id = ? AND status = 'aktif'",
      [lotId],
      t
    );
    if (existingRecall.length > 0) {
      await t.rollback();
      res.status(400).json({ error: 'Bu lot için zaten aktif bir geri çağırma işlemi bulunmaktadır.' });
      return;
    }

    // Market satışlarını bul (Sadece marketlere satılan mamuller geri çağrılabilir)
    const purchases = await selectQuery<{ id: number; market_id: number; quantity: number; unit: string; lot_id: number }>(
      sequelize,
      `SELECT DISTINCT mp.id, mp.market_id, mp.quantity, mp.unit, mp.lot_id
       FROM market_purchases mp
       WHERE mp.lot_id = ?`,
      [lotId],
      t
    );

    if (purchases.length === 0) {
      await t.rollback();
      res.status(400).json({ error: 'Bu mamul için market satışı bulunmadığından geri çağırma yapılamaz.' });
      return;
    }

    const recallId = await insertQuery(
      sequelize,
      "INSERT INTO recalls (initiated_by, lot_id, reason, status) VALUES (?, ?, ?, 'aktif')",
      [req.user!.id, lotId, reason],
      t
    );

    for (const purchase of purchases) {
      await insertQuery(
        sequelize,
        `INSERT INTO recall_affected_markets (recall_id, market_id, purchase_id, quantity, unit, status, notified_at)
         VALUES (?, ?, ?, ?, ?, 'bildirildi', NOW())`,
        [recallId, purchase.market_id, purchase.id, purchase.quantity, purchase.unit],
        t
      );

      // Etkilenen marketlere acil geri çağırma bildirimi gönder
      await insertQuery(
        sequelize,
        `INSERT INTO notifications (user_id, type, title, message, reference_type, reference_id)
         VALUES (?, 'geri_cagirma', 'ACİL: Ürün Geri Çağırma (Recall)', ?, 'recall', ?)`,
        [purchase.market_id, `Satın aldığınız ürün için geri çağırma başlatıldı. Gerekçe: ${reason}. Lütfen ürünü reyonlardan çekiniz.`, recallId],
        t
      );
    }

    // Bu lota ait TÜM market satışlarının durumunu 'geri_cagrildi' yap
    const purchaseIds = purchases.map(p => p.id);
    if (purchaseIds.length > 0) {
      await sequelize.query(
        `UPDATE market_purchases SET status = 'geri_cagrildi' WHERE id IN (?)`,
        { replacements: [purchaseIds], transaction: t }
      );
    }

    // Bu lotla ilgili kalite sorunu bildirimlerini okundu/işlendi yap
    await sequelize.query(
      `UPDATE notifications SET is_read = 1 WHERE reference_id = ? AND type = 'kalite_sorunu'`,
      { replacements: [lotId], transaction: t }
    );

    // Lotu ve satıştaki ilanını derhal bloke/recalled yap
    await sequelize.query(
      "UPDATE lots SET status = 'recalled' WHERE id = ?",
      { replacements: [lotId], transaction: t }
    );

    await sequelize.query(
      'UPDATE farmer_listings SET is_active = 0 WHERE lot_id = ?',
      { replacements: [lotId], transaction: t }
    );

    await insertQuery(
      sequelize,
      `INSERT INTO lot_movements (lot_id, movement_type, quantity, unit, reference_type, reference_id, from_user_id, notes)
       SELECT id, 'bloke', quantity, unit, 'recall', ?, owner_id, ? FROM lots WHERE id = ?`,
      [recallId, reason, lotId],
      t
    );

    await sequelize.query(
      'UPDATE recalls SET affected_count = ? WHERE id = ?',
      { replacements: [purchases.length, recallId], transaction: t }
    );

    await t.commit();

    res.status(201).json({
      message: 'Geri çağırma başlatıldı',
      recallId,
      affectedMarkets: purchases.length,
      lotBlocked: true,
    });
  } catch (err) {
    console.error('Geri çağırma başlatılırken hata:', err);
    await t.rollback();
    res.status(500).json({ error: 'Geri çağırma başlatılamadı' });
  }
}

export async function getRecallableLots(req: AuthRequest, res: Response): Promise<void> {
  try {
    const lots = await selectQuery(
      sequelize,
      `SELECT DISTINCT l.id, l.lot_number, l.lot_type, l.quantity, l.unit, l.status,
              COALESCE(fl.product_name, pfl_po.product_name, l.product_name, 'Mamul Ürün') as product_name,
              COUNT(mp.id) as market_sale_count
       FROM lots l
       JOIN market_purchases mp ON l.id = mp.lot_id
       LEFT JOIN farmer_listings fl ON mp.listing_id = fl.id
       LEFT JOIN production_finished_lots pfl ON l.id = pfl.lot_id
       LEFT JOIN production_orders pfl_po ON pfl.production_order_id = pfl_po.id
       WHERE (l.owner_id = ? OR mp.farmer_id = ?)
         AND l.lot_type = 'mamul'
         AND l.status != 'recalled'
       GROUP BY l.id, l.lot_number, l.lot_type, l.quantity, l.unit, l.status, product_name
       ORDER BY l.id DESC`,
      [req.user!.id, req.user!.id]
    );
    res.json(lots);
  } catch (err) {
    console.error('Geri çağrılabilir lotlar alınamadı:', err);
    res.status(500).json({ error: 'Geri çağrılabilir lotlar alınamadı' });
  }
}

export async function getRecalls(req: AuthRequest, res: Response): Promise<void> {
  try {
    const recalls = await selectQuery(
      sequelize,
      `SELECT r.*, l.lot_number, u.name as initiated_by_name
       FROM recalls r
       JOIN lots l ON r.lot_id = l.id
       JOIN users u ON r.initiated_by = u.id
       WHERE r.initiated_by = ? OR l.owner_id = ?
       ORDER BY r.created_at DESC`,
      [req.user!.id, req.user!.id]
    );
    res.json(recalls);
  } catch {
    res.status(500).json({ error: 'Geri çağırmalar alınamadı' });
  }
}

export async function getRecallById(req: AuthRequest, res: Response): Promise<void> {
  try {
    const recalls = await selectQuery(
      sequelize,
      `SELECT r.*, l.lot_number, u.name as initiated_by_name
       FROM recalls r
       JOIN lots l ON r.lot_id = l.id
       JOIN users u ON r.initiated_by = u.id
       WHERE r.id = ?`,
      [req.params.id]
    );

    if (recalls.length === 0) {
      res.status(404).json({ error: 'Geri çağırma bulunamadı' });
      return;
    }

    const affectedMarkets = await selectQuery(
      sequelize,
      `SELECT ram.*, u.name as market_name, u.phone as market_phone, u.address as market_address
       FROM recall_affected_markets ram
       JOIN users u ON ram.market_id = u.id
       WHERE ram.recall_id = ?`,
      [req.params.id]
    );

    res.json({ recall: recalls[0], affectedMarkets });
  } catch {
    res.status(500).json({ error: 'Geri çağırma detayı alınamadı' });
  }
}

export async function completeRecall(req: AuthRequest, res: Response): Promise<void> {
  try {
    await sequelize.query(
      "UPDATE recalls SET status = 'tamamlandi' WHERE id = ? AND initiated_by = ?",
      { replacements: [req.params.id, req.user!.id] }
    );
    res.json({ message: 'Geri çağırma tamamlandı' });
  } catch {
    res.status(500).json({ error: 'Geri çağırma güncellenemedi' });
  }
}
