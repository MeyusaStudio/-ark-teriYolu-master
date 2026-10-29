import { Router } from 'express';
import { authenticate } from '../middleware/auth.middleware';
import {
  getLots,
  getLotById,
  getLotMovements,
  traceForward,
  traceBackward,
  sellLot,
} from '../controllers/lot.controller';

const router = Router();

router.use(authenticate);

router.get('/',                    getLots);
router.post('/:id/sell',           sellLot);        // Mobil Satış (Görev 2 & 3)
router.get('/:id',                 getLotById);
router.get('/:id/movements',       getLotMovements);
router.get('/:id/trace/forward',   traceForward);   // İleri izleme
router.get('/:id/trace/backward',  traceBackward);  // Geri izleme

export default router;
