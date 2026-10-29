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
| Frontend | React 18 + Vite 5, React Router 6 |

---

## 1. Chạy thử

### Backend

```bash
cd backend
mvn spring-boot:run
```

Server chạy tại `http://localhost:8080`. CSDL H2 được tạo lại mỗi lần khởi động
và nạp sẵn dữ liệu mẫu, không cần cài đặt gì thêm.

Xem CSDL bằng H2 Console: `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:mem:tourdb`, user `sa`, mật khẩu để trống).

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Giao diện chạy tại `http://localhost:5173`. Vite đã cấu hình proxy `/api` sang
cổng 8080 nên không gặp vấn đề CORS khi phát triển.

### Chạy kiểm thử

```bash
cd backend
mvn test
```

---

## 2. Tài khoản demo

Mật khẩu của cả bốn tài khoản là `123456`.

| Tài khoản | Vai trò | Quyền |
|---|---|---|
| *(không đăng nhập)* | GUEST | Tra cứu, xem gợi ý, xem tour công khai |
| `khach01` | CUSTOMER | Giống GUEST |
| `product01` | PRODUCT | Gắn sở thích cho Tour, quản lý danh mục sở thích |
| `manager01` | MANAGER | Chỉ xem sở thích của phiên bản Tour |
| `admin01` | ADMIN | Quản trị tài khoản; **không** mặc nhiên được sửa nội dung |

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

### API nội bộ (bắt buộc JWT)

| # | Method | Endpoint | Vai trò |
|---|---|---|---|
| 6 | GET | `/api/v1/internal/tour-versions/{versionId}/interests` | PRODUCT, MANAGER |
| 7 | PUT | `/api/v1/internal/tour-versions/{versionId}/interests` | PRODUCT |
| 8 | POST | `/api/v1/internal/interests` | PRODUCT |
| 9 | PATCH | `/api/v1/internal/interests/{interestId}` | PRODUCT |

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
