import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { sequelize } from '../config/database';
import { QueryTypes } from 'sequelize';

export interface AuthRequest extends Request {
  user?: {
    id: number;
    email: string;
    role: 'hayvanci' | 'sucukcu' | 'market' | 'admin';
  };
}

export async function authenticate(req: AuthRequest, res: Response, next: NextFunction): Promise<void> {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    res.status(401).json({ error: 'Token gerekli' });
    return;
  }

  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET || 'secret') as AuthRequest['user'];
    if (!decoded) {
      res.status(401).json({ error: 'Geçersiz token' });
      return;
    }

    // Kullanıcının ban durumunu kontrol et
    const users = await sequelize.query<{ is_banned: number; ban_reason: string | null }>(
      'SELECT is_banned, ban_reason FROM users WHERE id = ?',
      { replacements: [decoded.id], type: QueryTypes.SELECT }
    );

    if (users.length > 0 && users[0].is_banned === 1) {
      res.status(403).json({
        error: users[0].ban_reason
          ? `Hesabınız yönetici tarafından askıya alınmıştır. Gerekçe: ${users[0].ban_reason}`
          : 'Hesabınız yönetici tarafından askıya alınmıştır (banlandı).'
      });
      return;
    }

    req.user = decoded;
    next();
  } catch {
    res.status(401).json({ error: 'Geçersiz token' });
  }
}

export function authorize(...roles: string[]) {
  return (req: AuthRequest, res: Response, next: NextFunction): void => {
    if (!req.user || !roles.includes(req.user.role)) {
      res.status(403).json({ error: 'Bu işlem için yetkiniz yok' });
      return;
    }
    next();
  };
}
