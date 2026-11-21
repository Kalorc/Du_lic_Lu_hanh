# Module Tra cứu Tour và Gợi ý Tour theo Sở thích

Hiện thực module theo đặc tả `MODULE TRA CỨU TOUR VÀ GỢI Ý TOUR THEO SỞ THÍCH.docx`
của đề tài *Phần mềm Quản lý Du lịch và Lữ hành Thông minh* (Nhóm 09).

Kiến trúc đúng theo đặc tả:

```
Tầng giao diện (React) → Auth(JWT) → Controller → Service → Repository → DB
```

| Thành phần | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 3.3.5, Spring Data JPA, Spring Security + JWT (jjwt) |
| CSDL | H2 in-memory, `MODE=PostgreSQL`, nạp sẵn `schema.sql` + `data.sql` |
| Frontend | React 18 + Vite 6.4.6, React Router 7.18.5 |

---

## 1. Chạy và trình bày demo

Xem [hướng dẫn chạy đã kiểm chứng](HUONG_DAN_CHAY_VA_DEMO.md) và [kịch bản6 phút30 giây](KICH_BAN_DEMO_TRUOC_GIANG_VIEN.md).

Trong PowerShell tại repository:

```powershell
$demoRuntime = 'E:\2.PTIT\QLDA\module-a-demo-runtime-20261121\local-demo'
$demoCredential = Join-Path $demoRuntime 'credential.clixml'
& '.\scripts\demo\Set-DemoPassword.ps1' -CredentialPath $demoCredential
& '.\scripts\demo\Start-Demo.ps1' -RuntimeRoot $demoRuntime -CredentialPath $demoCredential
```

Nhập mật khẩu riêng tối thiểu12 ký tự ở lời nhắc ẩn; credential DPAPI nằm ngoài Git. Script tạo bản sao sạch, chạy `mvn clean verify`, `npm ci`, `npm run build`, khởi tạo H2 memory rồi mở backend18080/frontend15173 trên loopback. Website: http://127.0.0.1:15173/tours; đăng nhập: http://127.0.0.1:15173/admin. Không dừng các dịch vụ sẵn có ở8080/5173. Xem hướng dẫn đầy đủ để chạy test, dừng/reset và khởi động lại bằng `-SkipBuild`.

Ảnh Tour có nguồn/tác giả/giấy phép tại [attribution](frontend/public/images/credits.html); toàn bộ ảnh được phục vụ local. CSDL H2 demo tạo lại từ schema/data mỗi lần khởi động; các thay đổi tag demo không được lưu lâu dài. Profile demo tắt H2 Console.

Phát triển thủ công vẫn dùng cấu hình mặc định backend8080 và Vite5173 nếu cổng trống. **Tài khoản mẫu chỉ được tạo ở profile demo/test và khi có APP_DEMO_PASSWORD**; đặt mật khẩu ở profile mặc định/production không bật seed. Khóa JWT ngẫu nhiên theo JVM nếu không cấp APP_JWT_SECRET. Không ghi password, token hoặc secret vào Git/log.

---

## 2. Tài khoản demo

Profile demo lấy mật khẩu từ `APP_DEMO_PASSWORD` do script truyền vào riêng tiến trình. JUnit dùng fixture riêng trong tài
nguyên test, không phải thông tin đăng nhập triển khai.

| Tài khoản | Vai trò | Quyền |
|---|---|---|
| *(không đăng nhập)* | GUEST | Tra cứu, xem gợi ý, xem tour công khai |
| `khach01` | CUSTOMER | Giống GUEST |
| `product01` | PRODUCT | Gắn sở thích cho Tour, quản lý danh mục sở thích |
| `manager01` | MANAGER | Chỉ xem sở thích của phiên bản Tour |
| `admin01` | ADMIN | Không có quyền nội bộ Module A; không có màn hình quản trị tài khoản |

---

## 3. Danh sách API

### API công khai (mở cho cả người dùng chưa đăng nhập)

| # | Method | Endpoint | Công dụng |
|---|---|---|---|
| 1 | GET | `/api/v1/interests` | Danh mục sở thích đang hoạt động cho bộ lọc |
| 2 | GET | `/api/v1/destinations` | Danh mục điểm đến (tùy chọn `?type=CITY`) |
| 3 | GET | `/api/v1/tours` | Tìm kiếm và lọc danh sách tour |
| 4 | GET | `/api/v1/tour-recommendations` | Gợi ý và xếp hạng tour theo sở thích |
| 5 | GET | `/api/v1/tours/{tourId}` | Chi tiết một tour công khai |

Tham số của API 3 và API 4 giống nhau: `keyword`, `destinationId`, `durationDays`,
`maxBudget`, `interestCodes` (lặp lại nhiều lần), `availableOnly`, `page`, `size`.
Tên tham số khác, gồm `minBudget` và `sort`, trả 400 `INVALID_FILTER`.
`keyword` được trim/NFC, không phân biệt hoa thường, phân biệt dấu; `%`, `_`
được tìm như ký tự thật. Điểm đến lạ/inactive trả 422 `INVALID_DESTINATION`.
Giới hạn: keyword 100 ký tự, tối đa 10 mã sở thích duy nhất, page từ 1, size 1–10.

### API nội bộ (bắt buộc JWT)

| # | Method | Endpoint | Vai trò |
|---|---|---|---|
| 6 | GET | `/api/v1/internal/tour-versions/{versionId}/interests` | PRODUCT, MANAGER |
| 7 | PUT | `/api/v1/internal/tour-versions/{versionId}/interests` | PRODUCT |
| 8 | POST | `/api/v1/internal/interests` | PRODUCT |
| 9 | PATCH | `/api/v1/internal/interests/{interestId}` | PRODUCT |

`GET /api/v1/internal/interests` chỉ dành cho PRODUCT, trả danh mục quản trị
gồm cả mục đã tắt. MANAGER chỉ xem tag của một phiên bản qua API 6.

Phụ trợ: `POST /api/v1/auth/login` để lấy token, `GET /api/v1/auth/me` để xem
thông tin phiên đăng nhập.

---

## 4. Thuật toán gợi ý

Thứ tự xử lý đúng theo mục 5 của đặc tả:

1. **Trim → validate → normalize** `interestCodes` — `SearchParamValidator`
2. **Chọn ứng viên**: Tour `ACTIVE`, phiên bản là `current_published_version_id`
   và có `content_status = PUBLISHED`, chuyến khởi hành `OPEN` nằm trong cửa sổ
   đặt tại `asOf`, bản giá đang active có `approval_status = APPROVED`, và còn chỗ
   nếu `availableOnly = true`
3. **Áp hard filters** theo `keyword`, `destinationId`, `durationDays`, `maxBudget`
4. **Lấy chuyến khởi hành rẻ nhất đủ điều kiện** của từng Tour — `TourSearchDao`
5. **Join tags**, chỉ tính các sở thích đang `is_active = true`
6. **Chấm điểm** — `InterestScorer`
7. **Sắp xếp** `matchScore DESC NULLS LAST, priceFrom ASC, tourId ASC`
8. **Phân trang**

### Hàm chấm điểm

Cho `S` là tập mã sở thích hợp lệ, không trùng nhau, do khách chọn; `T` là tập tag
thuộc phiên bản đã công bố và đang active của một tour.

```
matched      = S ∩ T
matchedCount = |matched|

|S| > 0  →  matchScore = round(100 × matchedCount / |S|, 2)     (0–100)
|S| = 0  →  matchScore = null,  sắp theo giá tăng rồi tourId tăng
```

`matchScore` là **tỷ lệ trùng sở thích**. Đây không phải xác suất và cũng không
phải tỷ lệ hài lòng dự báo. Khi khách không chọn sở thích nào, hệ thống không
tuyên bố "gợi ý cá nhân hóa".

Tour có `matched = 0` **vẫn được giữ** nếu đạt các hard filters.

---

## 5. Hai quy tắc nghiệp vụ dễ làm sai

**Chỉ dùng bản giá đã được phê duyệt.** Chuyến 301 có bản giá v1 đã duyệt
4.200.000 ₫ và bản v2 đang chờ duyệt 3.000.000 ₫. Khi khách tìm tour, hệ thống
vẫn phải dùng 4.200.000 ₫.

**Loại chuyến hết chỗ trước khi tìm giá thấp nhất.** Tour 110 có chuyến
3.900.000 ₫ đã hết chỗ và chuyến 4.400.000 ₫ còn chỗ. Với `availableOnly = true`,
giá đại diện là 4.400.000 ₫ — không được quảng cáo mức giá của chuyến không còn
đủ điều kiện bán.

---

## 6. Dữ liệu mẫu

`data.sql` dựng lại các ví dụ trong đặc tả để kiểm chứng từng nhánh của thuật toán:

| Tour | Tình huống minh họa |
|---|---|
| 101 | Ba chuyến giá 4,2 / 4,6 / 5,2 triệu; chuyến 301 có bản giá v2 chờ duyệt |
| 112, 113 | Cùng trùng 2/2 sở thích → cùng 100,0 điểm, phân định bằng giá |
| 102 | Bị loại vì vượt ngân sách (6,0 triệu) |
| 103 | Bị loại vì sai điểm đến (Đà Lạt) |
| 104 | Bị loại vì sai thời lượng (4 ngày) |
| 105 | Bị loại vì nội dung còn `PENDING`, chưa công bố |
| 110 | Chuyến rẻ nhất đã hết chỗ → minh họa `availableOnly` |
| 114 | Tour `ARCHIVED`, không xuất hiện trong kết quả công khai |
| v215 | Bản nháp `DRAFT` của tour 101 — phiên bản duy nhất sửa được sở thích |
| FESTIVAL | Sở thích `is_active = false`, không được tính điểm |

---

## 7. Bảng mã lỗi

Mọi phản hồi dùng chung một bao bì: `{ "success", "data", "meta", "error" }`.
Thân lỗi mang `code`, `message`, `details`, `requestId`, `timestamp`.

| HTTP | Code | Trường hợp |
|---|---|---|
| 400 | `INVALID_FILTER` / `INVALID_BUDGET` / `INVALID_PAGINATION` | Tham số không hợp lệ |
| 401 | `UNAUTHENTICATED` | Thiếu hoặc sai token ở API nội bộ |
| 403 | `FORBIDDEN` | Không đủ quyền |
| 404 | `TOUR_NOT_FOUND` / `VERSION_NOT_FOUND` / `INTEREST_NOT_FOUND` | Đối tượng không tồn tại hoặc không được public |
| 409 | `VERSION_LOCKED` / `TAG_UPDATE_CONFLICT` | Sửa phiên bản không phải `DRAFT`, xung đột cập nhật |
| 422 | `INVALID_INTEREST_CODE` / `DUPLICATE_INTEREST_CODE` | Sở thích không tồn tại / không active, hoặc body trùng mã |
| 422 | `INVALID_DESTINATION` | Điểm đến không tồn tại hoặc không active |
| 429 | `RATE_LIMITED` | Khi có cấu hình rate limit |
| 500 | `INTERNAL_ERROR` | Lỗi không mong đợi, log server kèm `requestId` |

---

## 8. Cấu trúc thư mục

```
backend/
  src/main/java/vn/ptit/tour/
    controller/   4 controller: catalog, tra cứu/gợi ý, nội bộ, đăng nhập
    service/      SearchParamValidator, InterestScorer, TourSearchService,
                  TourDetailService, CatalogService, InterestAdminService
    repository/   JPA repositories + TourSearchDao (SQL hard filter)
    entity/       10 bảng theo đặc tả + app_users
    dto/          request/response records
    security/     JwtService, JwtAuthenticationFilter
    config/       SecurityConfig (ma trận quyền), DataInitializer
    exception/    ErrorCode, ApiException, GlobalExceptionHandler
  src/main/resources/
    schema.sql    DDL đúng kiểu dữ liệu đặc tả
    data.sql      dữ liệu mẫu dựng lại các ví dụ
  src/test/java/  unit test hàm chấm điểm + integration test 9 API

frontend/
  src/
    api/client.js      lớp gọi API duy nhất, quản lý JWT
    pages/             SearchPage, TourDetailPage, AdminPage
    components/        TopBar, FilterPanel, TourCard, Notice, ErrorNotice
    styles/app.css
```

## 9. Kiểm thử giao diện và PostgreSQL

Node.js >=20. Chạy `npm ci`, `npm run build`, `npm run test:browser` trong
`frontend` khi hệ thống đang hoạt động; bản demo dùng18080/15173 theo hướng dẫn chạy. Cài Chromium tương ứng
với Playwright1.58.2 (`npx playwright install chromium`) nếu máy chưa có.
`MODULE_A_BASE_URL`, `MODULE_A_ADMIN_API_URL` và `MODULE_A_EVIDENCE_DIR` chọn URL/nơi lưu minh chứng.
Các ca admin nhận mật khẩu demo qua `MODULE_A_DEMO_PASSWORD` trong môi trường tiến trình; hướng dẫn chạy giải mã credential DPAPI rồi dọn biến sau test;
không ghi credential vào source, trace hoặc báo cáo.

Profile PostgreSQL, biến DB và khởi tạo trên DB trống: [hướng dẫn PostgreSQL](CHUYEN_SANG_POSTGRESQL.md).
Hợp đồng đầy đủ: [API Module A](docs/qa/API_CONTRACT_Module_A.md),
[các quyết định](docs/qa/module-a-contract-decisions.md).

`mvn -Dtest=ModuleAPerformanceIT test` là bộ đo riêng, dùng DB QA tách biệt và
cấu hình trong Test_Scripts/run-module-a-performance.ps1 của hồ sơ QA.
Số đo p95 không tự xác nhận SLA khi chưa có ngưỡng được chốt.
