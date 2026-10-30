import { useEffect, useState, useCallback } from 'react';
import {
  Row, Col, Card, Statistic, Table, Tag, Button, Input, Select, Space,
  Modal, Form, Typography, message, Popconfirm, Tooltip
} from 'antd';
import {
  UsergroupAddOutlined, StopOutlined, CheckCircleOutlined,
  SearchOutlined, ReloadOutlined, MailOutlined,
  NotificationOutlined, SafetyCertificateOutlined,
  ShopOutlined, ShoppingCartOutlined, AppstoreOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { adminService } from '../../services/api';
import { AdminUserItem, AdminStats } from '../../types';

const { Title, Text } = Typography;
const { TextArea } = Input;

const roleColors: Record<string, string> = {
  hayvanci: 'green',
  sucukcu: 'blue',
  market: 'orange',
  admin: 'purple',
};

const roleLabels: Record<string, string> = {
  hayvanci: 'İşletme (Hayvancı)',
  sucukcu: 'Üretici (Sucukçu)',
  market: 'Market / Alıcı',
  admin: 'Sistem Yöneticisi',
};

const quickBanReasons = [
  'Kural ihlali',
  'Geri çağırma kurallarına uymama',
  'Şüpheli ürün ve parti sevkiyatı',
  'E-Fatura ve evrak usulsüzlüğü',
  'Hesap güvenliği ve yetkisiz işlem',
];

const singleMessageTemplates = [
  { title: 'Geri Çağırma Denetimi', message: 'Lütfen son sevkiyat ve depo lotlarınızı acilen denetleyin.' },
  { title: 'Fatura Hatırlatması', message: 'Sistemde bekleyen faturalandırılmamış işlemleriniz mevcuttur. Lütfen tamamlayınız.' },
  { title: 'Kural Hatırlatması', message: 'Platform ticaret ve hijyen standartlarına uygun hareket etmeniz rica olunur.' },
];

const broadcastTemplates = [
  { title: '📢 Sistem Bakım Çalışması', message: 'Platformumuzda bu gece 02:00 - 04:00 saatleri arasında sunucu bakım çalışması gerçekleştirilecektir.' },
  { title: '📢 Mevzuat Güncellemesi', message: 'Yeni gıda güvenliği ve geri çağırma regülasyonlarına uygun olarak parti kodları zorunlu hale getirilmiştir.' },
  { title: '📢 Önemli Hatırlatma', message: 'Tüm tarafların e-arşiv ve e-fatura bilgilerini eksiksiz düzenlemesi rica olunur.' },
];

export default function AdminUsersPage() {
  const [users, setUsers] = useState<AdminUserItem[]>([]);
  const [stats, setStats] = useState<AdminStats>({ total: 0, hayvanci: 0, sucukcu: 0, market: 0, admin: 0, banned: 0 });
  const [loading, setLoading] = useState(false);

  // Filters
  const [roleFilter, setRoleFilter] = useState<string>('all');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Modals
  const [banModalVisible, setBanModalVisible] = useState(false);
  const [selectedUserForBan, setSelectedUserForBan] = useState<AdminUserItem | null>(null);
  const [banForm] = Form.useForm();

  const [messageModalVisible, setMessageModalVisible] = useState(false);
  const [selectedUserForMsg, setSelectedUserForMsg] = useState<AdminUserItem | null>(null);
  const [messageForm] = Form.useForm();

  const [broadcastModalVisible, setBroadcastModalVisible] = useState(false);
  const [broadcastForm] = Form.useForm();

  const [actionLoading, setActionLoading] = useState(false);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string> = {};
      if (roleFilter !== 'all') params.role = roleFilter;
      if (statusFilter !== 'all') params.status = statusFilter;
      if (searchQuery.trim()) params.q = searchQuery.trim();

      const { data } = await adminService.getUsers(params);
      setUsers(data.users || []);
      if (data.stats) {
        setStats(data.stats);
      }
    } catch (err: unknown) {
      const errorMsg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error || 'Kullanıcılar alınamadı';
      message.error(errorMsg);
    } finally {
      setLoading(false);
    }
  }, [roleFilter, statusFilter, searchQuery]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  // Ban
  async function handleBanUser(values: { ban_reason: string }) {
    if (!selectedUserForBan) return;
    setActionLoading(true);
    try {
      await adminService.banUser(selectedUserForBan.id, {
        is_banned: true,
        ban_reason: values.ban_reason?.trim(),
      });
      message.success(`${selectedUserForBan.name} başarıyla banlandı.`);
      setBanModalVisible(false);
      banForm.resetFields();
      setSelectedUserForBan(null);
      fetchUsers();
    } catch (err: unknown) {
      const errorMsg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error || 'Kullanıcı banlanamadı';
      message.error(errorMsg);
    } finally {
      setActionLoading(false);
    }
  }

  // Unban
  async function handleUnbanUser(user: AdminUserItem) {
    try {
      await adminService.banUser(user.id, { is_banned: false });
      message.success(`${user.name} kullanıcısının banı kaldırıldı.`);
      fetchUsers();
    } catch (err: unknown) {
      const errorMsg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error || 'Ban kaldırılamadı';
      message.error(errorMsg);
    }
  }

  // Bireysel Mesaj
  async function handleSendMessage(values: { title: string; message: string }) {
    if (!selectedUserForMsg) return;
    setActionLoading(true);
    try {
      await adminService.sendNotification({
        user_id: selectedUserForMsg.id,
        title: values.title.trim(),
        message: values.message.trim(),
      });
      message.success(`Bildirim ${selectedUserForMsg.name} kullanıcısına başarıyla iletildi.`);
      setMessageModalVisible(false);
      messageForm.resetFields();
      setSelectedUserForMsg(null);
      fetchUsers();
    } catch (err: unknown) {
      const errorMsg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error || 'Mesaj iletilemedi';
      message.error(errorMsg);
    } finally {
      setActionLoading(false);
    }
  }

  // Toplu Duyuru
  async function handleSendBroadcast(values: { role: string; title: string; message: string }) {
    setActionLoading(true);
    try {
      const res = await adminService.sendNotification({
        role: values.role === 'all' ? undefined : values.role,
        title: values.title.trim(),
        message: values.message.trim(),
      });
      message.success(res.data?.message || 'Toplu bildirim başarıyla gönderildi.');
      setBroadcastModalVisible(false);
      broadcastForm.resetFields();
      fetchUsers();
    } catch (err: unknown) {
      const errorMsg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error || 'Duyuru iletilemedi';
      message.error(errorMsg);
    } finally {
      setActionLoading(false);
    }
  }

  const columns = [
    {
      title: 'Kullanıcı',
      key: 'user',
      render: (_: unknown, record: AdminUserItem) => (
        <Space direction="vertical" size={2}>
          <Space>
            <Text strong style={{ fontSize: 14 }}>{record.name}</Text>
            {record.role === 'admin' && <Tag color="purple">Yönetici</Tag>}
          </Space>
          <Text type="secondary" style={{ fontSize: 12 }}>{record.email}</Text>
          {record.phone && <Text type="secondary" style={{ fontSize: 11 }}>📞 {record.phone}</Text>}
        </Space>
      ),
    },
    {
      title: 'Rol',
      dataIndex: 'role',
      key: 'role',
      render: (role: string) => (
        <Tag color={roleColors[role] || 'default'} style={{ fontSize: 12, padding: '4px 8px' }}>
          {roleLabels[role] || role}
        </Tag>
      ),
    },
    {
      title: 'Faaliyet Özeti',
      key: 'activity',
      render: (_: unknown, record: AdminUserItem) => (
        <Space wrap>
          {record.lot_count > 0 && <Tag color="blue"><AppstoreOutlined /> {record.lot_count} Lot</Tag>}
          {record.order_count > 0 && <Tag color="cyan"><ShoppingCartOutlined /> {record.order_count} Sipariş</Tag>}
          {record.purchase_count > 0 && <Tag color="orange"><ShopOutlined /> {record.purchase_count} Satın Alım</Tag>}
          {record.lot_count === 0 && record.order_count === 0 && record.purchase_count === 0 && (
            <Text type="secondary" style={{ fontSize: 11 }}>İşlem yok</Text>
          )}
        </Space>
      ),
    },
    {
      title: 'Hesap Durumu',
      key: 'status',
      render: (_: unknown, record: AdminUserItem) => {
        const isBanned = record.is_banned === 1;
        return (
          <Space direction="vertical" size={2}>
            {isBanned ? (
              <Tag color="error" style={{ fontSize: 12, fontWeight: 'bold' }}>
                <StopOutlined /> BANLI
              </Tag>
            ) : (
              <Tag color="success" style={{ fontSize: 12 }}>
                <CheckCircleOutlined /> Aktif
              </Tag>
            )}
            {isBanned && record.ban_reason && (
              <Tooltip title={record.ban_reason}>
                <Text type="danger" style={{ fontSize: 11, maxWidth: 180 }} ellipsis>
                  Gerekçe: {record.ban_reason}
                </Text>
              </Tooltip>
            )}
          </Space>
        );
      },
    },
    {
      title: 'Kayıt Tarihi',
      dataIndex: 'created_at',
      key: 'created_at',
      render: (date: string) => (
        <Text type="secondary" style={{ fontSize: 12 }}>
          {date ? dayjs(date).format('DD.MM.YYYY HH:mm') : '-'}
        </Text>
      ),
    },
    {
      title: 'İşlemler',
      key: 'actions',
      render: (_: unknown, record: AdminUserItem) => {
        const isBanned = record.is_banned === 1;
        const isAdmin = record.role === 'admin';

        return (
          <Space>
            <Button
              size="small"
              icon={<MailOutlined />}
              onClick={() => {
                setSelectedUserForMsg(record);
                messageForm.setFieldsValue({ title: 'Yönetici Bildirimi', message: '' });
                setMessageModalVisible(true);
              }}
            >
              Mesaj
            </Button>

            {!isAdmin && (
              <>
                {isBanned ? (
                  <Popconfirm
                    title="Kullanıcı banını kaldır"
                    description={`${record.name} adlı kullanıcının erişim engelini kaldırmak istediğinize emin misiniz?`}
                    onConfirm={() => handleUnbanUser(record)}
                    okText="Evet, Kaldır"
                    cancelText="Vazgeç"
                  >
                    <Button size="small" type="primary" style={{ backgroundColor: '#52c41a' }}>
                      Banı Kaldır
                    </Button>
                  </Popconfirm>
                ) : (
                  <Button
                    size="small"
                    danger
                    icon={<StopOutlined />}
                    onClick={() => {
                      setSelectedUserForBan(record);
                      banForm.resetFields();
                      setBanModalVisible(true);
                    }}
                  >
                    Banla
                  </Button>
                )}
              </>
            )}
          </Space>
        );
      },
    },
  ];

  return (
    <div style={{ padding: '4px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <div>
          <Title level={3} style={{ margin: 0 }}>
            <SafetyCertificateOutlined style={{ color: '#722ed1', marginRight: 8 }} />
            Kullanıcı Yönetimi & Denetim Paneli
          </Title>
          <Text type="secondary">
            Kullanıcıları filtreleyin, inceleyin, banlayın veya anlık yönetici bildirimi iletin.
          </Text>
        </div>
        <Space>
          <Button
            type="primary"
            icon={<NotificationOutlined />}
            style={{ backgroundColor: '#722ed1', borderColor: '#722ed1' }}
            onClick={() => {
              broadcastForm.setFieldsValue({ role: 'all', title: '📢 Sistem Duyurusu', message: '' });
              setBroadcastModalVisible(true);
            }}
          >
            Toplu Bildirim / Duyuru
          </Button>
          <Button icon={<ReloadOutlined />} onClick={fetchUsers}>
            Yenile
          </Button>
        </Space>
      </div>

      {/* KPI İstatistik Kartları */}
      <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
        <Col xs={24} sm={12} md={4}>
          <Card size="small" style={{ borderRadius: 8 }}>
            <Statistic
              title="Toplam Kullanıcı"
              value={stats.total}
              prefix={<UsergroupAddOutlined style={{ color: '#722ed1' }} />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={5}>
          <Card size="small" style={{ borderRadius: 8 }}>
            <Statistic
              title="🐑 İşletmeler (Hayvancı)"
              value={stats.hayvanci}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={5}>
          <Card size="small" style={{ borderRadius: 8 }}>
            <Statistic
              title="🥩 Üreticiler (Sucukçu)"
              value={stats.sucukcu}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={5}>
          <Card size="small" style={{ borderRadius: 8 }}>
            <Statistic
              title="🛒 Marketler"
              value={stats.market}
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={5}>
          <Card size="small" style={{ borderRadius: 8 }}>
            <Statistic
              title="⛔ Banlı Kullanıcılar"
              value={stats.banned}
              valueStyle={{ color: '#f5222d' }}
              prefix={<StopOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* Arama ve Filtre Çubuğu */}
      <Card size="small" style={{ marginBottom: 16, borderRadius: 8 }}>
        <Row gutter={[12, 12]} align="middle">
          <Col xs={24} sm={10} md={8}>
            <Input
              placeholder="İsim, e-posta veya telefon ile ara..."
              prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              allowClear
            />
          </Col>
          <Col xs={12} sm={7} md={6}>
            <Select
              style={{ width: '100%' }}
              value={roleFilter}
              onChange={setRoleFilter}
              options={[
                { value: 'all', label: 'Tüm Roller' },
                { value: 'hayvanci', label: '🐑 İşletme (Hayvancı)' },
                { value: 'sucukcu', label: '🥩 Üretici (Sucukçu)' },
                { value: 'market', label: '🛒 Market' },
                { value: 'admin', label: '🛡️ Yönetici' },
              ]}
            />
          </Col>
          <Col xs={12} sm={7} md={6}>
            <Select
              style={{ width: '100%' }}
              value={statusFilter}
              onChange={setStatusFilter}
              options={[
                { value: 'all', label: 'Tüm Hesap Durumları' },
                { value: 'active', label: '✓ Aktifler' },
                { value: 'banned', label: '⛔ Banlılar' },
              ]}
            />
          </Col>
          <Col xs={24} sm={24} md={4} style={{ textAlign: 'right' }}>
            <Text type="secondary">{users.length} kullanıcı listeleniyor</Text>
          </Col>
        </Row>
      </Card>

      {/* Kullanıcılar Tablosu */}
      <Card bodyStyle={{ padding: 0 }} style={{ borderRadius: 8, overflow: 'hidden' }}>
        <Table
          dataSource={users}
          columns={columns}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10, showSizeChanger: true }}
        />
      </Card>

      {/* Ban Modalı */}
      <Modal
        title={
          <Space>
            <StopOutlined style={{ color: '#f5222d' }} />
            <span>Kullanıcıyı Banla / Askıya Al</span>
          </Space>
        }
        open={banModalVisible}
        onCancel={() => setBanModalVisible(false)}
        confirmLoading={actionLoading}
        okText="Evet, Kullanıcıyı Banla"
        okButtonProps={{ danger: true }}
        onOk={() => banForm.submit()}
      >
        <div style={{ marginBottom: 12 }}>
          <Text>
            <strong>{selectedUserForBan?.name}</strong> ({selectedUserForBan?.email}) adlı kullanıcının sisteme erişimi tamamen engellenecektir.
          </Text>
        </div>

        <div style={{ marginBottom: 12 }}>
          <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 6 }}>
            Hızlı Gerekçe Seçin:
          </Text>
          <Space wrap size={[4, 6]}>
            {quickBanReasons.map((reason) => (
              <Tag
                key={reason}
                style={{ cursor: 'pointer' }}
                onClick={() => banForm.setFieldsValue({ ban_reason: reason })}
              >
                {reason}
              </Tag>
            ))}
          </Space>
        </div>

        <Form form={banForm} layout="vertical" onFinish={handleBanUser}>
          <Form.Item
            name="ban_reason"
            label="Ban Gerekçesi"
            rules={[{ required: true, message: 'Lütfen kullanıcıya bildirilecek bir gerekçe girin' }]}
          >
            <TextArea rows={3} placeholder="Gerekçe yazınız..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* Bireysel Mesaj Modalı */}
      <Modal
        title={
          <Space>
            <MailOutlined style={{ color: '#1890ff' }} />
            <span>Kullanıcıya Mesaj / Bildirim Gönder</span>
          </Space>
        }
        open={messageModalVisible}
        onCancel={() => setMessageModalVisible(false)}
        confirmLoading={actionLoading}
        okText="Bildirimi Gönder"
        onOk={() => messageForm.submit()}
      >
        <div style={{ marginBottom: 12, padding: '8px 12px', background: '#f0f5ff', borderRadius: 6 }}>
          <Text strong>Alıcı:</Text> {selectedUserForMsg?.name} ({selectedUserForMsg?.email})
        </div>

        <div style={{ marginBottom: 12 }}>
          <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 6 }}>
            Hızlı Şablonlar:
          </Text>
          <Space wrap size={[4, 6]}>
            {singleMessageTemplates.map((t) => (
              <Tag
                key={t.title}
                color="blue"
                style={{ cursor: 'pointer' }}
                onClick={() => messageForm.setFieldsValue({ title: t.title, message: t.message })}
              >
                {t.title}
              </Tag>
            ))}
          </Space>
        </div>

        <Form form={messageForm} layout="vertical" onFinish={handleSendMessage}>
          <Form.Item
            name="title"
            label="Bildirim Başlığı"
            rules={[{ required: true, message: 'Başlık zorunludur' }]}
          >
            <Input placeholder="Başlık girin..." />
          </Form.Item>
          <Form.Item
            name="message"
            label="Mesaj İçeriği"
            rules={[{ required: true, message: 'Mesaj içeriği zorunludur' }]}
          >
            <TextArea rows={4} placeholder="Kullanıcının bildirim kutusuna düşecek mesaj..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* Toplu Duyuru Modalı */}
      <Modal
        title={
          <Space>
            <NotificationOutlined style={{ color: '#722ed1' }} />
            <span>Toplu Bildirim & Duyuru Gönder</span>
          </Space>
        }
        open={broadcastModalVisible}
        onCancel={() => setBroadcastModalVisible(false)}
        confirmLoading={actionLoading}
        okText="Toplu Gönder"
        okButtonProps={{ style: { backgroundColor: '#722ed1', borderColor: '#722ed1' } }}
        onOk={() => broadcastForm.submit()}
      >
        <div style={{ marginBottom: 12 }}>
          <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 6 }}>
            Hızlı Duyuru Şablonları:
          </Text>
          <Space wrap size={[4, 6]}>
            {broadcastTemplates.map((t) => (
              <Tag
                key={t.title}
                color="purple"
                style={{ cursor: 'pointer' }}
                onClick={() => broadcastForm.setFieldsValue({ title: t.title, message: t.message })}
              >
                {t.title}
              </Tag>
            ))}
          </Space>
        </div>

        <Form form={broadcastForm} layout="vertical" onFinish={handleSendBroadcast}>
          <Form.Item
            name="role"
            label="Hedef Kitle"
            rules={[{ required: true }]}
            initialValue="all"
          >
            <Select
              options={[
                { value: 'all', label: '👥 Herkes (Tüm Aktif Kullanıcılar)' },
                { value: 'hayvanci', label: '🐑 Sadece İşletmeler (Hayvancılar)' },
                { value: 'sucukcu', label: '🥩 Sadece Üreticiler (Sucukçular)' },
                { value: 'market', label: '🛒 Sadece Marketler' },
              ]}
            />
          </Form.Item>
          <Form.Item
            name="title"
            label="Duyuru Başlığı"
            rules={[{ required: true, message: 'Başlık zorunludur' }]}
          >
            <Input placeholder="Duyuru başlığı..." />
          </Form.Item>
          <Form.Item
            name="message"
            label="Duyuru Mesajı"
            rules={[{ required: true, message: 'Mesaj metni zorunludur' }]}
          >
            <TextArea rows={4} placeholder="Seçilen hedef kitlenin tüm bildirim kutularına iletilecektir..." />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
