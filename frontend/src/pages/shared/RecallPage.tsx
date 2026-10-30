import { useEffect, useState } from 'react';
import { Table, Tag, Button, Modal, Form, Input, Select, Typography, message, Card, Descriptions, Alert } from 'antd';
import { WarningOutlined, EyeOutlined } from '@ant-design/icons';
import { recallService } from '../../services/api';
import { Recall } from '../../types';
import { useTranslation } from '../../i18n';
import { formatUnit, formatDbStatus } from '../../i18n/formatters';

const { Title, Text } = Typography;

export default function RecallPage() {
  const { t } = useTranslation();
  const [recalls, setRecalls]         = useState<Recall[]>([]);
  const [lots, setLots]               = useState<any[]>([]);
  const [loading, setLoading]         = useState(true);
  const [modalOpen, setModalOpen]     = useState(false);
  const [detailOpen, setDetailOpen]   = useState(false);
  const [selectedRecall, setSelected] = useState<Record<string, unknown> | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    Promise.all([recallService.getAll(), recallService.getRecallableLots()])
      .then(([r, l]) => { setRecalls(r.data); setLots(l.data || []); })
      .catch(() => message.error('Veriler yüklenemedi'))
      .finally(() => setLoading(false));
  }, []);

  async function onSubmit(values: { lot_id: number; reason: string }) {
    try {
      const { data } = await recallService.initiate(values);
      message.success(`Geri çağırma başlatıldı. ${data.affectedMarkets} market etkilendi.`);
      setModalOpen(false);
      form.resetFields();
      const [{ data: updatedRecalls }, { data: updatedLots }] = await Promise.all([
        recallService.getAll(),
        recallService.getRecallableLots(),
      ]);
      setRecalls(updatedRecalls);
      setLots(updatedLots || []);
    } catch (err: any) {
      const errMsg = err?.response?.data?.error || 'Geri çağırma başlatılamadı';
      message.error(errMsg);
    }
  }

  async function viewDetail(id: number) {
    try {
      const { data } = await recallService.getById(id);
      setSelected(data);
      setDetailOpen(true);
    } catch {
      message.error('Detay yüklenemedi');
    }
  }

  const columns = [
    { title: t('common.lotNumber'), dataIndex: 'lot_number', key: 'lot_number', render: (v: string) => <strong>{v}</strong> },
    { title: t('recalls.reasonLabel'), dataIndex: 'reason', key: 'reason' },
    { title: t('recalls.affectedMarkets'), dataIndex: 'affected_count', key: 'affected_count', render: (v: number) => <Tag color="red">{v} {t('roles.market')}</Tag> },
    { title: t('common.status'), dataIndex: 'status', key: 'status', render: (v: string) => <Tag color={v === 'aktif' ? 'red' : 'green'}>{formatDbStatus(v, t)}</Tag> },
    { title: t('common.date'), dataIndex: 'created_at', key: 'created_at', render: (v: string) => new Date(v).toLocaleDateString() },
    { title: t('common.actions'), key: 'action', render: (_: unknown, r: Recall) => <Button size="small" icon={<EyeOutlined />} onClick={() => viewDetail(r.id)}>{t('common.details')}</Button> },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={3}><WarningOutlined /> {t('recalls.title')}</Title>
        <Button type="primary" danger onClick={() => setModalOpen(true)}>{t('recalls.startRecall')}</Button>
      </div>

      <Table dataSource={recalls} columns={columns} rowKey="id" loading={loading} />

      <Modal title={t('recalls.startRecall')} open={modalOpen} onCancel={() => setModalOpen(false)} footer={null}>
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message="Geri Çağırma Kuralı"
          description="Yalnızca marketlere satışı yapılmış mamul ürün lotları (sucuk, salam, sosis vb.) geri çağrılabilir."
        />
        <Form form={form} onFinish={onSubmit} layout="vertical">
          <Form.Item name="lot_id" label={t('common.lotNumber')} rules={[{ required: true, message: 'Lütfen bir mamul lotu seçin' }]}>
            <Select showSearch optionFilterProp="children" placeholder="Markete satılmış mamul lotu seçin">
              {lots.map((l: any) => (
                <Select.Option key={l.id} value={l.id}>
                  {l.lot_number} — {l.product_name || 'Mamul'} ({l.quantity} {formatUnit(l.unit || 'paket', t)})
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          {lots.length === 0 && (
            <Text type="secondary" style={{ display: 'block', marginBottom: 12, fontSize: 12 }}>
              Geri çağrılabilecek markete satılmış mamul lot bulunamadı.
            </Text>
          )}
          <Form.Item name="reason" label={t('recalls.recallReason')} rules={[{ required: true, min: 10 }]}>
            <Input.TextArea rows={3} placeholder="Mikrobiyolojik uygunsuzluk veya bozulma şüphesi..." />
          </Form.Item>
          <Button type="primary" danger htmlType="submit" block disabled={lots.length === 0}>{t('recalls.startRecall')}</Button>
        </Form>
      </Modal>

      <Modal
        title={t('recalls.detailTitle')}
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={null}
        width={700}
      >
        {selectedRecall && (
          <div>
            <Descriptions bordered size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label={t('common.lotNumber')}>{(selectedRecall.recall as Record<string, unknown>)?.lot_number as string}</Descriptions.Item>
              <Descriptions.Item label={t('recalls.reasonLabel')}>{(selectedRecall.recall as Record<string, unknown>)?.reason as string}</Descriptions.Item>
            </Descriptions>
            <Card title={t('recalls.affectedMarkets')} size="small">
              <Table
                dataSource={selectedRecall.affectedMarkets as Record<string, unknown>[]}
                rowKey="id"
                size="small"
                columns={[
                  { title: t('roles.market'), dataIndex: 'market_name', key: 'market_name' },
                  { title: t('common.quantity'), key: 'qty', render: (_: unknown, r: Record<string, unknown>) => `${r.quantity} ${r.unit}` },
                  { title: t('common.phone'), dataIndex: 'market_phone', key: 'market_phone' },
                  { title: t('common.status'), dataIndex: 'status', key: 'status', render: (v: string) => <Tag>{v}</Tag> },
                ]}
              />
            </Card>
          </div>
        )}
      </Modal>
    </div>
  );
}
