# Module A — Tra cứu Tour và gợi ý Tour theo sở thích

Sản phẩm thực hành của Nhóm 09 trong học phần Quản lý Dự án Phần mềm, PTIT. Module cung cấp tra cứu/lọc Tour, xếp hạng theo tỷ lệ trùng sở thích, xem chi tiết và quản trị sở thích trên phiên bản nháp.

Booking, giữ chỗ, thanh toán, AI tư vấn và bản đồ thuộc kế hoạch toàn hệ thống. Trong Module A, dữ liệu giá/chỗ dùng để chọn kết quả hiển thị.

## Tài liệu và dữ liệu đi kèm

| Tệp | Nội dung |
| --- | --- |
| [Hướng dẫn chạy](HUONG_DAN_CHAY_VA_DEMO.md) | Cài công cụ, chạy/dừng/reset, API, kiểm thử và xử lý lỗi |
| [Kịch bản chạy thử](KICH_BAN_DEMO_TRUOC_GIANG_VIEN.md) | Thao tác và kết quả mong đợi cho người trình bày/người kiểm tra |
| [Danh mục dữ liệu mẫu](docs/demo/DU_LIEU_MAU.md) | Tour, phiên bản, tag, giá và kết quả đối chứng |
| [schema.sql](backend/src/main/resources/schema.sql) | Lược đồ 10 bảng và ràng buộc khởi tạo |
| [data.sql](backend/src/main/resources/data.sql) | Dữ liệu tổng hợp cho demo và test |
| [Hợp đồng API](docs/qa/API_CONTRACT_Module_A.md) | Tham số, kiểu dữ liệu, mã lỗi và quyền |
| [Quyết định nghiệp vụ](docs/qa/module-a-contract-decisions.md) | A-ADR-01–04 và nội dung hiệu năng chờ xác nhận |
| [PostgreSQL](CHUYEN_SANG_POSTGRESQL.md) | Thiết lập cơ sở dữ liệu QA riêng |
| [Nguồn ảnh](frontend/public/images/credits.html) | Tác giả, nguồn và giấy phép ảnh công khai |

## Công nghệ và công cụ

| Thành phần | Phiên bản/cấu hình |
| --- | --- |
| Backend | JDK 21, Maven 3.9+, Spring Boot 3.3.5 |
| Frontend | Node.js 20+, React 18.3.1, Vite 6.4.6, React Router 7.18.5 |
| Browser test | Playwright 1.58.2 và Chromium tương ứng |
| Demo | H2 trong bộ nhớ, chế độ tương thích PostgreSQL |
| Script demo | Windows và PowerShell 7; Java/Maven/Node/npm trong PATH |

Lần cài đầu cần mạng để tải dependency và Chromium. Ảnh Tour được phục vụ cục bộ từ nguồn đi kèm.

## Chạy nhanh trên Windows

Clone repository có phiên bản này, chọn đúng nhánh rồi mở PowerShell tại thư mục có backend/, frontend/ và scripts/.

~~~powershell
java -version
mvn -version
node --version
npm --version

$demoRuntime = Join-Path $env:LOCALAPPDATA 'TourDiscoveryDemo'
$demoCredential = Join-Path $demoRuntime 'credential.clixml'

& .\scripts\demo\Set-DemoPassword.ps1 -CredentialPath $demoCredential
& .\scripts\demo\Start-Demo.ps1 -RuntimeRoot $demoRuntime -CredentialPath $demoCredential
~~~

Nhập mật khẩu do bạn đặt, tối thiểu 12 ký tự, tại lời nhắc ẩn. Credential được mã hóa bằng DPAPI cho tài khoản Windows hiện tại. Trên máy khác, tạo lại credential.

Script tạo bản sao nguồn trong runtime, chạy mvn clean verify, npm ci và npm run build rồi chờ backend và Vite sẵn sàng. Khi thành công, script in URL và vị trí log.

| Địa chỉ mặc định | Chức năng |
| --- | --- |
| http://127.0.0.1:15173/tours | Tra cứu và gợi ý |
| http://127.0.0.1:15173/tours/101 | Chi tiết Tour 101 |
| http://127.0.0.1:15173/admin | Đăng nhập và quản trị sở thích |
| http://127.0.0.1:18080/api/v1/tours?page=1&size=10 | API tra cứu |

Frontend dùng Vite và proxy /api tới backend. Bản frontend/dist được tạo trong bước build. Hướng dẫn chạy có thêm cách khởi động thủ công bằng hai terminal.

## Tài khoản và quyền

| Tài khoản | Vai trò | Quyền trong Module A |
| --- | --- | --- |
| Khách vãng lai | GUEST | Tra cứu, gợi ý, danh mục công khai và chi tiết |
| khach01 | CUSTOMER | Chức năng công khai |
| product01 | PRODUCT | Đọc/sửa tag DRAFT, đọc/tạo/cập nhật danh mục sở thích |
| manager01 | MANAGER | Đọc tag của phiên bản |
| admin01 | ADMIN | Minh họa kiểm tra quyền truy cập nội bộ |

Bốn tài khoản dùng mật khẩu đã đặt cho lần chạy. DataInitializer tạo tài khoản khi profile demo hoặc test được bật và có APP_DEMO_PASSWORD. Profile mặc định phục vụ API công khai, còn tài khoản mẫu cần profile tương ứng.

APP_JWT_SECRET dùng cho khóa JWT cố định. Nếu biến này trống, mỗi JVM sinh khóa mới. Khi khởi động lại bản demo mặc định, cần đăng nhập lại.

## Kết quả mẫu

Seed có 14 Tour, trong đó 12 Tour công khai. Với size=10, danh sách mặc định có hai trang. Trên dữ liệu mới khởi tạo, chọn DN, 3 ngày, ngân sách tối đa 5000000, sở thích BEACH và CULTURE:

| Thứ tự | Tour | Điểm | Giá từ (đồng) |
| --- | --- | --- | --- |
| 1 | 112 — Đà Nẵng - Hội An trọn gói | 100% | 4.900.000 |
| 2 | 113 — Đà Nẵng biển và văn hóa | 100% | 5.000.000 |
| 3 | 101 — Đà Nẵng biển và ẩm thực | 50% | 4.200.000 |

Tour 105 có nội dung PENDING và Tour 114 đã ARCHIVED, API chi tiết công khai trả 404. Phiên bản 201 là PUBLISHED của Tour 101; phiên bản 215 là DRAFT cho bài thực hành gắn tag.

## Quy tắc tìm kiếm và xếp hạng

1. Chuẩn hóa và kiểm tra đầu vào.
2. Chọn Tour ACTIVE, đúng phiên bản PUBLISHED hiện tại, chuyến OPEN trong cửa sổ nhận đặt và active offer APPROVED.
3. Lọc chỗ khi availableOnly=true, rồi áp từ khóa, điểm đến, số ngày và ngân sách.
4. Chọn giá thấp nhất đủ điều kiện cho từng Tour.
5. Tính round(100 × số sở thích trùng / số sở thích chọn, 2) bằng tag active của bản PUBLISHED.
6. Sắp toàn bộ kết quả theo điểm giảm, giá tăng, mã Tour số tăng, sau đó phân trang.

Khi để trống sở thích, matchScore=null, personalized=false và thứ tự theo giá/mã Tour. Tour có điểm 0 vẫn xuất hiện nếu qua các bộ lọc. Điểm biểu thị tỷ lệ trùng sở thích đã chọn.

## API chính

Tiền tố chung /api/v1. ID bigint và tiền VND nguyên trong DTO được trả dạng chuỗi.

| Method | Đường dẫn | Quyền |
| --- | --- | --- |
| GET | /destinations, /interests, /tours, /tour-recommendations, /tours/{tourId} | Công khai |
| POST | /auth/login | Thông tin đăng nhập hợp lệ |
| GET | /auth/me | JWT hợp lệ |
| GET | /internal/tour-versions/{versionId}/interests | PRODUCT, MANAGER |
| PUT | /internal/tour-versions/{versionId}/interests | PRODUCT, bản DRAFT |
| GET/POST | /internal/interests | PRODUCT |
| PATCH | /internal/interests/{interestId} | PRODUCT |

Tra cứu nhận keyword, destinationId, durationDays, maxBudget, interestCodes lặp lại, availableOnly, page và size. Tên ngoài hợp đồng, gồm minBudget/sort, trả 400. Ngân sách là số nguyên dương, page từ 1 và size 1–10. Điểm đến lạ hoặc sở thích ngừng hiệu lực trả 422. Chi tiết nằm trong hợp đồng API.

## Kiểm thử

Từ thư mục gốc, chạy kiểm tra backend:

~~~powershell
Push-Location backend
try {
    mvn clean verify
    if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed.' }
} finally { Pop-Location }
~~~

Kiểm tra frontend:

~~~powershell
Push-Location frontend
try {
    npm ci
    if ($LASTEXITCODE -ne 0) { throw 'npm ci failed.' }
    npm run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
} finally { Pop-Location }
~~~

Browser test cần dịch vụ đang chạy, Chromium và mật khẩu demo trong môi trường tiến trình. Xem [hướng dẫn kiểm thử](HUONG_DAN_CHAY_VA_DEMO.md#6-kiểm-thử-và-lưu-kết-quả) để cấu hình URL, credential và nơi ghi minh chứng.

JUnit ghi báo cáo ở backend/target/surefire-reports và các ca QA ghi thêm backend/target/module-a-evidence. Playwright ghi JSON, ảnh và trace theo MODULE_A_EVIDENCE_DIR. Mỗi lần chạy cần lưu mã commit, công cụ, cấu hình và kết quả thực tế. TC38 còn chờ ngưỡng hiệu năng/mức tải được xác nhận.

## Dừng và khôi phục seed

~~~powershell
& .\scripts\demo\Stop-Demo.ps1 -RuntimeRoot $demoRuntime
& .\scripts\demo\Start-Demo.ps1 -RuntimeRoot $demoRuntime -CredentialPath $demoCredential -SkipBuild
~~~

H2 trong bộ nhớ được dựng lại khi backend khởi động. Tag DRAFT 215 trở lại BEACH,FOOD và mục sở thích tạo thêm trong buổi thực hành được xóa theo vòng đời database.

-SkipBuild dùng bản build đã lưu trong runtime. Sau khi sửa mã hoặc mới clone lần đầu, chạy lại với bước build đầy đủ.

## Cấu trúc nguồn

~~~text
backend/src/main/java/       Controller, service, repository, security và entity
backend/src/main/resources/  schema.sql, data.sql và các profile
backend/src/test/            Unit, integration và QA tests
frontend/src/               Giao diện React và API client
frontend/public/images/     Ảnh công khai và attribution
frontend/tests/             Kiểm thử Playwright
demo-assets/                Ảnh seed nội bộ và manifest ảnh
scripts/demo/               Tạo credential, chạy/dừng và xác minh restart
docs/demo/                  Danh mục dữ liệu và kết quả đối chứng
docs/qa/                    Hợp đồng API và quyết định kiểm thử
~~~

Khi đưa lên Git, chọn mã nguồn, SQL seed, tài liệu và tài nguyên theo .gitignore. Credential, biến bí mật và dữ liệu runtime được lưu ở vị trí riêng hoặc thư mục đã loại trừ.
