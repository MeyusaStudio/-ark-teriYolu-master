import { useEffect, useState, useMemo } from 'react';
import {
  Table, Tag, Typography, Button, Space, Card, Row, Col,
  Statistic, Empty, App, Alert, Tabs, Modal, Descriptions, Badge, Input,
} from 'antd';
import {
  DollarCircleOutlined, ShopOutlined, WarningOutlined,
  EyeOutlined, PhoneOutlined, EnvironmentOutlined, CheckCircleOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { useNavigate } from 'react-router-dom';
import { purchaseService, invoiceService, recallService } from '../../services/api';
import { Invoice } from '../../types';
import { InvoiceModal } from '../../components/InvoiceModal';
import { formatMoney, convertToEur } from '../../utils/currency';
import { useTranslation } from '../../i18n';
import { formatDbProduct } from '../../i18n/formatters';

const { Title, Text } = Typography;

interface SaleItem {
  id: number;
  market_id: number;
  market_name: string;
  market_phone?: string;
  market_address?: string;
  listing_id: number;
  product_name?: string;
  lot_id: number;
  lot_number: string;
  lot_status: string;
  quantity: number;
  unit: string;
  unit_price: number;
  total_price: number;
  currency?: string;
  status: string;
  purchase_date: string;
  complaint_count?: number;
  latest_complaint?: string;
}

interface ComplaintItem {
  id: number;
  title: string;
  message: string;
  lot_id: number;
  lot_number: string;
  lot_status: string;
  lot_quantity: number;
  lot_unit: string;
  is_read: boolean | number;
  created_at: string;
}

export default function FarmerSalesPage() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const navigate = useNavigate();

  const [sales, setSales]               = useState<SaleItem[]>([]);
  const [complaints, setComplaints]     = useState<ComplaintItem[]>([]);
  const [loading, setLoading]           = useState(true);

  // Detay modalı
  const [detailOpen, setDetailOpen]     = useState(false);
  const [selectedSale, setSelectedSale] = useState<SaleItem | null>(null);
  const [invoiceModalVisible, setInvoiceModalVisible] = useState(false);
  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null);

  // Doğrudan sipariş/satış üzerinden geri çağırma modalı
  const [recallModalOpen, setRecallModalOpen] = useState(false);
  const [targetRecallLot, setTargetRecallLot] = useState<{
    lot_id: number;
    lot_number: string;
    product_name?: string;
    market_name?: string;
  } | null>(null);
  const [recallReason, setRecallReason] = useState('');
  const [recalling, setRecalling] = useState(false);

  function openInlineRecallModal(lotId: number, lotNumber: string, productName?: string, marketName?: string) {
    setTargetRecallLot({ lot_id: lotId, lot_number: lotNumber, product_name: productName, market_name: marketName });
    setRecallReason('Mikrobiyolojik kontaminasyon riski ve kalite standart dışılığı şüphesi');
    setRecallModalOpen(true);
  }

  async function submitInlineRecall() {
    if (!targetRecallLot || !recallReason.trim()) return;
    setRecalling(true);
    try {
      const { data } = await recallService.initiate({
        lot_id: targetRecallLot.lot_id,
        reason: recallReason.trim(),
      });
      message.success(`⛔ Geri çağırma başlatıldı! ${data.affectedMarkets || 1} market bilgilendirildi ve parti bloke edildi.`);
      setRecallModalOpen(false);
      setRecallReason('');
      setTargetRecallLot(null);
      await loadData();
    } catch (err: any) {
      const errMsg = err?.response?.data?.error || 'Geri çağırma başlatılamadı';
      message.error(errMsg);
    } finally {
      setRecalling(false);
    }
  }

  async function openInvoice(purchaseId: number) {
    try {
      const { data } = await invoiceService.getAll({ reference_type: 'market_purchase', reference_id: purchaseId });
      if (data && data.length > 0) {
        setSelectedInvoice(data[0]);
        setInvoiceModalVisible(true);
      } else {
        message.warning(t('sales.invoiceNotFound'));
      }
    } catch {
      message.error(t('sales.invoiceLoadError'));
    }
  }

  async function loadData() {
    setLoading(true);
    try {
      const [salesRes, complaintsRes] = await Promise.all([
        purchaseService.getAll(),
        purchaseService.getComplaints().catch(() => ({ data: [] })),
      ]);
      setSales(salesRes.data || []);
      setComplaints(complaintsRes.data || []);
    } catch {
      message.error(t('sales.salesLoadError'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  // İstatistik hesaplamaları
  const totalRevenue = useMemo(
    () => sales.reduce((sum, s) => sum + convertToEur(Number(s.total_price || 0), s.currency), 0),
    [sales]
  );

  const problemCount = useMemo(
    () => sales.filter(s => Number(s.complaint_count || 0) > 0 || s.lot_status === 'recalled').length,
    [sales]
  );

  const columns = [
    {
      title: `${t('listings.productName')} / ${t('common.lotNumber')}`,
      key: 'product',
      render: (_: unknown, r: SaleItem) => (
        <div>
          <Text strong>{formatDbProduct(r.product_name, t) || r.lot_number}</Text>
          <div>
            <Tag color={r.lot_status === 'recalled' ? 'red' : 'blue'} style={{ fontSize: 11 }}>
              {r.lot_number}
            </Tag>
            {r.lot_status === 'recalled' && (
              <Tag color="red" style={{ fontSize: 11 }}>{t('common.recalled')}</Tag>
            )}
          </div>
        </div>
      ),
    },
    {
      title: t('sales.buyerMarket'),
      key: 'market',
      render: (_: unknown, r: SaleItem) => (
        <div>
          <Text strong style={{ color: '#1890ff' }}>
            <ShopOutlined style={{ marginRight: 4 }} />
            {r.market_name}
          </Text>
          {r.market_phone && (
            <div style={{ fontSize: 11, color: '#8c8c8c' }}>
              <PhoneOutlined style={{ marginRight: 4 }} />{r.market_phone}
            </div>
          )}
        </div>
      ),
    },
    {
      title: t('common.quantity'),
      key: 'quantity',
      render: (_: unknown, r: SaleItem) => `${Number(r.quantity).toFixed(0)} paket`,
    },
    {
      title: t('common.unitPrice'),
      dataIndex: 'unit_price',
      key: 'unit_price',
      render: (v: number, r: SaleItem) => `${formatMoney(v, r.currency)} / paket`,
    },
    {
      title: t('common.totalPrice'),
      dataIndex: 'total_price',
      key: 'total_price',
      render: (v: number, r: SaleItem) => (
        <Tag color="green" style={{ fontSize: 13, fontWeight: 600 }}>
          {formatMoney(v, r.currency, true)}
        </Tag>
      ),
    },
    {
      title: t('common.status'),
      key: 'status',
      render: (_: unknown, r: SaleItem) => {
        const hasIssue = Number(r.complaint_count || 0) > 0;
        if (r.lot_status === 'recalled') {
          return <Tag color="error">⚠️ {t('common.recalled')}</Tag>;
        }
        if (hasIssue) {
          return (
            <Badge status="error" text={<Tag color="error">{t('market.reportComplaint')} ({r.complaint_count})</Tag>} />
          );
        }
        return <Tag color="success"><CheckCircleOutlined /> {t('common.active')}</Tag>;
      },
    },
    {
      title: t('sales.saleDate'),
      dataIndex: 'purchase_date',
      key: 'purchase_date',
      render: (v: string) => dayjs(v).format('DD.MM.YYYY HH:mm'),
    },
    {
      title: t('common.actions'),
      key: 'actions',
      render: (_: unknown, r: SaleItem) => (
        <Space size="small">
          <Button
            size="small"
            icon={<FileTextOutlined />}
            onClick={() => openInvoice(r.id)}
          >
            {t('invoices.title')}
          </Button>
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => { setSelectedSale(r); setDetailOpen(true); }}
          >
            {t('common.details')}
          </Button>
          <Button
            size="small"
            type="link"
            style={{ padding: 0 }}
            onClick={() => navigate(`/lots/${r.lot_id}/trace`)}
          >
            {t('nav.lotTrace')}
          </Button>
          {r.lot_status === 'recalled' ? (
            <Tag color="red">{t('common.recalled')}</Tag>
          ) : (
            <Button
              size="small"
              danger
              icon={<WarningOutlined />}
              onClick={() => openInlineRecallModal(r.lot_id, r.lot_number, r.product_name, r.market_name)}
            >
              {t('recalls.startRecall')}
            </Button>
          )}
        </Space>
      ),
    },
  ];

  const complaintColumns = [
    {
      title: t('common.lotNumber'),
      key: 'lot',
      render: (_: unknown, c: ComplaintItem) => (
        <div>
          <Tag color="red" style={{ fontSize: 12, fontWeight: 600 }}>{c.lot_number}</Tag>
          <div style={{ marginTop: 2 }}>
            <Tag color={c.lot_status === 'recalled' ? 'red' : 'default'} style={{ fontSize: 10 }}>
              {c.lot_status === 'recalled' ? t('common.recalled') : t('common.active')}
            </Tag>
          </div>
        </div>
      ),
    },
    {
      title: t('common.description'),
      key: 'message',
      render: (_: unknown, c: ComplaintItem) => (
        <div>
          <Text strong style={{ color: '#cf1322', display: 'block' }}>
            {c.title}
          </Text>
          <Text style={{ fontSize: 13, color: '#434343' }}>
            {c.message}
          </Text>
        </div>
      ),
    },
    {
      title: t('common.date'),
      dataIndex: 'created_at',
      key: 'created_at',
      render: (v: string) => dayjs(v).format('DD.MM.YYYY HH:mm'),
    },
    {
      title: t('common.actions'),
      key: 'action',
      render: (_: unknown, c: ComplaintItem) => (
        <Space>
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => navigate(`/lots/${c.lot_id}/trace`)}
          >
            {t('nav.lotTrace')}
          </Button>
          {c.lot_status === 'recalled' ? (
            <Tag color="red">{t('common.recalled')}</Tag>
          ) : (
            <Button
              size="small"
              danger
              icon={<WarningOutlined />}
              onClick={() => openInlineRecallModal(c.lot_id, c.lot_number, 'Mamul Ürün', 'Market')}
            >
              {t('recalls.startRecall')}
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* Başlık */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <Title level={3} style={{ margin: 0 }}>
            <DollarCircleOutlined /> {t('sales.title')}
          </Title>
          <Text type="secondary" style={{ fontSize: 13 }}>
            {t('sales.subtitle')}
          </Text>
        </div>
        <Button onClick={loadData}>{t('common.refresh')}</Button>
      </div>

      {/* Problem Varsa Uyarı Kartı */}
      {problemCount > 0 && (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
          message={t('sales.complaintAlert')}
          description={
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 4 }}>
              <span>{t('sales.complaintActionDesc')}</span>
              <Button size="small" danger onClick={() => navigate('/recalls')}>
                {t('recalls.title')}
              </Button>
            </div>
          }
        />
      )}

      {/* İstatistik Kartları */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title={t('sales.totalSales')}
              value={sales.length}
              prefix={<ShopOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title={t('sales.totalRevenue')}
              value={totalRevenue.toFixed(2)}
              suffix="€"
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title={t('sales.distinctMarkets')}
              value={new Set(sales.map(s => s.market_id)).size}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title={t('sales.problemLots')}
              value={problemCount}
              valueStyle={{ color: problemCount > 0 ? '#cf1322' : '#52c41a' }}
              prefix={<WarningOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* Sekmeler */}
      <Tabs
        defaultActiveKey="sales"
        items={[
          {
            key: 'sales',
            label: `🛒 ${t('nav.sales')} (${sales.length})`,
            children: (
              sales.length === 0 && !loading ? (
                <Empty description={t('common.noData')} />
              ) : (
                <Table
                  dataSource={sales}
                  columns={columns}
                  rowKey="id"
                  loading={loading}
                  pagination={{ pageSize: 15 }}
                />
              )
            ),
          },
          {
            key: 'complaints',
            label: (
              <span>
                ⚠️ {t('market.reportComplaint')}{' '}
                {complaints.length > 0 && <Tag color="red">{complaints.length}</Tag>}
              </span>
            ),
            children: (
              complaints.length === 0 && !loading ? (
                <Empty
                  description={t('common.noData')}
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                />
              ) : (
                <Table
                  dataSource={complaints}
                  columns={complaintColumns}
                  rowKey="id"
                  loading={loading}
                  pagination={{ pageSize: 10 }}
                />
              )
            ),
          },
        ]}
      />

      {/* Detay Modalı */}
      <Modal
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={
          <Space>
            {selectedSale && (
              <Button
                type="primary"
                onClick={() => {
                  setDetailOpen(false);
                  navigate(`/lots/${selectedSale.lot_id}/trace`);
                }}
              >
                {t('nav.lotTrace')}
              </Button>
            )}
            <Button onClick={() => setDetailOpen(false)}>{t('common.close')}</Button>
          </Space>
        }
        width={560}
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <DollarCircleOutlined style={{ color: '#52c41a', fontSize: 20 }} />
            <span>{t('sales.detailTitle')} — #{selectedSale?.id}</span>
          </div>
        }
      >
        {selectedSale && (
          <div>
            {/* Şikayet uyarısı varsa */}
            {Number(selectedSale.complaint_count || 0) > 0 && (
              <Alert
                type="warning"
                showIcon
                style={{ marginBottom: 16 }}
                message={t('market.reportComplaint')}
                description={selectedSale.latest_complaint}
              />
            )}

            <Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
              <Descriptions.Item label={t('sales.buyerMarket')} span={2}>
                <Text strong>{selectedSale.market_name}</Text>
              </Descriptions.Item>
              {selectedSale.market_phone && (
                <Descriptions.Item label={t('common.phone')} span={2}>
                  <PhoneOutlined style={{ marginRight: 6 }} />{selectedSale.market_phone}
                </Descriptions.Item>
              )}
              {selectedSale.market_address && (
                <Descriptions.Item label={t('common.address')} span={2}>
                  <EnvironmentOutlined style={{ marginRight: 6 }} />{selectedSale.market_address}
                </Descriptions.Item>
              )}
              <Descriptions.Item label={t('listings.productName')}>
                {selectedSale.product_name || selectedSale.lot_number}
              </Descriptions.Item>
              <Descriptions.Item label={t('common.lotNumber')}>
                <Tag color="blue">{selectedSale.lot_number}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label={t('common.quantity')}>
                <Text strong>{Number(selectedSale.quantity).toFixed(0)} paket</Text>
              </Descriptions.Item>
              <Descriptions.Item label={t('common.unitPrice')}>
                {formatMoney(selectedSale.unit_price, selectedSale.currency)} / paket
              </Descriptions.Item>
              <Descriptions.Item label={t('common.totalPrice')} span={2}>
                <Text strong style={{ color: '#52c41a', fontSize: 16 }}>
                  {formatMoney(selectedSale.total_price, selectedSale.currency, true)}
                </Text>
              </Descriptions.Item>
              <Descriptions.Item label={t('sales.saleDate')} span={2}>
                {dayjs(selectedSale.purchase_date).format('DD.MM.YYYY HH:mm:ss')}
              </Descriptions.Item>
            </Descriptions>
          </div>
        )}
      </Modal>

      {/* Doğrudan Satış Üzerinden Geri Çağırma (Inline Modal) */}
      <Modal
        title={<span><WarningOutlined style={{ color: '#cf1322', marginRight: 8 }} />{t('recalls.startRecall')}</span>}
        open={recallModalOpen}
        onCancel={() => { setRecallModalOpen(false); setTargetRecallLot(null); }}
        footer={null}
      >
        {targetRecallLot && (
          <div>
            <Alert
              type="error"
              showIcon
              style={{ marginBottom: 16 }}
              message="Dikkat: Mamul Geri Çağırma (Recall)"
              description={`Lot ${targetRecallLot.lot_number} için geri çağırma başlatıldığında bu parti sistem tarafından bloke edilecek ve satın alan marketlere derhal acil bildirim iletilecektir.`}
            />
            <Descriptions bordered size="small" column={1} style={{ marginBottom: 16 }}>
              <Descriptions.Item label={t('common.lotNumber')}>
                <Tag color="red" style={{ fontWeight: 600 }}>{targetRecallLot.lot_number}</Tag>
              </Descriptions.Item>
              {targetRecallLot.product_name && (
                <Descriptions.Item label={t('listings.productName')}>
                  {targetRecallLot.product_name}
                </Descriptions.Item>
              )}
              {targetRecallLot.market_name && (
                <Descriptions.Item label={t('sales.buyerMarket')}>
                  {targetRecallLot.market_name}
                </Descriptions.Item>
              )}
            </Descriptions>
            <div style={{ marginBottom: 16 }}>
              <Text strong style={{ display: 'block', marginBottom: 6 }}>{t('recalls.recallReason')} *</Text>
              <Input.TextArea
                rows={3}
                value={recallReason}
                onChange={(e: any) => setRecallReason(e.target.value)}
                placeholder="Geri çağırma gerekçesini giriniz..."
              />
            </div>
            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
              <Button onClick={() => { setRecallModalOpen(false); setTargetRecallLot(null); }}>
                {t('common.cancel')}
              </Button>
              <Button
                type="primary"
                danger
                loading={recalling}
                disabled={!recallReason.trim()}
                onClick={submitInlineRecall}
              >
                {t('recalls.startRecall')}
              </Button>
            </div>
          </div>
        )}
      </Modal>

      <InvoiceModal
        visible={invoiceModalVisible}
        invoice={selectedInvoice}
        onClose={() => setInvoiceModalVisible(false)}
      />
    </div>
  );
}
