# Plan 03-01: Test Setup & Store Tests

## What was built
- Cài đặt `vitest`, `jsdom`, và các thư viện `@testing-library/*`.
- Cấu hình Vite (`vite.config.ts`) và `setupTests.ts` để hỗ trợ môi trường test `jsdom` kèm mock `ResizeObserver`.
- Viết bộ unit test cho `authStore` (`src/stores/__tests__/authStore.test.ts`), phủ sóng các action như `login`, `logout`, `loadFromStorage`, `fetchProfile`.
- Viết bộ unit test cho `notificationStore` (`src/stores/__tests__/notificationStore.test.ts`), bao phủ `fetchNotifications`, `markAsRead` và quản lý trạng thái UI (`togglePanel`, `closePanel`).

## Verification
- Tất cả unit tests cho stores đều pass (`npx vitest run`).
- State được reset độc lập thông qua `beforeEach` tránh tình trạng rò rỉ trạng thái.

## Next Steps
- Hoàn thiện integration test cho page trong Plan 03-02.
