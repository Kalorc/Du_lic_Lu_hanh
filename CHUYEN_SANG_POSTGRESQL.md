# Chạy Module A với PostgreSQL

Luồng H2 trong [hướng dẫn chạy](HUONG_DAN_CHAY_VA_DEMO.md) phù hợp cho trình bày nhanh. Luồng PostgreSQL dưới đây dành cho kiểm tra tương thích bằng một database QA riêng. Cần có PostgreSQL và các công cụ psql/createuser/createdb trong PATH, cùng quyền tạo role/database.

## 1. Tạo role và database riêng

Thực hiện bằng tài khoản quản trị PostgreSQL trên server bạn được phép sử dụng:

~~~powershell
createuser -h 127.0.0.1 -p 5432 -U postgres --pwprompt module_a_demo
createdb -h 127.0.0.1 -p 5432 -U postgres --owner=module_a_demo --encoding=UTF8 --template=template0 module_a_demo
~~~

Thay host/cổng/tài khoản quản trị theo server thực tế. Công cụ có thể hỏi cả mật khẩu role mới và mật khẩu tài khoản quản trị. Role ứng dụng sở hữu database QA; chỉ dùng tên này cho môi trường kiểm tra.

Kiểm tra database trống và encoding bằng psql. Các lệnh sau chạy từ thư mục gốc của repository:

~~~powershell
psql -h 127.0.0.1 -p 5432 -U module_a_demo -d module_a_demo -W -c "SELECT current_database(), current_user; SHOW server_encoding;"
psql -h 127.0.0.1 -p 5432 -U module_a_demo -d module_a_demo -W -c "SELECT tablename FROM pg_tables WHERE schemaname='public';"
~~~

Với database vừa tạo, danh sách bảng public cần rỗng. Kiểm tra collation/casefold tiếng Việt trên server sẽ dùng; locale của từng hệ điều hành có thể làm kết quả LOWER khác nhau.

## 2. Nạp schema và dữ liệu mẫu

schema.sql có các lệnh DROP TABLE cho bảng Module A. Chỉ chạy bước này trên database QA trống đã kiểm tra. Với database có dữ liệu cần giữ, dùng migration và phương án sao lưu đã được người quản lý database xác nhận.

~~~powershell
psql -h 127.0.0.1 -p 5432 -U module_a_demo -d module_a_demo -W -v ON_ERROR_STOP=1 --single-transaction -f backend/src/main/resources/schema.sql -f backend/src/main/resources/data.sql
if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL seed import failed.' }
~~~

Cuối data.sql đã đặt lại identity cho dữ liệu tạo tiếp theo: interest bắt đầu từ 100; Tour, phiên bản, media, chuyến và offer từ 1000. app_users được DataInitializer điền khi bật profile demo và có APP_DEMO_PASSWORD. Có thể kiểm tra bộ đếm của database QA trong psql bằng SELECT pg_get_serial_sequence cùng tên bảng/cột tương ứng.

## 3. Chạy backend với profile PostgreSQL

Dừng backend H2 đang dùng cổng 18080 trước. Nhập mật khẩu của role database và mật khẩu cho bốn tài khoản demo. Khối PowerShell sau chạy từ thư mục gốc:

~~~powershell
$pgCredential = [PSCredential]::new('module_a_demo', (Read-Host 'Mat khau PostgreSQL role module_a_demo' -AsSecureString))
$demoLogin = [PSCredential]::new('demo', (Read-Host 'Mat khau tai khoan demo' -AsSecureString))
$pgSaved = @{}
foreach ($name in 'DB_URL','DB_USER','DB_PASSWORD','DB_INIT_MODE','APP_DEMO_PASSWORD') {
    $pgSaved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
Push-Location backend
try {
    $env:DB_URL = 'jdbc:postgresql://127.0.0.1:5432/module_a_demo'
    $env:DB_USER = 'module_a_demo'
    $env:DB_PASSWORD = $pgCredential.GetNetworkCredential().Password
    $env:DB_INIT_MODE = 'never'
    $env:APP_DEMO_PASSWORD = $demoLogin.GetNetworkCredential().Password
    mvn spring-boot:run '-Dspring-boot.run.profiles=demo,postgres'
} finally {
    foreach ($name in $pgSaved.Keys) {
        [Environment]::SetEnvironmentVariable($name, $pgSaved[$name], 'Process')
    }
    $pgCredential = $null
    $demoLogin = $null
    Pop-Location
}
~~~

Dùng DB_INIT_MODE=never vì đã import ở bước 2. Profile demo đặt cổng 18080 và tạo tài khoản; profile postgres chọn driver và kết nối PostgreSQL. Nếu tài khoản đã tồn tại, DataInitializer giữ bản ghi hiện có, nên đổi APP_DEMO_PASSWORD ở lần chạy sau cần quy trình đổi mật khẩu tài khoản tương ứng.

Chạy frontend ở terminal khác theo mục 4 của hướng dẫn, proxy tới http://127.0.0.1:18080.

## 4. Kiểm tra

Dùng các ví dụ API và kịch bản của bộ tài liệu để kiểm tra:

- Danh sách mặc định 12 Tour và thứ tự 112–113–101 trong truy vấn DN/3 ngày/5 triệu.
- Từ khóa có dấu và hoa/thường, chẳng hạn Đà Nẵng, đà nẵng và dạng Unicode tổ hợp.
- API tạo danh mục PRODUCT để xác nhận sequence không trùng ID seed.
- Quyền đọc/ghi tag DRAFT và phản hồi khóa PUBLISHED.
- Thử lại sau khi khởi động backend để kiểm tra dữ liệu PostgreSQL được lưu.

PostgreSQL lưu dữ liệu qua các lần chạy. Muốn khôi phục seed, tạo một database QA trống mới hoặc dùng quy trình reset database riêng đã được xác nhận. Script Start-Demo/Test-DemoRestart trong scripts/demo được thiết kế cho H2 memory và dùng cho luồng H2.

## 5. Phạm vi kiểm chứng

Lược đồ đang chạy có 10 bảng và active_offer_id. Hợp đồng giá dựa trên bản giá đang dùng được duyệt. Khi đổi lược đồ hoặc đưa lên hệ thống thương mại, cần migration, kiểm tra ràng buộc và đo tải theo cấu hình đích.

Hồ sơ QA cũ của nhóm có ghi các lần thử PostgreSQL riêng. Người chạy bộ hướng dẫn này cần lưu version, locale, database, commit và kết quả của lần chạy thực tế. TC38 còn chờ ngưỡng hiệu năng được xác nhận.
