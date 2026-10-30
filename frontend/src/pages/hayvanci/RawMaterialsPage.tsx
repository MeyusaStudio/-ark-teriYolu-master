import { useEffect, useState } from 'react';
import {
  Table, Button, Modal, Form, Select, InputNumber, DatePicker,
  Typography, Tag, message, Card, Statistic, Row, Col,
  Input, Popconfirm, Space,
} from 'antd';
import {
  PlusOutlined, DeleteOutlined, InboxOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { rawMaterialService } from '../../services/api';
import { formatMoney, convertToEur, CurrencySelect } from '../../utils/currency';
import { useTranslation } from '../../i18n';
import { formatUnit, formatDbStatus, formatDbPart } from '../../i18n/formatters';

const { Title, Text } = Typography;

// Dana ve Kuzu karkas seçenekleri tamamen kaldırıldı.
const MATERIALS = [
  { label: 'Tuz',                  unit: 'kg',  category: 'Kuru/Toz' },
  { label: 'Baharat',              unit: 'kg',  category: 'Kuru/Toz' },
  { label: 'Çemen',                unit: 'kg',  category: 'Kuru/Toz' },
  { label: 'Sarımsak',             unit: 'kg',  category: 'Kuru/Toz' },
  { label: 'Jelatin',              unit: 'kg',  category: 'Kuru/Toz' },
  { label: 'Yağ',                  unit: 'kg',  category: 'Sıvı' },
  { label: 'İlikli Kemik Suyu',    unit: 'kg',  category: 'Sıvı' },
  { label: 'Kırmızı Pancar Suyu',  unit: 'kg',  category: 'Sıvı' },
  { label: 'Salça',                unit: 'kg',  category: 'Sebze/Meyve' },
  { label: 'Yoğurt',               unit: 'kg',  category: 'Süt/Yumurta' },
  { label: 'Yumurta Akı',          unit: 'kg',  category: 'Süt/Yumurta' },
  { label: 'İnek Bağırsağı',       unit: 'kg',  category: 'Bağırsak' },
  { label: 'Koyun Bağırsağı',      unit: 'kg',  category: 'Bağırsak' },
  { label: 'Bağırsak',             unit: 'kg',  category: 'Bağırsak' },
];

const CATEGORY_COLORS: Record<string, string> = {
  'Kuru/Toz':     'orange',
  'Sıvı':         'blue',
  'Sebze/Meyve':  'green',
  'Süt/Yumurta':  'purple',
  'Bağırsak':     'cyan',
};

interface RawMaterial {
  id: number;
  material_type: string;
  quantity: number;
  unit: string;
  unit_price: number;
  total_cost: number;
  currency?: string;
  lot_number: string;
  lot_status: string;
  expiry_date?: string | null;
  created_at: string;
}

export default function RawMaterialsPage() {
  const { t } = useTranslation();
  const [materials, setMaterials] = useState<RawMaterial[]>([]);
  const [loading, setLoading]     = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();

  async function load() {
    try {
      const { data } = await rawMaterialService.getAll();
      setMaterials(data);
    } catch {
      message.error('Hammaddeler yüklenemedi');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  async function onSubmit(values: {
    material_type: string;
    quantity: number;
    unit_price: number;
    currency?: string;
    supplier_name?: string;
    expiry_date?: dayjs.Dayjs;
  }) {
    setSubmitting(true);
    try {
      await rawMaterialService.create({
        material_type: values.material_type,
        quantity: values.quantity,
        unit: 'kg', // Birim seçimi kaldırıldı, varsayılan kg
        unit_price: values.unit_price,
        currency: values.currency || 'EUR',
        supplier_name: values.supplier_name,
        expiry_date: values.expiry_date ? values.expiry_date.format('YYYY-MM-DD') : undefined,
      });
      message.success('Hammadde stoka eklendi');
      setModalOpen(false);
      form.resetFields();
      load();
    } catch {
      message.error('Hammadde eklenemedi');
    } finally {
      setSubmitting(false);
    }
  }

  async function onDelete(id: number) {
    try {
      await rawMaterialService.remove(id);
      message.success('Hammadde silindi');
      load();
    } catch {
      message.error('Silinemedi');
    }
  }

  // İstatistikler
  const activeCount = materials.filter(m => m.lot_status === 'active').length;
  const totalValue  = materials
    .filter(m => m.lot_status === 'active')
    .reduce((s, m) => s + convertToEur(Number(m.total_cost), m.currency), 0);

  const columns = [
    {
      title: t('rawMaterials.materialType'), dataIndex: 'material_type', key: 'material_type',
      render: (v: string) => {
        const found = MATERIALS.find(m => m.label === v);
        const color = found ? CATEGORY_COLORS[found.category] : 'default';
        return <Tag color={color}>{formatDbPart(v, t)}</Tag>;
      },
    },
    {
      title: t('common.quantity'), key: 'qty',
      render: (_: unknown, r: RawMaterial) =>
        <Text strong>{Number(r.quantity).toFixed(2)} {formatUnit(r.unit, t)}</Text>,
    },
    {
      title: t('common.unitPrice'), dataIndex: 'unit_price', key: 'unit_price',
      render: (v: number, r: RawMaterial) => formatMoney(v, r.currency),
    },
    {
      title: t('common.totalCost'), dataIndex: 'total_cost', key: 'total_cost',
      render: (v: number, r: RawMaterial) => <Text strong>{formatMoney(v, r.currency, true)}</Text>,
    },
    {
      title: t('common.lotNumber'), dataIndex: 'lot_number', key: 'lot_number',
      render: (v: string) => <Tag color="blue">{v}</Tag>,
    },
    {
      title: 'SKT',
      dataIndex: 'expiry_date',
      key: 'expiry_date',
      render: (v: string | null | undefined) => v ? <Tag color="geekblue">{dayjs(v).format('DD.MM.YYYY')}</Tag> : <Text type="secondary">-</Text>,
    },
    {
      title: t('common.status'), dataIndex: 'lot_status', key: 'lot_status',
      render: (v: string) => (
        <Tag color={v === 'active' ? 'green' : 'default'}>
          {formatDbStatus(v, t)}
        </Tag>
      ),
    },
    {
      title: t('common.date'), dataIndex: 'created_at', key: 'created_at',
      render: (v: string) => dayjs(v).format('DD.MM.YYYY'),
    },
    {
      title: '', key: 'action',
      render: (_: unknown, r: RawMaterial) => (
        <Popconfirm
          title={t('rawMaterials.deleteConfirm')}
          onConfirm={() => onDelete(r.id)}
          okText={t('common.yes')} cancelText={t('common.no')}
        >
          <Button size="small" danger icon={<DeleteOutlined />} />
        </Popconfirm>
      ),
    },
  ];

  return (
    <div>
      {/* Başlık */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={3} style={{ margin: 0 }}>
          <InboxOutlined /> {t('rawMaterials.title')}
        </Title>
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
            {t('rawMaterials.addModalTitle')}
          </Button>
        </Space>
      </div>

      {/* İstatistik kartları */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Card size="small">
            <Statistic title={t('rawMaterials.activeStockItem')} value={activeCount} />
          </Card>
        </Col>
        <Col span={8}>
          <Card size="small">
            <Statistic title={t('rawMaterials.totalStockValue')} value={totalValue.toFixed(2)} suffix="€" />
          </Card>
        </Col>
        <Col span={8}>
          <Card size="small">
            <Statistic title={t('rawMaterials.totalRecords')} value={materials.length} />
          </Card>
        </Col>
      </Row>

      <Table
        dataSource={materials}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 15 }}
        rowClassName={(r) => r.lot_status !== 'active' ? 'ant-table-row-disabled' : ''}
      />

      {/* Ekleme Modalı */}
      <Modal
        title={t('rawMaterials.addModalTitle')}
        open={modalOpen}
        onCancel={() => { setModalOpen(false); form.resetFields(); }}
        footer={null}
        width={500}
      >
        <Form
          form={form}
          onFinish={onSubmit}
          layout="vertical"
          size="large"
          initialValues={{ currency: 'EUR' }}
        >
          <Form.Item
            name="material_type"
            label={t('rawMaterials.materialType')}
            rules={[{ required: true, message: t('rawMaterials.selectMaterial') }]}
          >
            <Select
              showSearch
              placeholder={t('rawMaterials.selectOrSearch')}
            >
              {Object.entries(
                MATERIALS.reduce<Record<string, typeof MATERIALS>>((acc, m) => {
                  if (!acc[m.category]) acc[m.category] = [];
                  acc[m.category].push(m);
                  return acc;
                }, {})
              ).map(([cat, items]) => (
                <Select.OptGroup key={cat} label={cat}>
                  {items.map(m => (
                    <Select.Option key={m.label} value={m.label}>
                      <Tag color={CATEGORY_COLORS[cat]} style={{ marginRight: 6 }}>{cat}</Tag>
                      {m.label}
                    </Select.Option>
                  ))}
                </Select.OptGroup>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="quantity"
            label={t('common.quantity')}
            rules={[{ required: true, message: t('rawMaterials.enterQuantity') }]}
          >
            <InputNumber
              min={0.001} step={0.1} precision={3}
              style={{ width: '100%' }}
              addonAfter="kg"
              placeholder="0.00"
            />
          </Form.Item>

          <Row gutter={12}>
            <Col span={14}>
              <Form.Item
                name="unit_price"
                label={t('common.unitPrice')}
                rules={[{ required: true, message: t('rawMaterials.enterUnitPrice') }]}
              >
                <InputNumber
                  min={0.0001} step={0.5} precision={4}
                  style={{ width: '100%' }}
                  placeholder="0.00"
                />
              </Form.Item>
            </Col>
            <Col span={10}>
              <Form.Item name="currency" label={t('common.currency')}>
                <CurrencySelect style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item name="supplier_name" label={t('common.supplier')}>
            <Input placeholder={t('rawMaterials.supplierPlaceholder')} />
          </Form.Item>

          <Form.Item name="expiry_date" label="Son Kullanma Tarihi (SKT - İsteğe Bağlı)">
            <DatePicker
              style={{ width: '100%' }}
              format="YYYY-MM-DD"
              placeholder="SKT seçiniz (Opsiyonel)"
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0 }}>
            <Button type="primary" htmlType="submit" loading={submitting} block style={{ borderRadius: 8 }}>
              {t('rawMaterials.saveButton')}
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
