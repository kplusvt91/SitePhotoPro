# Site Photo Pro — Android source project

Ứng dụng Android Kotlin chụp ảnh hiện trường, lưu ảnh gốc và ảnh đóng dấu riêng, thêm logo từ thư viện, ngày giờ chụp, tên thiết bị và tên dự án (nếu nhập). Hoạt động offline sau khi cài đặt; Gradle cần internet ở lần tải thư viện đầu tiên.

## Yêu cầu
- Android Studio bản ổn định gần đây
- JDK 17
- Android SDK Platform 35
- Điện thoại Android 8.0 (API 26) trở lên để chạy app

## Mở và build APK
1. Giải nén `SitePhotoPro-Android-Source.zip`.
2. Mở Android Studio → **Open** → chọn thư mục `SitePhotoPro`.
3. Chờ Gradle Sync hoàn tất và cài các SDK được Android Studio đề xuất.
4. Kết nối điện thoại bằng USB, bật Developer options → USB debugging; hoặc chạy emulator.
5. Bấm **Run** để cài và thử ứng dụng.
6. Tạo APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
7. APK debug thường nằm tại `app/build/outputs/apk/debug/app-debug.apk`.
8. Để tạo bản phát hành: **Build → Generate Signed Bundle / APK → APK**, tạo hoặc chọn keystore và lưu an toàn.

## Cách sử dụng
1. Mở app và cấp quyền camera.
2. Bấm **Chọn logo công ty** để chọn PNG/JPG trong thư viện.
3. Nhập tên thiết bị; tên dự án có thể để trống.
4. Bấm **CHỤP ẢNH & ĐÓNG DẤU**.
5. Ảnh gốc lưu ở `Pictures/SitePhotoPro/Original`; ảnh đóng dấu ở `Pictures/SitePhotoPro/Stamped`.
6. Bấm **Chia sẻ ảnh đã đóng dấu** để gửi qua ứng dụng phù hợp.

## Ghi chú kỹ thuật
- App không cần tài khoản hoặc máy chủ và không gửi ảnh lên mạng.
- Ảnh gốc được giữ nguyên byte JPEG do CameraX tạo; ảnh đóng dấu được lưu thành JPEG chất lượng 98.
- Ngày giờ lấy từ đồng hồ điện thoại, nên cần đặt giờ hệ thống chính xác.
- Logo được đọc từ URI người dùng chọn. Nếu logo quá lớn hoặc có định dạng không hỗ trợ, hãy dùng PNG/JPG thông thường.
- Đây là mã nguồn dự án; file APK chỉ xuất hiện sau khi build thành công trong Android Studio. Cần thử trên thiết bị thật trước khi dùng trong quy trình nghiệm thu chính thức.
