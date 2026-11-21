# Dữ liệu mẫu và kết quả đối chiếu cho demo Module A

Tài liệu mô tả seed hiện tại của Module Tra cứu Tour và Gợi ý Tour theo sở thích. Các số lượng, ID, trạng thái và giá trong phần seed được đọc từ [schema.sql](../../backend/src/main/resources/schema.sql) và [data.sql](../../backend/src/main/resources/data.sql). Các kết quả tìm kiếm bên dưới được suy ra từ seed cùng điều kiện trong [TourSearchDao](../../backend/src/main/java/vn/ptit/tour/repository/TourSearchDao.java), [TourSearchService](../../backend/src/main/java/vn/ptit/tour/service/TourSearchService.java) và được đối chiếu với [TourSearchIntegrationTest](../../backend/src/test/java/vn/ptit/tour/controller/TourSearchIntegrationTest.java).

Tên địa điểm, Tour, giá, chỗ, chính sách, lịch trình và tài khoản phục vụ trình diễn học phần. Địa danh có thể là địa danh thật, còn sản phẩm Tour và điều kiện bán là dữ liệu tổng hợp. Giá trong seed không phải báo giá du lịch hiện hành. Các trường tỉnh/thành và vùng phản ánh chuỗi dữ liệu trong seed, chưa được cập nhật theo danh mục hành chính hiện hành.

Các kết quả dự kiến áp dụng sau khi khởi tạo H2 mới, trong cửa sổ bán còn hiệu lực và trước khi sửa danh mục hoặc chạy ca quản trị. Hướng dẫn khởi động ở [README](../../README.md), hướng dẫn thao tác ở [HUONG_DAN_CHAY_VA_DEMO.md](../../HUONG_DAN_CHAY_VA_DEMO.md), kịch bản trình bày ở [KICH_BAN_DEMO_TRUOC_GIANG_VIEN.md](../../KICH_BAN_DEMO_TRUOC_GIANG_VIEN.md).

## 1. Số lượng bản ghi của seed

| Bảng | Sau `schema.sql` và `data.sql` | Nội dung đối chiếu |
| --- | ---: | --- |
| `app_users` | 0 | Tài khoản do Java khởi tạo có điều kiện, xem mục 7 |
| `destinations` | 13 | 3 REGION, 9 CITY, 1 ATTRACTION; cả 13 đang active |
| `tours` | 14 | ID 101–114; 13 ACTIVE, 1 ARCHIVED |
| `tour_versions` | 15 | ID 201–215; 13 PUBLISHED, 1 PENDING, 1 DRAFT |
| `tour_version_destinations` | 18 | Liên kết điểm đến theo phiên bản |
| `tour_interests` | 11 | 10 active và FESTIVAL inactive |
| `tour_version_interest_tags` | 36 | Có cả tag của phiên bản nháp và tag FESTIVAL inactive |
| `tour_media` | 15 | Một cover cho mỗi phiên bản; bản 215 dùng cùng URL ảnh với 201 |
| `tour_departures` | 19 | 18 OPEN, 1 CLOSED; còn phải kiểm tra cửa sổ bán, giá và chỗ |
| `departure_offer_versions` | 20 | 18 APPROVED, 1 PENDING, 1 DRAFT |

Trong profile `demo` hoặc `test`, nếu đã đặt `APP_DEMO_PASSWORD`, [DataInitializer](../../backend/src/main/java/vn/ptit/tour/config/DataInitializer.java) tạo thêm 4 tài khoản khi chưa tồn tại. Vì vậy bảng `app_users` có 4 dòng trong phiên demo mới khởi tạo bằng script chuẩn. Số Tour công khai là kết quả truy vấn, không phải số dòng của bảng `tours`.

## 2. Điểm đến và mã lọc

| ID | Tên seed | Loại | Parent |
| --- | --- | --- | --- |
| MIENTRUNG | Miền Trung | REGION | — |
| MIENBAC | Miền Bắc | REGION | — |
| MIENNAM | Miền Nam | REGION | — |
| DN | Đà Nẵng | CITY | MIENTRUNG |
| HOIAN | Hội An | CITY | MIENTRUNG |
| HUE | Huế | CITY | MIENTRUNG |
| NT | Nha Trang | CITY | MIENTRUNG |
| DLT | Đà Lạt | CITY | MIENNAM |
| PQ | Phú Quốc | CITY | MIENNAM |
| SGN | TP. Hồ Chí Minh | CITY | MIENNAM |
| HN | Hà Nội | CITY | MIENBAC |
| SAPA | Sa Pa | CITY | MIENBAC |
| BANAHILLS | Bà Nà Hills | ATTRACTION | DN |

`GET /api/v1/destinations` trả danh mục active; có thể lọc loại bằng `?type=CITY`. Tham số `destinationId` so khớp trực tiếp với bảng `tour_version_destinations` của phiên bản công bố. Quan hệ parent không được mở rộng tự động. Ví dụ `destinationId=DN` tìm Tour gắn DN, còn `destinationId=MIENTRUNG` cho kết quả rỗng trên seed vì chưa có phiên bản nào gắn trực tiếp mã vùng đó. SGN/HN trong `origin_destination_id` là điểm khởi hành, còn bộ lọc điểm đến dùng các liên kết riêng trong bảng trên. Xem [CatalogService](../../backend/src/main/java/vn/ptit/tour/service/CatalogService.java) và [SearchParamValidator](../../backend/src/main/java/vn/ptit/tour/service/SearchParamValidator.java).

## 3. Danh mục 14 Tour trong seed

Giá trong bảng là `adult_price` của chuyến được chọn khi tra cứu mặc định `availableOnly=true`, tại thời điểm vừa khởi tạo seed. Đơn vị VND/người lớn, `priceBasis=ADULT_PER_PERSON`. Cột trạng thái thể hiện Tour/phiên bản nội dung đang trỏ tới; T105 có bản 205 PENDING và chưa có `current_published_version_id`.

| Tour ID / mã | Tên | Phiên bản | Điểm đến gắn trực tiếp | Ngày/đêm | Trạng thái Tour/nội dung | Giá từ dự kiến | Departure / offer được chọn |
| --- | --- | --- | --- | --- | --- | ---: | --- |
| 101 / T101 | Đà Nẵng biển và ẩm thực | 201, v1 | DN, HOIAN | 3/2 | ACTIVE / PUBLISHED | 4.200.000 | 301 / 601 |
| 102 / T102 | Đà Nẵng nghỉ dưỡng cao cấp | 202, v1 | DN | 3/2 | ACTIVE / PUBLISHED | 6.000.000 | 321 / 621 |
| 103 / T103 | Đà Lạt săn mây | 203, v1 | DLT | 3/2 | ACTIVE / PUBLISHED | 3.500.000 | 331 / 631 |
| 104 / T104 | Đà Nẵng - Bà Nà bốn ngày | 204, v1 | DN, BANAHILLS | 4/3 | ACTIVE / PUBLISHED | 4.500.000 | 341 / 641 |
| 105 / T105 | Đà Nẵng giá sốc | 205, v1 | DN | 3/2 | ACTIVE / PENDING, chưa trỏ bản công bố | — | Chưa đủ điều kiện công khai |
| 106 / T106 | Hội An phố cổ và ẩm thực | 206, v1 | HOIAN | 3/2 | ACTIVE / PUBLISHED | 2.800.000 | 361 / 661 |
| 107 / T107 | Huế di sản | 207, v1 | HUE | 3/2 | ACTIVE / PUBLISHED | 3.200.000 | 371 / 671 |
| 108 / T108 | Phú Quốc biển xanh | 208, v1 | PQ | 4/3 | ACTIVE / PUBLISHED | 6.500.000 | 381 / 681 |
| 109 / T109 | Sa Pa chinh phục Fansipan | 209, v1 | SAPA | 3/2 | ACTIVE / PUBLISHED | 4.100.000 | 391 / 691 |
| 110 / T110 | Nha Trang lặn biển | 210, v1 | NT | 3/2 | ACTIVE / PUBLISHED | 4.400.000 | 401 / 701 |
| 111 / T111 | Hà Nội city tour | 211, v1 | HN | 2/1 | ACTIVE / PUBLISHED | 1.900.000 | 411 / 711 |
| 112 / T112 | Đà Nẵng - Hội An trọn gói | 212, v1 | DN, HOIAN | 3/2 | ACTIVE / PUBLISHED | 4.900.000 | 421 / 721 |
| 113 / T113 | Đà Nẵng biển và văn hóa | 213, v1 | DN | 3/2 | ACTIVE / PUBLISHED | 5.000.000 | 314 / 614 |
| 114 / T114 | Đà Nẵng bản cũ lưu trữ | 214, v1 | DN | 3/2 | ARCHIVED / PUBLISHED | — | Tour đã lưu trữ |

Tour 101 có thêm phiên bản **215, v2, DRAFT**, cùng thời lượng 3 ngày/2 đêm, điểm đến DN và tag BEACH, FOOD. Con trỏ công bố vẫn là **201**. Bản 201 chứa tag BEACH, FOOD, RELAXATION. Chi tiết `/api/v1/tours/101` lấy bản 201, kể cả khi người phụ trách thay đổi tag ở bản 215.

Tour 113 có chữ Hội An trong lịch trình JSON nhưng liên kết điểm đến của bản 213 chỉ có DN. Vì thế lọc `destinationId=HOIAN` trả T101, T106, T112 trên seed. Nội dung lịch trình và liên kết điểm đến là hai nguồn trường khác nhau trong dữ liệu hiện tại.

Seed có 15 dòng media, gồm ảnh của bản 215. Ảnh của 12 Tour công khai nằm trong [frontend/public/images/tours](../../frontend/public/images/tours). Hai bộ ảnh Tour 105 và 114 được giữ tại [demo-assets/private/tours](../../demo-assets/private/tours), còn các URL ảnh tương ứng trong seed được dùng để kiểm tra việc từ chối truy cập công khai. Phần lớn lịch trình/dịch vụ trong seed là mảng JSON rỗng; bản 201 và 213 có ví dụ chi tiết cho trang Tour.

## 4. Chuyến khởi hành, giá và mốc thời gian tương đối

Gọi **T0** là thời điểm thực thi seed. `CURRENT_TIMESTAMP + INTERVAL '17' DAY` tạo mốc khoảng T0 + 17 ngày. Mỗi lần dựng lại cơ sở dữ liệu, các ngày được tính lại theo thời điểm chạy. Chạy liên tục nhiều ngày có thể làm chuyến hết cửa sổ bán, nên cần khởi động lại trước buổi demo để lấy bộ dữ liệu mới.

Trong bảng, `mở…đóng` và ngày khởi hành dùng số ngày lệch so với T0. Các giá là giá người lớn ở **active offer** của chuyến. Bảng lưu seed đầy đủ, kể cả chuyến chưa đủ điều kiện để được chọn cho giá từ.

| Departure ID / mã | Tour version | Khởi hành | Mở…đóng bán | Sales status | Chỗ còn | Active offer / trạng thái | Giá VND |
| --- | --- | --- | --- | --- | ---: | --- | ---: |
| 301 / T101-D1 | 201 | +17 | −30…+14 | OPEN | 12 | 601 / APPROVED | 4.200.000 |
| 302 / T101-D2 | 201 | +24 | −30…+21 | OPEN | 20 | 603 / APPROVED | 4.600.000 |
| 303 / T101-D3 | 201 | +31 | −30…+28 | OPEN | 25 | 604 / APPROVED | 5.200.000 |
| 304 / T101-D4 | 201 | +40 | +10…+38 | CLOSED | 30 | 605 / APPROVED | 3.900.000 |
| 314 / T113-D1 | 213 | +20 | −20…+18 | OPEN | 9 | 614 / APPROVED | 5.000.000 |
| 321 / T102-D1 | 202 | +18 | −20…+16 | OPEN | 15 | 621 / APPROVED | 6.000.000 |
| 331 / T103-D1 | 203 | +15 | −25…+13 | OPEN | 18 | 631 / APPROVED | 3.500.000 |
| 341 / T104-D1 | 204 | +21 | −25…+19 | OPEN | 20 | 641 / APPROVED | 4.500.000 |
| 351 / T105-D1 | 205 | +19 | −10…+17 | OPEN | 22 | 651 / APPROVED | 3.800.000 |
| 361 / T106-D1 | 206 | +12 | −30…+10 | OPEN | 14 | 661 / APPROVED | 2.800.000 |
| 371 / T107-D1 | 207 | +16 | −30…+14 | OPEN | 21 | 671 / APPROVED | 3.200.000 |
| 381 / T108-D1 | 208 | +25 | −30…+23 | OPEN | 11 | 681 / APPROVED | 6.500.000 |
| 391 / T109-D1 | 209 | +22 | −30…+20 | OPEN | 17 | 691 / APPROVED | 4.100.000 |
| 401 / T110-D1 | 210 | +14 | −30…+12 | OPEN | 13 | 701 / APPROVED | 4.400.000 |
| 402 / T110-D2 | 210 | +28 | −30…+26 | OPEN | 0 | 702 / APPROVED | 3.900.000 |
| 411 / T111-D1 | 211 | +10 | −30…+8 | OPEN | 24 | 711 / APPROVED | 1.900.000 |
| 421 / T112-D1 | 212 | +19 | −30…+17 | OPEN | 16 | 721 / APPROVED | 4.900.000 |
| 431 / T114-D1 | 214 | +20 | −30…+18 | OPEN | 20 | 731 / APPROVED | 4.000.000 |
| 441 / T106-D2 | 206 | +60 | +30…+58 | OPEN | 30 | 741 / DRAFT | 2.700.000 |

Offer **602** là bản giá v2 của departure **301**, PENDING, giá 3.000.000 VND. Departure 301 vẫn trỏ active offer **601**, v1 APPROVED, giá 4.200.000 VND. Tổng 20 offer gồm 19 offer đang được con trỏ chuyến sử dụng và bản 602 đang chờ duyệt. Các active offer ở bảng trên đều v1.

Giá từ được chọn sau khi kiểm tra Tour ACTIVE, đúng bản công bố PUBLISHED, departure thuộc bản công bố, sales status OPEN, thời điểm truy vấn nằm trong `[booking_opens_at, booking_closes_at]`, active offer đúng chuyến và APPROVED. Khi `availableOnly=true`, điều kiện chỗ còn > 0 được áp dụng trước khi lấy chuyến rẻ nhất. Chuyến cùng Tour được ưu tiên theo giá tăng, thời điểm khởi hành tăng rồi departure ID tăng.

Các ca cần chỉ rõ khi trình bày:

- **T101** lấy departure 301/offer 601, giá 4.200.000. Offer 602 PENDING không thay giá công khai. Departure 304 có giá 3.900.000 nhưng CLOSED và cửa sổ bán chưa mở ở T0.
- **T110** với `availableOnly=true` lấy 401/701, giá 4.400.000 và 13 chỗ. Với `availableOnly=false`, lấy 402/702, giá 3.900.000 và 0 chỗ. Trang chi tiết luôn tính giá từ bằng điều kiện còn chỗ, đồng thời liệt kê các chuyến kèm `bookable`.
- **T106-D2**, departure 441/offer 741, vừa có cửa sổ bán ở tương lai vừa có giá DRAFT. Giữ seed này để minh họa bộ lọc nghiệp vụ.
- **T105** chưa có bản công bố dù departure 351 có giá APPROVED. **T114** đã ARCHIVED dù bản 214 mang PUBLISHED.

Đối chiếu danh sách chuyến và `bookable` trong [TourDetailService](../../backend/src/main/java/vn/ptit/tour/service/TourDetailService.java). Trường `bookable` phản ánh điều kiện của dữ liệu demo tại thời điểm gọi API; Module A hiện phục vụ tra cứu, gợi ý và xem thông tin chuyến.

## 5. Sở thích và ví dụ chấm điểm

| Interest ID | Mã | Nhãn | Active |
| --- | --- | --- | --- |
| 1 | BEACH | Biển | true |
| 2 | MOUNTAIN | Núi | true |
| 3 | CULTURE | Văn hóa | true |
| 4 | NATURE | Thiên nhiên | true |
| 5 | ADVENTURE | Khám phá | true |
| 6 | RELAXATION | Nghỉ dưỡng | true |
| 7 | FOOD | Ẩm thực | true |
| 8 | FAMILY | Gia đình | true |
| 9 | HISTORY | Lịch sử | true |
| 10 | CITY | Đô thị | true |
| 11 | FESTIVAL | Lễ hội | false |

`GET /api/v1/interests` trả 10 mục active. Danh mục nội bộ mà PRODUCT xem được có đủ 11 dòng. Bản 213 có tag FESTIVAL trong seed nhưng tag này bị loại khi tạo dữ liệu công khai và tính điểm. Gửi `interestCodes=FESTIVAL` làm đầu vào gợi ý nhận HTTP 422, `INVALID_INTEREST_CODE`.

| Version ID | Tag seed |
| --- | --- |
| 201 | BEACH, FOOD, RELAXATION |
| 202 | RELAXATION, BEACH |
| 203 | MOUNTAIN, NATURE |
| 204 | ADVENTURE, CITY |
| 205 | BEACH, FOOD |
| 206 | CULTURE, FOOD, HISTORY |
| 207 | HISTORY, CULTURE |
| 208 | BEACH, RELAXATION, FAMILY |
| 209 | MOUNTAIN, ADVENTURE, NATURE |
| 210 | BEACH, ADVENTURE |
| 211 | CITY, CULTURE, FOOD |
| 212 | BEACH, CULTURE, FOOD |
| 213 | BEACH, CULTURE, FESTIVAL; FESTIVAL đang tắt |
| 214 | BEACH |
| 215 | BEACH, FOOD |

[InterestScorer](../../backend/src/main/java/vn/ptit/tour/service/InterestScorer.java) tính `matchScore = round(100 × số mã trùng / số mã sở thích đã chọn, 2)`. Mã đầu vào được trim, chuyển uppercase, bỏ trùng và kiểm tra active. Tour đạt bộ lọc cứng nhưng không trùng sở thích vẫn được giữ với 0 điểm. Khi không chọn sở thích, `matchScore=null`, `meta.personalized=false`, `meta.sort=PRICE_ASC`.

Ví dụ chọn DN, 3 ngày, ngân sách tối đa 5.000.000 VND, BEACH và CULTURE:

| Thứ tự dự kiến | Tour | Mã trùng | Điểm | Giá từ VND | Giải thích |
| --- | --- | --- | ---: | ---: | --- |
| 1 | T112 / 112, version 212 | BEACH, CULTURE | 100 | 4.900.000 | Trùng 2/2 |
| 2 | T113 / 113, version 213 | BEACH, CULTURE | 100 | 5.000.000 | Trùng 2/2; giá cao hơn T112 |
| 3 | T101 / 101, version 201 | BEACH | 50 | 4.200.000 | Trùng 1/2 |

Sắp xếp toàn bộ tập kết quả theo điểm giảm, giá tăng rồi Tour ID dạng số tăng, sau đó mới phân trang. Điểm thể hiện tỷ lệ trùng nhãn đã chọn. T102 vượt ngân sách, T104 sai số ngày, T103 sai điểm đến, T105 chưa công bố và T114 đã lưu trữ nên bị loại trước bước tính điểm.

## 6. Kết quả truy vấn dự kiến để kiểm tra nhanh

Dùng backend của script demo tại `http://127.0.0.1:18080`. Nếu đã đổi cổng, lấy `backendUrl` từ `state.json` trong RuntimeRoot. Ví dụ PowerShell sau chỉ đọc API công khai:

```powershell
$api = 'http://127.0.0.1:18080/api/v1'
$page1 = Invoke-RestMethod "$api/tours?page=1&size=10"
$page2 = Invoke-RestMethod "$api/tours?page=2&size=10"
$page1.data.pagination
$page1.data.items | Select-Object tourId, tourVersionId, priceFrom, departureId
$page2.data.items | Select-Object tourId, tourVersionId, priceFrom, departureId
Invoke-RestMethod "$api/tour-recommendations?destinationId=DN&durationDays=3&maxBudget=5000000&interestCodes=BEACH&interestCodes=CULTURE"
```

| Truy vấn `GET /api/v1/...` | Kết quả dự kiến trên seed mới |
| --- | --- |
| `tours?page=1&size=10` | `totalItems=12`; Tour ID theo thứ tự 111, 106, 107, 103, 109, 101, 110, 104, 112, 113 |
| `tours?page=2&size=10` | Hai Tour ID 102, 108 |
| `tours?destinationId=DN&durationDays=3&maxBudget=5000000` | 101, 112, 113 theo giá tăng |
| `tour-recommendations?destinationId=DN&durationDays=3&maxBudget=5000000&interestCodes=BEACH&interestCodes=CULTURE` | 112 → 113 → 101; điểm 100 → 100 → 50; `meta.sort=RELEVANCE` |
| `tours?destinationId=HOIAN` | 106 → 101 → 112 theo giá tăng |
| `tours?destinationId=NT&availableOnly=true` | T110, giá từ `4400000`, departure `401` |
| `tours?destinationId=NT&availableOnly=false` | T110, giá từ `3900000`, departure `402`, chỗ còn 0 |
| `tour-recommendations?destinationId=DN&durationDays=3&interestCodes=MOUNTAIN` | 101 → 112 → 113 → 102; tất cả 0 điểm |
| `tours?destinationId=MIENTRUNG` | Danh sách rỗng, `totalItems=0` |
| `tours/101` | Version `201`, giá từ `4200000`, tag active BEACH/FOOD/RELAXATION |
| `tours/105` hoặc `tours/114` | HTTP 404, `TOUR_NOT_FOUND` |
| `tour-recommendations?interestCodes=FESTIVAL` | HTTP 422, `INVALID_INTEREST_CODE` |
| `tours?maxBudget=0` | HTTP 400, `INVALID_BUDGET` |
| `tours?size=50` | HTTP 400, `INVALID_PAGINATION`; size hợp lệ 1–10 |

12 Tour là tổng qua hai trang; trang đầu hiển thị tối đa 10. Các ID/giá trong JSON thường là chuỗi như `"101"`, `"4200000"`. Ngân sách là số nguyên VND/người lớn, mức tối đa được so sánh bao gồm dấu bằng. Các bộ lọc còn hỗ trợ keyword, durationDays, destinationId và availableOnly mặc định true. Keyword tìm trong tên/tóm tắt/mô tả Tour, mã Tour và tên điểm đến đã liên kết, với so khớp chữ thường và chuỗi có dấu hiện có trong dữ liệu.

## 7. Tài khoản, bản nháp và tác động của ca quản trị

| Username | Role | Thao tác demo trong Module A |
| --- | --- | --- |
| `khach01` | CUSTOMER | Đăng nhập, dùng các API công khai |
| `product01` | PRODUCT | Đọc tag phiên bản; thay tag DRAFT; tạo/cập nhật danh mục sở thích |
| `manager01` | MANAGER | Đọc tag phiên bản qua API nội bộ |
| `admin01` | ADMIN | Đăng nhập, dùng các API công khai; quyền nội bộ theo ma trận hiện tại |

Khách chưa đăng nhập truy cập danh mục, tra cứu, gợi ý và chi tiết. Ma trận API được cấu hình ở [SecurityConfig](../../backend/src/main/java/vn/ptit/tour/config/SecurityConfig.java). Đọc tag phiên bản cần PRODUCT hoặc MANAGER; ghi tag và truy cập danh mục sở thích nội bộ cần PRODUCT. Gọi API nội bộ khi chưa đăng nhập nhận 401; dùng role không được cấp cho endpoint nhận 403.

Mật khẩu do người chạy đặt bằng [Set-DemoPassword.ps1](../../scripts/demo/Set-DemoPassword.ps1), tối thiểu 12 ký tự. Trên Windows, script lưu PSCredential mã hóa DPAPI cho tài khoản Windows hiện tại ở `%LOCALAPPDATA%\TourDiscoveryDemo\credential.clixml`. [Start-Demo.ps1](../../scripts/demo/Start-Demo.ps1) đọc credential này và truyền `APP_DEMO_PASSWORD` vào backend `demo`. Các tài khoản được tạo dùng chung mật khẩu demo vừa đặt, BCrypt lưu hash trong DB. Khi chạy backend thủ công, cần chọn profile `demo` và cung cấp `APP_DEMO_PASSWORD` nếu muốn có bốn tài khoản. Chỉ đặt biến mật khẩu mà chưa chọn profile demo/test sẽ không kích hoạt DataInitializer.

Ca quản trị chuẩn thay tag ở **215 DRAFT** từ BEACH, FOOD sang BEACH, CULTURE. PUT `/api/v1/internal/tour-versions/215/interests` nhận body `{"interestCodes":["BEACH","CULTURE"]}` và ghi thay cả tập tag. Gọi PUT vào bản **201 PUBLISHED** bằng PRODUCT nhận 409 `VERSION_LOCKED`; MANAGER ghi bản 215 nhận 403. Đối chiếu [InterestAdminService](../../backend/src/main/java/vn/ptit/tour/service/InterestAdminService.java), [InternalInterestIntegrationTest](../../backend/src/test/java/vn/ptit/tour/controller/InternalInterestIntegrationTest.java) và [module-a-demo.spec.js](../../frontend/tests/module-a-demo.spec.js).

Sau ca thay tag, bản 215 giữ BEACH, CULTURE cho đến khi sửa lại hoặc dựng lại H2. Ca tạo danh mục của bộ kiểm thử frontend còn có thể để lại mã `DEMO_...` đã tắt. Tắt một sở thích đang có tag công khai làm thay đổi danh mục, điểm và lý do gợi ý. Bật FESTIVAL làm thay đổi tập tag active của bản 213. Vì vậy số dòng danh mục và kết quả ở mục 1, 5, 6 cần đối chiếu sau bước khôi phục seed.

## 8. Khôi phục bộ dữ liệu trước lần trình diễn tiếp theo

Từ thư mục gốc repository, với runtime và credential mặc định đã được tạo:

```powershell
.\scripts\demo\Stop-Demo.ps1
.\scripts\demo\Start-Demo.ps1 -SkipBuild
```

`-SkipBuild` dùng bản build được ghi trong state của lần chạy trước. Nếu vừa sửa SQL hoặc mã nguồn, chạy `Start-Demo.ps1` không có `-SkipBuild` để tạo build mới. Nếu dùng `-RuntimeRoot`, `-CredentialPath` hoặc cổng tùy chỉnh, truyền lại cùng giá trị tương ứng khi khởi động. Xem tham số trong [Start-Demo.ps1](../../scripts/demo/Start-Demo.ps1) và [Stop-Demo.ps1](../../scripts/demo/Stop-Demo.ps1).

Backend demo dùng H2 trong bộ nhớ. Khởi động backend mới chạy lại schema/data, xóa dữ liệu phiên trước, nạp lại các mốc tương đối, khôi phục tag bản 215 thành BEACH, FOOD và tạo lại bốn tài khoản theo credential. Các identity tiếp theo được đặt từ 1000 cho tours, versions, media, departures, offers; interest mới bắt đầu từ 100. Token đăng nhập cũ hết khả năng xác thực sau lần khởi động bằng script demo vì script sử dụng khóa JWT mới theo tiến trình. Đăng xuất rồi đăng nhập lại để thao tác quản trị.

Đọc lại hai trang ở mục 6 để xác nhận 12 Tour và thứ tự/giá seed. Có thể dùng [Test-DemoRestart.ps1](../../scripts/demo/Test-DemoRestart.ps1) để kiểm tra tự động việc đặt tag thử, restart, phục hồi tag, đăng nhập bốn role và từ chối token cũ. Script này chủ động khởi động lại demo đang chạy; lưu việc đang làm trước khi thực hiện.

Profile PostgreSQL mặc định `DB_INIT_MODE=never`, phục vụ cơ sở dữ liệu riêng đã được chuẩn bị. Khởi động lại PostgreSQL theo cấu hình đó giữ dữ liệu hiện hữu. Luồng khôi phục H2 ở trên áp dụng cho script demo local. Xem [hướng dẫn PostgreSQL](../../CHUYEN_SANG_POSTGRESQL.md) khi dùng môi trường riêng.
