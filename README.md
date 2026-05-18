# 💰 Hệ thống Quản lý Tài chính Cá nhân

Dự án phát triển ứng dụng Quản lý tài chính cá nhân dựa trên kiến trúc Client-Server và RESTful API. Giúp người dùng theo dõi thu chi, quản lý ngân sách và thống kê dữ liệu trực quan.

## 🏢 Thông tin chung

- **Trường:** Đại học Công nghiệp Hà Nội (HaUI) - Khoa CNTT
- **Môn học:** Phát triển ứng dụng trên thiết bị di động
- **Thực hiện:** Nhóm 5
- **Project Manager:** Nguyễn Danh Tuấn

## 🛠 Công nghệ sử dụng

- **Client (Frontend):** Android Studio (Java/Kotlin)
- **Server (Backend):** Node.js, Express.js
- **Database:** MongoDB (Mongoose)
- **Deploy:** Render (Backend), MongoDB Atlas (Database)

## ✨ Tính năng nổi bật (Features)

- 🔒 Xác thực người dùng an toàn bằng JWT.
- 📝 Quản lý CRUD (Thêm, sửa, xóa, xem) các khoản Thu/Chi.
- 🗂 Tự tạo và quản lý Danh mục thu chi cá nhân.
- 📊 Thống kê số dư và hiển thị trực quan.
- 🌐 Dữ liệu được đồng bộ realtime qua Cloud Database.

## 🚀 Hướng dẫn cài đặt (Local Development)

### 1. Cài đặt Backend

\`\`\`bash
cd backend
npm install
\`\`\`

- Tạo file `.env` dựa trên file `.env.example` và điền thông tin Database của bạn.
- Chạy server:
  \`\`\`bash
  npm start
  \`\`\`

### 2. Cài đặt Client

- Mở thư mục `android_client` bằng Android Studio.
- Đợi Gradle build xong.
- Đổi URL API trong code thành địa chỉ IP local của bạn hoặc link API từ Render.
- Chạy ứng dụng trên máy ảo (Emulator) hoặc thiết bị thật qua USB Debugging.

## 🔗 Liên kết hệ thống

- **Live API URL:** `https://your-api-link.onrender.com`
- **Tài khoản Test:** `test@gmail.com` / `123456`
