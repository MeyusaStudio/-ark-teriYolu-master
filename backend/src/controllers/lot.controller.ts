import { Response } from 'express';
import { sequelize } from '../config/database';
import { AuthRequest } from '../middleware/auth.middleware';
import { insertQuery, selectQuery } from '../utils/dbHelpers';

export async function getLots(req: AuthRequest, res: Response): Promise<void> {
  try {
    const lots = await selectQuery(
      sequelize,
      `SELECT l.*, u.name as owner_name,
              COALESCE(bp.part_name, po.product_name, fl.product_name, l.lot_type) as product_name
       FROM lots l
       JOIN users u ON l.owner_id = u.id
       LEFT JOIN butchery_parts bp ON l.id = bp.lot_id
       LEFT JOIN production_finished_lots pfl ON l.id = pfl.lot_id
       LEFT JOIN production_orders po ON pfl.production_order_id = po.id
       LEFT JOIN farmer_listings fl ON l.id = fl.lot_id
       WHERE l.owner_id = ?
       ORDER BY l.created_at DESC`,
      [req.user!.id]
    );
    res.json(lots);
  } catch {
    res.status(500).json({ error: 'Lotlar alınamadı' });
  }
}

export async function getLotById(req: AuthRequest, res: Response): Promise<void> {
  try {
    const lots = await selectQuery(
      sequelize,
      `SELECT l.*, u.name as owner_name
       FROM lots l
       JOIN users u ON l.owner_id = u.id
       WHERE l.id = ?`,
      [req.params.id]
    );

    if (lots.length === 0) {
      res.status(404).json({ error: 'Lot bulunamadı' });
      return;
    }
    res.json(lots[0]);
  } catch {
    res.status(500).json({ error: 'Lot bilgisi alınamadı' });
  }
}

export async function getLotMovements(req: AuthRequest, res: Response): Promise<void> {
  try {
    const movements = await selectQuery(
      sequelize,
      `SELECT lm.*,
              fu.name as from_user_name,
              tu.name as to_user_name
       FROM lot_movements lm
       LEFT JOIN users fu ON lm.from_user_id = fu.id
       LEFT JOIN users tu ON lm.to_user_id = tu.id
       WHERE lm.lot_id = ?
       ORDER BY lm.created_at ASC`,
      [req.params.id]
    );
    res.json(movements);
  } catch {
    res.status(500).json({ error: 'Lot hareketleri alınamadı' });
  }
}

export async function traceForward(req: AuthRequest, res: Response): Promise<void> {
  try {
    const lotId = req.params.id;

    const lots = await selectQuery(sequelize, 'SELECT * FROM lots WHERE id = ?', [lotId]);
    if (lots.length === 0) {
      res.status(404).json({ error: 'Lot bulunamadı' });
      return;
    }

    const productionOrders = await selectQuery(
      sequelize,
      `SELECT po.*, prm.quantity_used
       FROM production_orders po
       JOIN production_raw_materials prm ON po.id = prm.production_order_id
       WHERE prm.lot_id = ?`,
      [lotId]
    );

    const finishedLots = await selectQuery(
      sequelize,
      `SELECT l.*, pfl.production_order_id
       FROM lots l
       JOIN production_finished_lots pfl ON l.id = pfl.lot_id
       JOIN production_raw_materials prm ON pfl.production_order_id = prm.production_order_id
       WHERE prm.lot_id = ?`,
      [lotId]
    );

    const marketSales = await selectQuery(
      sequelize,
      `SELECT DISTINCT mp.*, u.name as market_name, u.address as market_address, u.phone as market_phone, l.lot_number
       FROM market_purchases mp
       JOIN users u ON mp.market_id = u.id
       JOIN lots l ON mp.lot_id = l.id
       LEFT JOIN production_finished_lots pfl ON mp.lot_id = pfl.lot_id
       LEFT JOIN production_raw_materials prm ON pfl.production_order_id = prm.production_order_id
       WHERE prm.lot_id = ? OR mp.lot_id = ?`,
      [lotId, lotId]
    );

    res.json({
      sourceLot:        lots[0],
      productionOrders,
      finishedLots,
      marketSales,
      summary: {
        productionOrderCount: productionOrders.length,
        finishedLotCount:     finishedLots.length,
        affectedMarketCount:  marketSales.length,
      },
    });
  } catch {
    res.status(500).json({ error: 'İleri izleme yapılamadı' });
  }
}

export async function traceBackward(req: AuthRequest, res: Response): Promise<void> {
  try {
    const lotId = req.params.id;

    const lots = await selectQuery(sequelize, 'SELECT * FROM lots WHERE id = ?', [lotId]);
    if (lots.length === 0) {
      res.status(404).json({ error: 'Lot bulunamadı' });
      return;
    }

    const productionOrders = await selectQuery(
      sequelize,
      `SELECT po.*
       FROM production_orders po
       JOIN production_finished_lots pfl ON po.id = pfl.production_order_id
       WHERE pfl.lot_id = ?`,
      [lotId]
    );

    const rawMaterialLots = await selectQuery<{ supplier_name: string }>(
      sequelize,
      `SELECT l.*, prm.quantity_used, prm.material_type, prm.unit_price, prm.total_cost
       FROM lots l
       JOIN production_raw_materials prm ON l.id = prm.lot_id
       JOIN production_finished_lots pfl ON prm.production_order_id = pfl.production_order_id
       WHERE pfl.lot_id = ?`,
      [lotId]
    );

    res.json({
      finishedLot:      lots[0],
      productionOrders,
      rawMaterialLots,
      summary: {
        productionOrderCount: productionOrders.length,
        rawMaterialLotCount:  rawMaterialLots.length,
        suppliers: [...new Set(rawMaterialLots.map(r => r.supplier_name).filter(Boolean))],
      },
    });
  } catch {
    res.status(500).json({ error: 'Geri izleme yapılamadı' });
  }
}

// ── Mobil Satış (Şartname Görev 2 & Görev 3: Bloke Satış Engeli) ──────────────
export async function sellLot(req: AuthRequest, res: Response): Promise<void> {
  const lotId = req.params.id;
  const { customer_name, quantity, unit_price, currency, notes } = req.body;
  const curr = currency ? String(currency).trim().toUpperCase() : 'EUR';
  const qty = Number(quantity);
  const price = Number(unit_price);

  if (!qty || qty <= 0 || !price || price <= 0 || !customer_name) {
    res.status(400).json({ error: 'Müşteri adı, miktar ve birim fiyat zorunludur' });
    return;
  }

  const t = await sequelize.transaction();
  try {
    const lots = await selectQuery<{
      id: number;
      lot_number: string;
      lot_type: string;
      quantity: number;
      unit: string;
      status: string;
      owner_id: number;
      unit_cost: number;
      total_cost: number;
    }>(
      sequelize,
      'SELECT * FROM lots WHERE id = ?',
      [lotId],
      t
    );

    if (lots.length === 0) {
      await t.rollback();
      res.status(404).json({ error: 'Lot bulunamadı' });
      return;
    }

    const lot = lots[0];

    // Görev 3: Geri çağrılan / bloke lotların satışı KESİNLİKLE ENGELLENİR
    if (lot.status === 'recalled' || lot.status === 'blocked') {
      await t.rollback();
      res.status(403).json({
        error: `DİKKAT: ${lot.lot_number} numaralı lot geri çağrılmış veya bloke edilmiştir! Satışı kesinlikle yapılamaz.`,
        is_blocked: true,
        lot_number: lot.lot_number,
        status: lot.status,
      });
      return;
    }

    if (lot.status !== 'active') {
      await t.rollback();
      res.status(400).json({ error: `Bu lot satışa uygun değil (Durum: ${lot.status})` });
      return;
    }

    if (Number(lot.quantity) < qty) {
      await t.rollback();
      res.status(400).json({ error: `Yetersiz stok! Mevcut miktar: ${Number(lot.quantity).toFixed(2)} ${lot.unit}` });
      return;
    }

    const totalPrice = parseFloat((qty * price).toFixed(2));
    const newQuantity = parseFloat((Number(lot.quantity) - qty).toFixed(3));
    const newStatus = newQuantity <= 0 ? 'consumed' : 'active';

    // 1. Lot stok miktarını düşür
    await sequelize.query(
      'UPDATE lots SET quantity = ?, status = ?, updated_at = NOW() WHERE id = ?',
      { replacements: [newQuantity, newStatus, lot.id], transaction: t }
    );

    // 2. Lot hareketi kaydet (movement_type: 'satis')
    const movementId = await insertQuery(
      sequelize,
      `INSERT INTO lot_movements (lot_id, movement_type, quantity, unit, reference_type, reference_id, from_user_id, notes)
       VALUES (?, 'satis', ?, ?, 'mobile_sale', ?, ?, ?)`,
      [lot.id, qty, lot.unit, lot.id, req.user!.id, `${customer_name} - ${notes || 'Mobil Satış'}`],
      t
    );

    // 3. Fatura oluştur (invoices)
    const invCount = await selectQuery<{ cnt: number }>(
      sequelize,
      "SELECT COUNT(*) as cnt FROM invoices WHERE invoice_type = 'satis'",
      [],
      t
    );
    const invSeq = (invCount[0]?.cnt || 0) + 1;
    const invoiceNumber = `FTR-SAT-${new Date().getFullYear()}-${String(invSeq).padStart(3, '0')}`;

    // Ürün adını tespit et
    let productName = `${lot.lot_type.toUpperCase()} - ${lot.lot_number}`;
    const partRows = await selectQuery<{ part_name: string }>(
      sequelize,
      'SELECT part_name FROM butchery_parts WHERE lot_id = ? LIMIT 1',
      [lot.id],
      t
    );
    if (partRows.length > 0 && partRows[0]?.part_name) {
      productName = partRows[0].part_name;
    }

    await insertQuery(
      sequelize,
      `INSERT INTO invoices (
        invoice_number, invoice_type, sender_id, receiver_id, reference_type, reference_id,
        lot_number, product_name, description, quantity, unit, unit_price, subtotal,
        tax_rate, tax_amount, total_amount, currency, status, invoice_date
      ) VALUES (?, 'satis', ?, ?, 'mobile_sale', ?, ?, ?, ?, ?, ?, ?, ?, 0.00, 0.00, ?, ?, 'odendi', NOW())`,
      [
        invoiceNumber,
        req.user!.id,
        req.user!.id,
        movementId,
        lot.lot_number,
        productName,
        `Mobil Satış: ${customer_name}`,
        qty,
        lot.unit,
        price,
        totalPrice,
        totalPrice,
        curr,
      ],
      t
    );

    await t.commit();

    res.status(201).json({
      message: 'Satış başarıyla tamamlandı ve stoktan düşüldü',
      invoice_number: invoiceNumber,
      lot_id: lot.id,
      lot_number: lot.lot_number,
      product_name: productName,
      sold_quantity: qty,
      unit: lot.unit,
      unit_price: price,
      total_price: totalPrice,
      currency: curr,
      remaining_quantity: newQuantity,
      customer_name,
    });
  } catch (error) {
    await t.rollback();
    const errMsg = error instanceof Error ? error.message : String(error);
    console.error('MOBILE_SALE_HATA:', errMsg);
    res.status(500).json({ error: 'Satış işlemi kaydedilemedi', detail: errMsg });
  }
}
