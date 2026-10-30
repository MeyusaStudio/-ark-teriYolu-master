import { Router, Response } from 'express';
import { authenticate, authorize, AuthRequest } from '../middleware/auth.middleware';
import { sequelize } from '../config/database';
import { selectQuery } from '../utils/dbHelpers';

const router = Router();
router.use(authenticate);
router.use(authorize('admin'));

// GET /api/admin/users - Kullanıcı listesi, filtreleme, arama ve istatistikler
router.get('/users', async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const { role, status, q } = req.query;

    let whereClause = '1=1';
    const params: unknown[] = [];

    // Rol filtreleme (hayvanci, sucukcu, market)
    if (role && role !== 'all') {
      whereClause += ' AND u.role = ?';
      params.push(role);
    }

    // Ban / Aktiflik filtreleme
    if (status === 'banned') {
      whereClause += ' AND u.is_banned = 1';
    } else if (status === 'active') {
      whereClause += ' AND u.is_banned = 0';
    }

    // Arama sorgusu (isim, e-posta, telefon)
    if (q && typeof q === 'string' && q.trim().length > 0) {
      const searchPattern = `%${q.trim()}%`;
      whereClause += ' AND (u.name LIKE ? OR u.email LIKE ? OR u.phone LIKE ?)';
      params.push(searchPattern, searchPattern, searchPattern);
    }

    const query = `
      SELECT 
        u.id,
        u.name,
        u.email,
        u.role,
        u.phone,
        u.address,
        u.is_active,
        COALESCE(u.is_banned, 0) AS is_banned,
        u.ban_reason,
        u.created_at,
        (SELECT COUNT(*) FROM lots l WHERE l.owner_id = u.id) AS lot_count,
        (SELECT COUNT(*) FROM orders o WHERE o.buyer_id = u.id OR o.seller_id = u.id) AS order_count,
        (SELECT COUNT(*) FROM market_purchases mp WHERE mp.market_id = u.id OR mp.farmer_id = u.id) AS purchase_count,
        (SELECT COUNT(*) FROM notifications n WHERE n.user_id = u.id AND n.is_read = 0) AS unread_notifications_count
      FROM users u
      WHERE ${whereClause}
      ORDER BY u.id DESC
    `;

    const users = await selectQuery(sequelize, query, params);

    // İstatistikler (Tüm kullanıcı havuzu üzerinden)
    const statsQuery = `
      SELECT 
        COUNT(*) AS total_users,
        SUM(CASE WHEN role = 'hayvanci' THEN 1 ELSE 0 END) AS hayvanci_count,
        SUM(CASE WHEN role = 'sucukcu' THEN 1 ELSE 0 END)  AS sucukcu_count,
        SUM(CASE WHEN role = 'market' THEN 1 ELSE 0 END)   AS market_count,
        SUM(CASE WHEN role = 'admin' THEN 1 ELSE 0 END)    AS admin_count,
        SUM(CASE WHEN is_banned = 1 THEN 1 ELSE 0 END)     AS banned_count
      FROM users
    `;
    const [stats] = await selectQuery<Record<string, unknown>>(sequelize, statsQuery, []);

    res.json({
      users,
      stats: {
        total: Number(stats?.total_users || 0),
        hayvanci: Number(stats?.hayvanci_count || 0),
        sucukcu: Number(stats?.sucukcu_count || 0),
        market: Number(stats?.market_count || 0),
        admin: Number(stats?.admin_count || 0),
        banned: Number(stats?.banned_count || 0),
      }
    });
  } catch (error) {
    console.error('Admin kullanıcıları listeleme hatası:', error);
    res.status(500).json({ error: 'Kullanıcılar alınamadı' });
  }
});

// PATCH /api/admin/users/:id/ban - Kullanıcıyı banla veya banını kaldır
router.patch('/users/:id/ban', async (req: AuthRequest, res: Response): Promise<void> => {
  const targetUserId = parseInt(req.params.id, 10);
  const { is_banned, ban_reason } = req.body;

  if (isNaN(targetUserId)) {
    res.status(400).json({ error: 'Geçersiz kullanıcı ID' });
    return;
  }

  // Admin kendisini banlayamaz
  if (targetUserId === req.user!.id) {
    res.status(400).json({ error: 'Yönetici kendi hesabını banlayamaz.' });
    return;
  }

  const t = await sequelize.transaction();
  try {
    const targetUsers = await selectQuery<{ id: number; name: string; role: string }>(
      sequelize,
      'SELECT id, name, role FROM users WHERE id = ?',
      [targetUserId],
      t
    );

    if (targetUsers.length === 0) {
      await t.rollback();
      res.status(404).json({ error: 'Kullanıcı bulunamadı' });
      return;
    }

    if (targetUsers[0].role === 'admin') {
      await t.rollback();
      res.status(403).json({ error: 'Yönetici hesapları banlanamaz' });
      return;
    }

    const bannedVal = is_banned ? 1 : 0;
    const reasonText = is_banned ? (ban_reason?.trim() || 'Yönetici tarafından askıya alındı') : null;

    await sequelize.query(
      `UPDATE users 
       SET is_banned = ?, 
           ban_reason = ?,
           updated_at = NOW()
       WHERE id = ?`,
      { replacements: [bannedVal, reasonText, targetUserId], transaction: t }
    );

    // Kullanıcıya bildirim gönder
    const notifTitle = is_banned ? '⛔ Hesabınız Askıya Alındı' : '✓ Hesabınızın Engeli Kaldırıldı';
    const notifMsg = is_banned
      ? `Hesabınız yönetici tarafından askıya alınmıştır. Gerekçe: ${reasonText}`
      : 'Hesabınızın erişim engeli yönetici tarafından kaldırılmıştır. Sisteme yeniden erişebilirsiniz.';

    await sequelize.query(
      `INSERT INTO notifications (user_id, type, title, message, reference_type, reference_id, is_read, created_at)
       VALUES (?, 'admin_uyari', ?, ?, 'admin', ?, 0, NOW())`,
      { replacements: [targetUserId, notifTitle, notifMsg, req.user!.id], transaction: t }
    );

    await t.commit();

    res.json({
      message: is_banned ? 'Kullanıcı başarıyla banlandı' : 'Kullanıcının banı kaldırıldı',
      userId: targetUserId,
      isBanned: is_banned,
      banReason: reasonText
    });
  } catch (error) {
    await t.rollback();
    console.error('Kullanıcı ban güncelleme hatası:', error);
    res.status(500).json({ error: 'İşlem gerçekleştirilemedi' });
  }
});

// POST /api/admin/notifications - Belirli bir kullanıcıya veya role mesaj/bildirim gönder
router.post('/notifications', async (req: AuthRequest, res: Response): Promise<void> => {
  const { user_id, user_ids, role, title, message: msgContent } = req.body;

  if (!title || !msgContent) {
    res.status(400).json({ error: 'Bildirim başlığı ve mesaj içeriği zorunludur' });
    return;
  }

  try {
    let targetUserIds: number[] = [];

    if (user_id) {
      targetUserIds = [parseInt(user_id, 10)];
    } else if (Array.isArray(user_ids) && user_ids.length > 0) {
      targetUserIds = user_ids.map(Number);
    } else if (role && role !== 'all') {
      const users = await selectQuery<{ id: number }>(
        sequelize,
        'SELECT id FROM users WHERE role = ? AND is_active = 1',
        [role]
      );
      targetUserIds = users.map(u => u.id);
    } else {
      // Tüm aktif kullanıcılara gönder (Admin hariç)
      const users = await selectQuery<{ id: number }>(
        sequelize,
        'SELECT id FROM users WHERE is_active = 1 AND id != ?',
        [req.user!.id]
      );
      targetUserIds = users.map(u => u.id);
    }

    if (targetUserIds.length === 0) {
      res.status(400).json({ error: 'Bildirim gönderilecek uygun kullanıcı bulunamadı' });
      return;
    }

    for (const uId of targetUserIds) {
      await sequelize.query(
        `INSERT INTO notifications (user_id, type, title, message, reference_type, reference_id, is_read, created_at)
         VALUES (?, 'admin_mesaj', ?, ?, 'admin', ?, 0, NOW())`,
        { replacements: [uId, title.trim(), msgContent.trim(), req.user!.id] }
      );
    }

    res.status(201).json({
      message: `${targetUserIds.length} kullanıcıya bildirim başarıyla iletildi`,
      deliveredCount: targetUserIds.length
    });
  } catch (error) {
    console.error('Admin bildirim gönderme hatası:', error);
    res.status(500).json({ error: 'Bildirim gönderilemedi' });
  }
});

// GET /api/admin/stats - Genel platform sistem istatistikleri
router.get('/stats', async (_req: AuthRequest, res: Response): Promise<void> => {
  try {
    const [userStats] = await selectQuery<Record<string, unknown>>(sequelize, 'SELECT COUNT(*) as cnt FROM users', []);
    const [lotStats] = await selectQuery<Record<string, unknown>>(sequelize, "SELECT COUNT(*) as cnt FROM lots WHERE status = 'active'", []);
    const [orderStats] = await selectQuery<Record<string, unknown>>(sequelize, "SELECT COUNT(*) as cnt FROM orders WHERE status = 'tamamlandi'", []);
    const [recallStats] = await selectQuery<Record<string, unknown>>(sequelize, "SELECT COUNT(*) as cnt FROM recalls WHERE status = 'aktif'", []);
    const [invoiceStats] = await selectQuery<Record<string, unknown>>(sequelize, "SELECT COUNT(*) as cnt, COALESCE(SUM(total_amount), 0) as total FROM invoices", []);

    res.json({
      totalUsers: Number(userStats?.cnt || 0),
      activeLots: Number(lotStats?.cnt || 0),
      completedOrders: Number(orderStats?.cnt || 0),
      activeRecalls: Number(recallStats?.cnt || 0),
      totalInvoices: Number(invoiceStats?.cnt || 0),
      totalRevenue: Number(invoiceStats?.total || 0),
    });
  } catch (error) {
    console.error('Admin stats hatası:', error);
    res.status(500).json({ error: 'İstatistikler alınamadı' });
  }
});

export default router;
