import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import FarmDetailPage from '../farmer/FarmDetailPage';
import api from '@/lib/axios';

vi.mock('@/lib/axios', () => {
  return {
    default: {
      get: vi.fn(),
      post: vi.fn(),
    },
    getApiErrorMessage: vi.fn((err) => err?.message || 'Error'),
  };
});

// Mock Recharts to avoid jsdom rendering issues with SVG/canvas
vi.mock('recharts', () => {
  return {
    ResponsiveContainer: ({ children }: any) => <div data-testid="responsive-container">{children}</div>,
    LineChart: ({ children }: any) => <div data-testid="line-chart">{children}</div>,
    Line: () => <div data-testid="line" />,
    XAxis: () => <div data-testid="x-axis" />,
    YAxis: () => <div data-testid="y-axis" />,
    CartesianGrid: () => <div data-testid="cartesian-grid" />,
    Tooltip: () => <div data-testid="tooltip" />,
    Legend: () => <div data-testid="legend" />,
  };
});

// Mock WebSocket to prevent actual connections
vi.mock('@/lib/websocket', () => ({
  connectToFarm: vi.fn(),
  disconnectWebSocket: vi.fn(),
}));

const mockFarm = {
  id: 'farm-1',
  name: 'Farm Test',
  location: 'Hanoi',
};

const mockModels = [
  { id: 'model-1', model_name: 'Temp Sensor', device_type: 'SENSOR', metric_type: 'TEMP' },
  { id: 'model-2', model_name: 'Water Pump', device_type: 'ACTUATOR' },
];

const mockDevices = [
  { id: 'dev-1', name: 'Sensor A', model_id: 'model-1', status: 'ACTIVE', device_type: 'SENSOR' },
  { id: 'dev-2', name: 'Pump A', model_id: 'model-2', status: 'ACTIVE', device_type: 'ACTUATOR', operating_mode: 'MANUAL', connection_status: 'ONLINE' },
];

describe('FarmDetailPage Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderComponent = () => {
    render(
      <MemoryRouter initialEntries={['/farms/farm-1']}>
        <Routes>
          <Route path="/farms/:farmId" element={<FarmDetailPage />} />
        </Routes>
      </MemoryRouter>
    );
  };

  it('renders loading state initially', () => {
    // Không mock resolved value ngay lập tức để thấy loading
    vi.mocked(api.get).mockImplementation(() => new Promise(() => {}));
    renderComponent();
    expect(screen.getByText('Đang tải chi tiết nông trại...')).toBeInTheDocument();
  });

  it('renders farm details and devices successfully', async () => {
    vi.mocked(api.get).mockImplementation((url: string) => {
      if (url.includes('/devices')) return Promise.resolve({ data: mockDevices });
      if (url.includes('/device-models')) return Promise.resolve({ data: mockModels });
      return Promise.resolve({ data: mockFarm });
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getAllByText('Farm Test')[0]).toBeInTheDocument();
    });

    // Overview Tab is active by default
    expect(screen.getByText('Sensor A')).toBeInTheDocument();
    expect(screen.getByText('Pump A')).toBeInTheDocument();
  });

  it('switches to telemetry tab and renders chart', async () => {
    vi.mocked(api.get).mockImplementation((url: string) => {
      if (url.includes('/devices')) return Promise.resolve({ data: mockDevices });
      if (url.includes('/device-models')) return Promise.resolve({ data: mockModels });
      if (url.includes('/telemetry')) return Promise.resolve({ data: [] });
      return Promise.resolve({ data: mockFarm });
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getAllByText('Farm Test')[0]).toBeInTheDocument();
    });

    const telemetryTabBtn = screen.getByText('Biểu đồ thống kê');
    fireEvent.click(telemetryTabBtn);

    await waitFor(() => {
      expect(screen.getByText('Thống kê cảm biến')).toBeInTheDocument();
    });
  });

  it('allows toggling actuator', async () => {
    vi.mocked(api.get).mockImplementation((url: string) => {
      if (url.includes('/devices')) return Promise.resolve({ data: mockDevices });
      if (url.includes('/device-models')) return Promise.resolve({ data: mockModels });
      return Promise.resolve({ data: mockFarm });
    });

    vi.mocked(api.post).mockResolvedValue({ data: { status: 'success' } });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Pump A')).toBeInTheDocument();
    });

    const toggleButton = screen.getByRole('button', { name: /Bật/i }); // "Bật" is shown when device is OFF? Or "OFF" -> "ON".
    // Wait, the ActuatorCard renders "BẬT" if it's off, or something similar. 
    // Let's just find the button by its class or text.
    fireEvent.click(toggleButton);

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/devices/dev-2/command', { command: 'ON' });
    });
  });
});
