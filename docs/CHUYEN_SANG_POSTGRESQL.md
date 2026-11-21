# Chạy Module A với PostgreSQL

Backend đã có JDBC driver PostgreSQL và profile `postgres`. Schema thực tế của module được giữ nguyên. Cùng `schema.sql` và `data.sql` chạy được trên H2 và PostgreSQL; JSON vẫn là TEXT theo entity hiện hành. DDL đề xuất trong hồ sơ thiết kế có tên/cột khác, không dùng thay thế schema ứng dụng.

Chuẩn bị một cơ sở dữ liệu QA **trống, tách biệt** và cấp quyền trước khi chạy. Ví dụ PowerShell:

Database phải có phân loại ký tự Unicode để `LOWER` xử lý tiếng Việt đúng.
UTF-8 encoding riêng lẻ chưa đủ nếu database kế thừa locale `C`. Trên PostgreSQL18,
đợt QA dùng database mới tạo bằng lệnh sau (không thay locale của DB đang có dữ liệu):

```sql
CREATE DATABASE module_a_qa
    TEMPLATE template0 ENCODING 'UTF8'
    LOCALE_PROVIDER builtin BUILTIN_LOCALE 'C.UTF-8';
```

Đây là cấu hình được kiểm chứng trên PostgreSQL18; không suy diễn cho PostgreSQL16.
Tham chiếu [CREATE DATABASE PostgreSQL18](https://www.postgresql.org/docs/18/sql-createdatabase.html).
Sau khi tạo DB, kiểm tra API với `keyword=ĐÀ NẴNG` và Unicode NFD trước khi nghiệm thu.

```powershell
$env:DB_URL = 'jdbc:postgresql://127.0.0.1:15432/module_a_qa'
$env:DB_USER = 'qa_module_a'
$env:DB_PASSWORD = Read-Host 'Mật khẩu DB QA'
$env:DB_INIT_MODE = 'always'
cd backend
mvn spring-boot:run '-Dspring-boot.run.profiles=postgres'
```

Profile `postgres` đơn lẻ không tạo tài khoản mẫu. Để trình diễn đăng nhập với
PRODUCT/MANAGER, dùng H2 local qua [hướng dẫn demo](HUONG_DAN_CHAY_VA_DEMO.md).
Không dùng cơ chế reset tài khoản demo lên DB PostgreSQL có dữ liệu cần giữ.

Chỉ bật khởi tạo SQL cho DB trống: script demo có DROP/CREATE, không phải migration nâng cấp dữ liệu đang vận hành. Sau lần khởi tạo đầu, đặt `DB_INIT_MODE=never` (mặc định profile PostgreSQL). Không dùng database thật cho reset, ngắt DB hoặc tạo tải.

Từ JAR: `java -jar target/tour-discovery-1.0.0.jar --spring.profiles.active=postgres`. H2 Console tắt trong profile PostgreSQL. Cấp `APP_JWT_SECRET` riêng qua môi trường nếu cần giữ token qua lần khởi động lại.

QA ngày 18/11/2026 sử dụng PostgreSQL 18.0 tại cụm riêng cổng15432, database nghiệm thu kỹ thuật dùng builtin C.UTF-8. Giữ bằng chứng FAIL trên localeC và retest sau sửa cấu hình. Dịch vụ có sẵn cổng5432 không được sử dụng. Kết quả H2/PostgreSQL ghi riêng, không suy diễn tương thích giữa các phiên bản chưa chạy.
