import { Form, Input, Button, Card, Typography, message, Space, Tag } from 'antd';
import { UserOutlined, LockOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { useNavigate, Link } from 'react-router-dom';
import { authService } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import { useTranslation, LanguageSwitcher } from '../../i18n';

const { Title, Text } = Typography;

export default function LoginPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();
  const [form] = Form.useForm();

  async function onFinish(values: { email: string; password: string }) {
    try {
      const { data } = await authService.login(values.email, values.password);
      setAuth(data.user, data.token);
      message.success(t('auth.loginSuccess'));
      if (data.user?.role === 'admin') {
        navigate('/admin/users');
      } else {
        navigate('/');
      }
    } catch (err: unknown) {
      const errorMsg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error || t('auth.loginFailed');
      message.error(errorMsg);
    }
  }

  function setQuickAccount(email: string, pass: string) {
    form.setFieldsValue({ email, password: pass });
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', background: '#f0f2f5', position: 'relative' }}>
      <div style={{ position: 'absolute', top: 20, right: 24 }}>
        <LanguageSwitcher />
      </div>
      <Card style={{ width: 440, boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}>
        <Space direction="vertical" size="middle" style={{ width: '100%', textAlign: 'center' }}>
          <div>
            <Title level={2} style={{ marginBottom: 4 }}>🐄 AVATAR</Title>
            <Text type="secondary">{t('auth.systemSubtitle')}</Text>
          </div>

          {/* Hızlı Giriş Butonları */}
          <div style={{ background: '#fafafa', padding: '10px 12px', borderRadius: 8, border: '1px solid #f0f0f0', textAlign: 'left' }}>
            <Text type="secondary" style={{ fontSize: 11, display: 'block', marginBottom: 6 }}>
              Hızlı Test Girişi (Tek Tıkla Doldur):
            </Text>
            <Space wrap size={[6, 6]}>
              <Tag
                color="green"
                style={{ cursor: 'pointer', padding: '3px 8px' }}
                onClick={() => setQuickAccount('ahmet@gmail.com', '123456')}
              >
                🐑 Hayvancı
              </Tag>
              <Tag
                color="blue"
                style={{ cursor: 'pointer', padding: '3px 8px' }}
                onClick={() => setQuickAccount('eren@gmail.com', '123456')}
              >
                🥩 Sucukçu
              </Tag>
              <Tag
                color="orange"
                style={{ cursor: 'pointer', padding: '3px 8px' }}
                onClick={() => setQuickAccount('yunus@gmail.com', '123456')}
              >
                🛒 Market
              </Tag>
              <Tag
                color="purple"
                style={{ cursor: 'pointer', padding: '3px 8px' }}
                onClick={() => setQuickAccount('admin@avatar.com', 'admin123')}
              >
                <SafetyCertificateOutlined /> 🛡️ Admin
              </Tag>
            </Space>
          </div>

          <Form form={form} onFinish={onFinish} layout="vertical" size="large">
            <Form.Item name="email" rules={[{ required: true, type: 'email' }]}>
              <Input prefix={<UserOutlined />} placeholder={t('auth.email')} />
            </Form.Item>
            <Form.Item name="password" rules={[{ required: true }]}>
              <Input.Password prefix={<LockOutlined />} placeholder={t('auth.password')} />
            </Form.Item>
            <Form.Item style={{ marginBottom: 12 }}>
              <Button type="primary" htmlType="submit" block>{t('auth.login')}</Button>
            </Form.Item>
          </Form>
          <Text>{t('auth.noAccount')} <Link to="/register">{t('auth.register')}</Link></Text>
        </Space>
      </Card>
    </div>
  );
}
