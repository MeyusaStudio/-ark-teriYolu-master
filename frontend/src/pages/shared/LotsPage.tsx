import { useEffect, useState, useMemo } from 'react';
import {
  Table, Tag, Typography, Button, Space, message, Input,
  Card, Collapse, Row, Col, Empty,
} from 'antd';
import {
  EyeOutlined, ApartmentOutlined, SearchOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { lotService } from '../../services/api';
import { Lot } from '../../types';
import { useTranslation } from '../../i18n';
import { formatUnit, formatDbStatus, formatDbPart } from '../../i18n/formatters';

const { Title, Text } = Typography;

const statusColors: Record<string, string> = {
  active:   'green',
  blocked:  'orange',
  recalled: 'red',
  consumed: 'default',
  sold:     'blue',
};

// Yalnızca mamuller için (sucuk, salam, sosis, pastırma) izlenebilirlik sunulur
function isMamul(lot: Lot): boolean {
  const t = (lot.lot_type || '').toLowerCase();
  const name = (lot.product_name || '').toLowerCase();
  return t === 'mamul' ||
    name.includes('sucuk') ||
    name.includes('salam') ||
    name.includes('sosis') ||
    name.includes('pastırma') ||
    name.includes('pastirma');
}

function getAnimalEmoji(animalType?: string): string {
  const norm = (animalType || '').toLowerCase();
  if (norm.includes('inek') || norm.includes('dana')) return '🐄';
  if (norm.includes('koyun') || norm.includes('kuzu')) return '🐑';
  if (norm.includes('tavuk')) return '🐔';
  return '🥩';
}

interface AnimalLotGroup {
  earTag: string;
  animalType?: string;
  butcheryDate?: string;
  totalKg: number;
  parts: Lot[];
}

export default function LotsPage() {
  const { t } = useTranslation();
  const [lots, setLots] = useState<Lot[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    lotService.getAll()
      .then(r => setLots(r.data))
      .catch(() => message.error('Lotlar yüklenemedi'))
      .finally(() => setLoading(false));
  }, []);

  // ── Arama Filtrelemesi ───────────────────────────────────────────────────
  const filteredLots = useMemo(() => {
    if (!searchQuery.trim()) return lots;
    const q = searchQuery.toLowerCase().trim();
    return lots.filter(l =>
      l.lot_number?.toLowerCase().includes(q) ||
      (l.product_name && l.product_name.toLowerCase().includes(q)) ||
      (l.animal_ear_tag && l.animal_ear_tag.toLowerCase().includes(q)) ||
      (l.supplier_name && l.supplier_name.toLowerCase().includes(q))
    );
  }, [lots, searchQuery]);

  // ── Hayvan Lot Numarasına Göre Gruplandırma ──────────────────────────────
  const { animalGroups, otherLots } = useMemo(() => {
    const groupsMap = new Map<string, AnimalLotGroup>();
    const others: Lot[] = [];

    filteredLots.forEach(lot => {
      // Parça veya hayvan küpe numarasına sahipse hayvan bazında gruplandır
      const tag = lot.animal_ear_tag || (lot.lot_type === 'parca' ? (lot.butchery_operation_id ? `Kesim Operasyonu #${lot.butchery_operation_id}` : 'Diğer Parçalar') : null);

      if (tag) {
        if (!groupsMap.has(tag)) {
          groupsMap.set(tag, {
            earTag: tag,
            animalType: lot.animal_type || (tag.includes('KUZU') ? 'koyun' : tag.includes('DANA') ? 'inek' : 'Et'),
            butcheryDate: lot.butchery_date || lot.production_date || lot.created_at,
            totalKg: 0,
            parts: [],
          });
        }
        const g = groupsMap.get(tag)!;
        g.parts.push(lot);
        g.totalKg += Number(lot.quantity || 0);
      } else {
        others.push(lot);
      }
    });

    return {
      animalGroups: Array.from(groupsMap.values()),
      otherLots: others,
    };
  }, [filteredLots]);

  // ── Parça Tablosu Sütunları (İleri/Geri İzleme BUTONU YOK) ─────────────
  const partColumns = [
    {
      title: 'Parça Lot No',
      dataIndex: 'lot_number',
      key: 'lot_number',
      render: (v: string) => <Tag color="blue" style={{ fontWeight: 600 }}>{v}</Tag>,
    },
    {
      title: 'Parça Adı',
      dataIndex: 'product_name',
      key: 'product_name',
      render: (v: string) => <strong>{formatDbPart(v || 'Et Parçası', t)}</strong>,
    },
    {
      title: 'Ağırlık',
      key: 'qty',
      render: (_: unknown, r: Lot) => <Text strong>{Number(r.quantity).toFixed(2)} kg</Text>,
    },
    {
      title: 'Birim Maliyet',
      dataIndex: 'unit_cost',
      key: 'unit_cost',
      render: (v: unknown, r: Lot) => Number(v) > 0 ? `${Number(v).toFixed(2)} ${r.currency || '€'}/kg` : '-',
    },
    {
      title: 'Toplam Tutar',
      dataIndex: 'total_cost',
      key: 'total_cost',
      render: (v: unknown, r: Lot) => Number(v) > 0 ? <Text strong style={{ color: '#52c41a' }}>{Number(v).toFixed(2)} {r.currency || '€'}</Text> : '-',
    },
    {
      title: 'Durum',
      dataIndex: 'status',
      key: 'status',
      render: (v: string) => <Tag color={statusColors[v] ?? 'default'}>{formatDbStatus(v, t)}</Tag>,
    },
  ];

  // ── Diğer Lotlar Sütunları (Yalnızca Mamuller için İzleme Açık) ──────────
  const otherColumns = [
    {
      title: t('common.lotNumber'),
      dataIndex: 'lot_number',
      key: 'lot_number',
      render: (v: string) => <strong>{v}</strong>,
    },
    {
      title: 'Ürün / Malzeme',
      dataIndex: 'product_name',
      key: 'product_name',
      render: (v: string, r: Lot) => v ? formatDbPart(v, t) : r.lot_type,
    },
    {
      title: 'Tür',
      dataIndex: 'lot_type',
      key: 'lot_type',
      render: (v: string) => <Tag color={v === 'mamul' ? 'purple' : 'orange'}>{v}</Tag>,
    },
    {
      title: t('common.quantity'),
      key: 'qty',
      render: (_: unknown, r: Lot) => `${Number(r.quantity).toFixed(2)} ${formatUnit(r.unit, t)}`,
    },
    {
      title: t('common.unitPrice'),
      dataIndex: 'unit_cost',
      key: 'unit_cost',
      render: (v: unknown, r: Lot) => Number(v) > 0 ? `${Number(v).toFixed(2)} ${r.currency || '€'}` : '-',
    },
    {
      title: t('common.totalCost'),
      dataIndex: 'total_cost',
      key: 'total_cost',
      render: (v: unknown, r: Lot) => Number(v) > 0 ? `${Number(v).toFixed(2)} ${r.currency || '€'}` : '-',
    },
    {
      title: t('common.status'),
      dataIndex: 'status',
      key: 'status',
      render: (v: string) => <Tag color={statusColors[v] ?? 'default'}>{formatDbStatus(v, t)}</Tag>,
    },
    {
      title: t('common.date'),
      dataIndex: 'created_at',
      key: 'created_at',
      render: (v: string) => v ? new Date(v).toLocaleDateString() : '-',
    },
    {
      title: t('common.actions'),
      key: 'action',
      render: (_: unknown, r: Lot) => {
        // İleri ve geri izleme sadece mamul ürünler (sucuk, salam vb.) için gösterilir
        if (isMamul(r)) {
          return (
            <Space>
              <Button size="small" icon={<EyeOutlined />} onClick={() => navigate(`/lots/${r.id}/trace`)}>
                {t('lots.traceForward')}
              </Button>
              <Button size="small" icon={<ApartmentOutlined />} onClick={() => navigate(`/lots/${r.id}/trace`)}>
                {t('lots.traceBackward')}
              </Button>
            </Space>
          );
        }
        return <Text type="secondary">-</Text>;
      },
    },
  ];

  return (
    <div>
      {/* Başlık ve Arama */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12, marginBottom: 16 }}>
        <div>
          <Title level={3} style={{ margin: 0 }}>🏷️ {t('nav.lots')}</Title>
          <Text type="secondary" style={{ fontSize: 13 }}>
            Hayvan lotlarına göre gruplandırılmış parçalar ve mamul stokları
          </Text>
        </div>

        {/* Lot Arama Çubuğu */}
        <Input
          placeholder="Lot no, küpe no veya ürün adı ile ara..."
          prefix={<SearchOutlined style={{ color: '#8c8c8c' }} />}
          value={searchQuery}
          onChange={e => setSearchQuery(e.target.value)}
          allowClear
          style={{ width: 340, borderRadius: 8 }}
        />
      </div>

      {/* ── 1. HAYVAN LOT NUMARASINA GÖRE GRUPLANDIRILMIŞ PARÇALAR ── */}
      <div style={{ marginBottom: 28 }}>
        <Title level={4} style={{ color: '#1f2937', marginBottom: 12 }}>
          🥩 Hayvan Lot Numarasına Göre Parçalar
        </Title>

        {animalGroups.length === 0 && !loading ? (
          <Card size="small" style={{ borderRadius: 8, textAlign: 'center', padding: '24px 0' }}>
            <Empty description="Kriterlere uygun parçalanmış hayvan lotu bulunamadı." />
          </Card>
        ) : (
          <Collapse
            defaultActiveKey={animalGroups.map(g => g.earTag)}
            style={{ background: 'transparent', border: 'none' }}
          >
            {animalGroups.map(group => {
              const emoji = getAnimalEmoji(group.animalType);
              return (
                <Collapse.Panel
                  key={group.earTag}
                  style={{
                    background: '#ffffff',
                    borderRadius: 10,
                    marginBottom: 12,
                    border: '1px solid #e5e7eb',
                    overflow: 'hidden',
                  }}
                  header={
                    <Row style={{ width: '100%' }} align="middle" justify="space-between">
                      <Col>
                        <Space size="middle">
                          <span style={{ fontSize: 22 }}>{emoji}</span>
                          <div>
                            <Text strong style={{ fontSize: 15, color: '#111827' }}>
                              Hayvan Küpe / Lot No: <span style={{ color: '#1d4ed8' }}>{group.earTag}</span>
                            </Text>
                            <div style={{ fontSize: 12, color: '#6b7280' }}>
                              Tür: {group.animalType?.toUpperCase() || 'HAYVAN'} · Kesim Tarihi: {group.butcheryDate ? new Date(group.butcheryDate).toLocaleDateString() : '-'}
                            </div>
                          </div>
                        </Space>
                      </Col>
                      <Col style={{ marginRight: 16 }}>
                        <Space>
                          <Tag color="cyan" style={{ fontSize: 12, padding: '2px 8px' }}>
                            {group.parts.length} Parça
                          </Tag>
                          <Tag color="green" style={{ fontSize: 12, padding: '2px 8px' }}>
                            Toplam: {group.totalKg.toFixed(2)} kg
                          </Tag>
                        </Space>
                      </Col>
                    </Row>
                  }
                >
                  <Table
                    dataSource={group.parts}
                    columns={partColumns}
                    rowKey="id"
                    pagination={false}
                    size="small"
                  />
                </Collapse.Panel>
              );
            })}
          </Collapse>
        )}
      </div>

      {/* ── 2. DİĞER LOTLAR (MAMUL VE HAMMADDE) ── */}
      {otherLots.length > 0 && (
        <div>
          <Title level={4} style={{ color: '#1f2937', marginBottom: 12 }}>
            🌭 Mamul ve Genel Stok Lotları
          </Title>
          <Table
            dataSource={otherLots}
            columns={otherColumns}
            rowKey="id"
            loading={loading}
            pagination={{ pageSize: 10 }}
            size="middle"
          />
        </div>
      )}
    </div>
  );
}
