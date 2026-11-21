# Kiểm chứng giao diện và demo Module A — 21/11/2026

Đã chạy bản demo local tại http://127.0.0.1:15173/tours, backend18080. Phiên bản nội dung: `source-sha256:787564d9f67af7dc03f9ae3edc8c0ba01a4f5b28dc10fc4a11d16e0a9f2b558a`; repository chưa có Git commit. Ngày giờ kiểm định: 2026-11-21T07:22:39.969542+07:00.

| Kiểm tra | Kết quả thực tế |
|---|---|
| Baseline backend trước sửa |78 tests,0 failure/error/skip |
| Backend clean verify/package từ bản sao sạch |83 tests,0 failure/error/skip |
| Frontend npm ci/build |Thành công;51 modules; cài mới từ lockfile |
| Browser cuối |26/26 PASS,0 skipped; gồm17 hồi quy và9 ca mới |
| Ảnh |14 ảnh riêng/28 WebP;12 public,2 Tour chưa công khai giữ ảnh ngoài frontend; decode/hash/dimension và quyền nguồn đã kiểm |
| Danh sách/chi tiết |12 public Tours,2 trang; ảnh được kiểm trên tất cả12 detail;105/114 vẫn404 |
| Demo theo thứ tự |9 bước PASS;112=100%/4.900.000đ,113=100%/5.000.000đ,101=50%/4.200.000đ |
| Nội bộ |PRODUCT tạo/tắt danh mục và lưu DRAFT; public201 không đổi; PUBLISHED409,MANAGER403,anonymous401 |
| Restart/reset |Dừng/chạy lại bằng script PASS;4 vai trò đăng nhập200; token cũ401; DRAFT215 trở vềBEACH+FOOD;12 Tour/giá/thứ tự giữ đúng seed |
| Credential |DPAPI ngoài Git, process ENV cho test; đổi mật khẩu thực tế và xóa plaintext tạm; khoảng trắng biên được giữ nguyên |
| Dependency audit ngày21/11 |Full0,production0; số đo của đợt này, không sao chép trạng thái audit ngày18/11 |

Giao diện giữ React/Vite, tông xanh ngọc/nền sáng; card dùng ảnh640px/srcset, detail1280px; thiếu/lỗi ảnh dùng khung minh họa SVG có nhãn. Đã kiểm360/768/1440, focus/Tab/Enter, retry/race, trạng thái tải/rỗng/lỗi; xem ảnh chụp thực tế. Bộ lọc, điểm số, sort toàn cục, phân trang, vai trò và các ADR01–04 giữ nguyên. Gallery dùng cấu trúc media hiện có, không thêm API/booking/thanh toán.

Hướng dẫn chạy: [HUONG_DAN_CHAY_VA_DEMO.md](../../HUONG_DAN_CHAY_VA_DEMO.md). Kịch bản: [KICH_BAN_DEMO_TRUOC_GIANG_VIEN.md](../../KICH_BAN_DEMO_TRUOC_GIANG_VIEN.md). Thời lượng6 phút30 là dự kiến cho người thuyết trình; automation xác minh thứ tự/thao tác/kết quả, không giả thời lượng nói.

Hồ sơ bàn giao và log: [Demo_Module_A_20261121](<E:/2.PTIT/QLDA/Báo cáo Quản lý dự án phần mềm/Kiểm thử và Đảm bảo chất lượng/Demo_Module_A_20261121>). Xem Changed_Files.md/json, Source_Manifest.json, Evidence_Manifest.json, Verification_Result.json, Code_Review_Record.md và Test_Evidence. Minh chứng RED/FAIL và harness failure được giữ riêng, không dùng làm kết quả bản cuối. API JSON/JUnit giữ sourceId thô working-tree; manifest ngoài cùng được đối chiếu với từng file ứng dụng trong bản đã build, kèm SHA-256 JAR/dist, không sửa thô thành Git SHA giả.

Các tài liệu mã nguồn/cài đặt hiện hành đã được đọc và đối chiếu16 tệp. Bằng chứng cập nhật đặt riêng, không sửa hồ sơ45 ca cũ. TC38 vẫn thiếu tiêu chí tải/p95 được xác nhận; kết quả83/26 không được gọi là45/45 hoặc nghiệm thu thương mại.
