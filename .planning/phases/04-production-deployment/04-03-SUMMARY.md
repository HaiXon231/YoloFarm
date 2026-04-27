# Plan 04-03: Frontend Deploy on Vercel

## What was built
- Chỉnh sửa `axios.ts` để sử dụng `import.meta.env.VITE_API_URL` thay cho chuỗi `/api/v1` hardcode.
- Chỉnh sửa `websocket.ts` (`connectToFarm`, `connectAdminStats`, `connectNotificationUnread`) để sử dụng `import.meta.env.VITE_WS_URL`.
- Cấu hình `vercel.json` định nghĩa catch-all rewrite rule (`/(.*)` -> `/index.html`) để Vite React Router (SPA) không bị lỗi 404 khi người dùng refresh các sub-path.

## Verification
- Lỗi CORS đã được phòng ngừa từ bên Backend (Plan 04-02).
- Vercel tự động build React Vite app mà không cần cấu hình thêm package.json.

## Next Steps
- Đẩy source code lên GitHub.
- Liên kết project Vercel với repository Github, cấu hình Environment Variables (`VITE_API_URL`, `VITE_WS_URL`) tương ứng với URL thật của HuggingFace Space backend.
