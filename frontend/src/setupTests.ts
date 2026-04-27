import '@testing-library/jest-dom';
import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';

// Cleanup sau mỗi test để tránh leak state giữa các component
afterEach(() => {
    cleanup();
});

// Mock ResizeObserver cho jsdom (thường cần cho các biểu đồ như Recharts)
class ResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
}
window.ResizeObserver = ResizeObserver;
