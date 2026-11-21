# Kịch bản demo Module A trước giảng viên

Thời lượng gợi ý cho luồng chính là **6 phút 30 giây**, có thể dành 6–8 phút tùy tốc độ thao tác và câu hỏi. Tài liệu hướng dẫn tra cứu Tour, gợi ý theo tỷ lệ trùng sở thích, xem chi tiết và quản trị sở thích theo vai trò. Cột kết quả dưới đây là **mong đợi để đối chiếu khi chạy**. Điền quan sát và minh chứng sau khi thực hiện trên bản demo đang trình bày.

Đọc cùng [README](README.md), [hướng dẫn chạy và demo](HUONG_DAN_CHAY_VA_DEMO.md) và [dữ liệu mẫu](docs/demo/DU_LIEU_MAU.md). Các bước bám theo mã nguồn, seed và bộ kiểm thử `frontend/tests/module-a-demo.spec.js`, `module-a-browser.spec.js`, `module-a-admin.spec.js` hiện có. Kết quả chạy thực tế nằm trong báo cáo kiểm chứng tương ứng của từng lần chạy.

## 1. Chuẩn bị trước buổi trình bày

1. Chạy theo hướng dẫn, kiểm tra trạng thái build/test và hai URL. Khi dùng cấu hình trong tài liệu, website ở <http://127.0.0.1:15173/tours>, trang nội bộ ở <http://127.0.0.1:15173/admin>, API ở `http://127.0.0.1:18080/api/v1`. Nếu đổi cổng khi chạy script, dùng các URL trong `state.json` của runtime.
2. Sau các bài kiểm thử có sửa tag/danh mục, Stop rồi Start bằng `-SkipBuild` để lấy lại H2 seed sạch. `-SkipBuild` cần một lần build thành công trước đó. Đăng xuất phiên trình duyệt cũ sau restart.
3. Xác nhận trang đầu có 10 thẻ trong tổng 12 Tour công khai, có **Trang sau**, ảnh tải thành công và checkbox **Chỉ tour còn chỗ** đang bật. Seed có 14 Tour tổng cộng, Tour 105 và Tour 114 bị ẩn với khách.
4. Chuẩn bị mật khẩu riêng đã đặt bằng `Set-DemoPassword.ps1`. Cùng mật khẩu cấu hình demo dùng cho `product01`, `manager01`, `khach01`, `admin01`; profile **demo** tạo bốn tài khoản theo vai trò tương ứng. Mật khẩu được nhập qua lời nhắc ẩn, tối thiểu 12 ký tự, lưu credential DPAPI ngoài repository. Không đưa mật khẩu, JWT, nội dung credential hoặc header Authorization vào ảnh/bản trình chiếu.
5. Mở sẵn tab tra cứu và tab nội bộ. Để font trình duyệt ở mức dễ đọc; nếu phóng to, cuộn tới bộ lọc trước khi bắt đầu. Chuẩn bị phần kiểm tra API ở mục 5 nếu giảng viên yêu cầu xác minh mã HTTP.

Lời mở đầu gợi ý — “Em trình bày Module A, giúp khách tìm Tour theo điểm đến, số ngày, ngân sách và sở thích. Dữ liệu demo được dựng bằng H2 cục bộ, ảnh Tour được phục vụ từ ứng dụng. Phần nội bộ quản lý sở thích theo vai trò và trạng thái phiên bản.”

## 2. Luồng chính khoảng 6 phút 30 giây

Sau mỗi lần đổi điều kiện, chọn **Tìm tour** rồi đợi hết trạng thái **Đang tìm…**. Mã `tourId` dùng để đối chiếu API; trên giao diện nhận biết Tour qua tên trong bảng dưới.

| Bước và thời gian | Thao tác cụ thể | Lời trình bày gợi ý | Kết quả mong đợi / điểm cần kiểm tra |
|---|---|---|---|
| 1 · 0:00–0:35 | Mở `/tours` khi chưa đăng nhập. Để trống các điều kiện và chưa chọn sở thích. Chỉ ảnh, bộ lọc và tổng số. Có thể bấm **Trang sau** rồi **Trang trước**. | “Khách được tra cứu ngay. Khi chưa chọn sở thích, hệ thống sắp theo giá tăng dần.” | Tổng 12 Tour, trang đầu 10 và trang hai 2, mỗi Tour một thẻ. Nhãn **Giá tăng dần**; API có `personalized=false`, `matchScore=null`. |
| 2 · 0:35–1:05 | Nhập **Từ khóa** `Đà Nẵng`, chọn **Điểm đến** `Đà Nẵng`, bấm **Tìm tour**. | “Em áp điều kiện về nơi khách muốn đi trước.” | Kết quả thỏa từ khóa và điểm đến `DN`. Tìm kiếm có phân biệt dấu, không phân biệt hoa thường. |
| 3 · 1:05–1:45 | Giữ điều kiện vừa nhập. Điền **Số ngày** `3`, **Ngân sách tối đa (VNĐ)** `5000000`, giữ **Chỉ tour còn chỗ**, bấm **Tìm tour**. | “Ngân sách là giá cho một người lớn. Các điều kiện này xác định tập Tour hợp lệ trước khi tính điểm.” | Còn ba Tour theo giá tăng — **101**, **112**, **113**. Tour 102 giá 6.000.000 và Tour 104 thời lượng 4 ngày bị loại. Giá 5.000.000 đúng biên được nhận. |
| 4 · 1:45–2:35 | Chọn hai chip **Biển** và **Văn hóa**, bấm **Tìm tour**. Chỉ lần lượt ba kết quả. | “Hệ thống tính số sở thích trùng chia số sở thích khách chọn, rồi xếp điểm giảm dần. Cùng điểm thì giá thấp hơn đứng trước.” | **112** — “Đà Nẵng - Hội An trọn gói”, **100%**, **4.900.000đ**; **113** — “Đà Nẵng biển và văn hóa”, **100%**, **5.000.000đ**; **101** — “Đà Nẵng biển và ẩm thực”, **50%**, **4.200.000đ**. Nhãn **Độ trùng sở thích**. |
| 5 · 2:35–3:15 | Bấm tên Tour 112. Chỉ ảnh, điểm đến và giá. Chọn **Xem lịch khởi hành**. | “Chi tiết đọc thông tin của phiên bản đang công bố và giá của chuyến đủ điều kiện. Ảnh minh họa điểm đến của hành trình.” | URL `/tours/112`, sau khi chọn lịch có `#khoi-hanh`; giá từ 4.900.000đ; bảng **Chuyến khởi hành** có giá/chỗ/trạng thái. Nếu cần minh họa lịch trình đầy đủ hơn, mở thêm `/tours/101` ở phần hỏi đáp. |
| 6 · 3:15–3:50 | Mở `/admin`. Nhập `product01`, mật khẩu riêng và bấm **Đăng nhập**. | “Nhân viên sản phẩm được quản lý tag và danh mục sở thích.” | Hiện vai trò **PRODUCT**, khối **Gắn sở thích cho phiên bản Tour** và **Danh mục sở thích**. |
| 7 · 3:50–4:45 | Nhập **Mã phiên bản (tourVersionId)** `215`, chọn **Tải**. Xác nhận **DRAFT**. Đưa tập lựa chọn về đúng **Biển + Văn hóa** bằng cách bỏ **Ẩm thực** nếu đang chọn. Bấm **Lưu danh sách sở thích**. | “Em đang chỉnh phiên bản nháp của Tour 101. Thao tác lưu thay toàn bộ tập tag của phiên bản này.” | Thông báo lưu 2 sở thích cho phiên bản 215; tải lại215 vẫn có `BEACH,CULTURE`. Mở `/tours/101` ở tab khác để kiểm tra public vẫn là phiên bản 201 có `BEACH,FOOD,RELAXATION`. |
| 8 · 4:45–6:00 | Trong phiên PRODUCT, đổi mã sang `201`, chọn **Tải**. Chỉ **PUBLISHED**, chip/nút lưu bị khóa. **Đăng xuất**, đăng nhập `manager01`; tải `215`. | “Bản đã công bố được khóa. MANAGER được xem tag của phiên bản. Backend cũng kiểm quyền và trạng thái khi gọi API trực tiếp.” | PRODUCT xem201 nhưng không lưu được trên UI. MANAGER tải 215 được, không có nút **Lưu danh sách sở thích** và không có khối danh mục. Nếu kiểm trực tiếp, PRODUCT PUT 201 → ** 409 VERSION_LOCKED**, MANAGER PUT 215 → ** 403 FORBIDDEN**, API nội bộ không token → ** 401 UNAUTHENTICATED**. Dùng mục 5 khi cần thực hiện tại chỗ. |
| 9 · 6:00–6:30 | Đăng xuất, trở về `/tours`. | “Module hỗ trợ tra cứu, lọc điều kiện, xếp hạng theo sở thích, xem chi tiết và quản trị tag theo vai trò. Các tình huống giá, còn chỗ và lỗi đầu vào có thể kiểm tra thêm bằng dữ liệu mẫu.” | Danh sách công khai tiếp tục tải được. Kết thúc ở trang khách, sẵn sàng cho hỏi đáp hoặc mục 3–5. |

Giải thích điểm — với `S={BEACH,CULTURE}`, Tour 112 và113 trùng 2/2 nên đạt 100; Tour 101 trùng1/2 nên đạt 50. Giá chỉ phân định sau điểm. Đây là tỷ lệ trùng sở thích. Một Tour đạt 0 vẫn được giữ khi thỏa các điều kiện lọc.

## 3. Kịch bản bổ sung khi giảng viên hỏi sâu

### A. Bản giá đang chờ duyệt và giá được công khai

1. Trở về tra cứu, chọn **Xóa lọc**. Nhập từ khóa `T101`, bấm **Tìm tour**. Mã Tour cũng được tìm theo từ khóa.
2. Đối chiếu giá từ **4.200.000đ**, mở `/tours/101`, xem chuyến **T101-D1** cùng giá người lớn **4.200.000đ**.
3. Mở tệp `backend/src/main/resources/data.sql` tại phần **PHIEN BAN GIA**. Offer601 thuộc chuyến 301 là `APPROVED`, giá 4.200.000; offer 602 cùng chuyến là `PENDING`, giá 3.000.000. `active_offer_id` của chuyến 301 trỏ601.
4. Giải thích “Chuyến này đang dùng bản giá đã duyệt. Bản3 triệu còn chờ duyệt nên giá công khai là 4,2 triệu.” Bản chờ duyệt được đối chiếu trong seed/API test; giao diện khách chỉ hiển thị giá được công khai.
5. Kiểm tra thêm từ khóa `T101`, `maxBudget=4000000`, còn chỗ bật → không có kết quả. ChuyếnT101-D4 giá 3,9 triệu đang `CLOSED`, cũng bị loại.

### B. Chọn còn chỗ trước khi lấy giá thấp nhất

1. Chọn **Xóa lọc**, nhập `T110`, giữ **Chỉ tour còn chỗ**, bấm **Tìm tour**.
2. Kỳ vọng Tour **Nha Trang lặn biển** giá từ **4.400.000đ**. Chuyến 401 còn 13 chỗ, giá 4,4 triệu; chuyến 402 hết chỗ, giá 3,9 triệu.
3. Bỏ chọn **Chỉ tour còn chỗ**, bấm **Tìm tour**. Giá từ chuyển thành **3.900.000đ**; kết quả đại diện cho phép cả chuyến hết chỗ trong tra cứu này.
4. Mở `/tours/110`. Giá từ chi tiết dùng chuyến còn chỗ nên là **4.400.000đ**; bảng lịch vẫn cho thấy chuyến 3,9 triệu còn 0 chỗ và **Không bán được**. Đối chiếu đúng điều kiện của mỗi API.
5. Chọn **Xóa lọc** hoặc bật lại checkbox trước khi quay về luồng chính.

### C. Tour chưa công bố hoặc đã lưu trữ

1. Mở `/tours/105` và `/tours/114` trong tab khách.
2. Kỳ vọng lỗi `TOUR_NOT_FOUND`, API trả 404. Tour 105 có nội dung `PENDING`; Tour 114 có vòng đời `ARCHIVED`.
3. Danh sách public đủ12 Tour, không có105/114. Bộ kiểm thử demo còn kiểm việc bảo vệ ảnh và metadata của hai Tour này.
4. Quay lại `/tours`. Khóa công khai áp ở backend, không phụ thuộc việc có đặt link trên giao diện.

### D. PRODUCT tạo, tắt và bật danh mục sở thích

1. Đăng nhập `product01`, tới khối **Danh mục sở thích**. Ở **Thêm sở thích mới**, nhập mã duy nhất, ví dụ `DEMO_20261121_A`; tên `Trải nghiệm demo`; mô tả `Dữ liệu tổng hợp cho buổi trình bày`; thứ tự `99`. Nếu mã đã tồn tại, đổi hậu tố.
2. Chọn **Tạo sở thích**. Kỳ vọng POST 201, thông báo thành công và dòng mới **Đang bật**.
3. Mở mới hoặc tải lại trang `/tours` ở tab khác. Danh mục công khai được nạp khi vào trang nên chip **Trải nghiệm demo** xuất hiện sau khi nạp lại.
4. Trong dòng vừa tạo, chọn **Tắt**. Kỳ vọng PATCH 200, dòng thành **Đã tắt**, nút đổi thành **Bật**. Tải lại `/tours`; chip vừa tắt biến mất.
5. Chọn **Bật** để kiểm chiều ngược lại nếu cần; sau đó tắt lại mục vừa tạo. Mục vẫn có trong danh mục quản trị để lưu lịch sử.
6. UI hiện có thao tác **tạo, tắt, bật**. API PATCH còn hỗ trợ đổi tên, mô tả, thứ tự và trạng thái. Module hiện không cung cấp nút/API DELETE; khi được hỏi về xóa, trình bày cơ chế tắt. Restart H2 sạch sẽ loại dữ liệu bổ sung do buổi demo tạo.

Khi tắt một sở thích đã có mapping, mapping lịch sử được giữ; danh mục public và chấm điểm loại sở thích đó. API từ chối gắn mới mã inactive. Trên bản nháp, thẻ lịch sử hiện thêm **(đã tắt)**; người dùng phải bỏ chọn thẻ đã tắt trước khi thay danh sách. Bật lại mục khôi phục hiệu lực mapping đã giữ. Dùng sở thích mới tạo để kiểm thao tác thông thường; tắt `BEACH` hoặc `CULTURE` sẽ làm thay đổi kết quả112/113/101 của luồng chính và cần khôi phục/reset.

### E. Danh sách rỗng, thử lại và đáp ứng màn hình

1. **Rỗng** — Xóa lọc, nhập `ZZ_DEMO_KHONG_CO_TOUR`, bấm **Tìm tour**. Kỳ vọng tổng0 và **Không có tour nào đạt điều kiện**. Bấm **Xóa lọc** để trở về 12 Tour.
2. **Retry lỗi mạng** — Ở trang khách đã tải, mở DevTools → Network → chọn **Offline**; nhập `T101`, bấm **Tìm tour**. Chờ thông báo lỗi và **Thử lại**. Chuyển Network về **No throttling/Online**, giữ nguyên bộ lọc đã áp dụng, bấm **Thử lại**. Kỳ vọng gửi request mới và khôi phục Tour 101. Đưa DevTools về Online ngay sau thao tác. Nếu trang có sẵn Service Worker/extension can thiệp mạng, dùng bộ kiểm thử TC32 để kiểm có kiểm soát.
3. **Bộ lọc chưa áp dụng** — Sau lỗiT101, có thể sửa ô từ khóa thànhT110 rồi bấm **Thử lại**. Kỳ vọng retry bộ lọcT101 đã áp dụng. Chỉ khi bấm **Tìm tour**, T110 mới trở thành bộ lọc áp dụng.
4. **Responsive** — Dùng Device Toolbar lần lượt 360×900,768×900,1440×900. Kiểm `/tours`, cuộn tới thẻ và nút phân trang; `/tours/101`, cuộn bảng lịch; `/admin`, đăng nhập PRODUCT và tải 215. Kiểm thao tác được, thẻ/nút không bị bộ lọc che, toàn trang không cuộn ngang; bảng có thể cuộn ngang trong vùng riêng.
5. **Bàn phím** — Dùng Tab tới từ khóa, bộ lọc, chip; Enter chọn chip, quan sát trạng thái được chọn; Tab tới **Tìm tour**, Enter để gửi. Focus nhìn thấy và thao tác hoàn thành.
6. **Phản hồi đến muộn** — Các ca TC31 trong `module-a-browser.spec.js` trì hoãn request rồi reset/đổi điều kiện, kiểm phản hồi cũ không thay kết quả/lỗi/loading của request mới. Chạy các ca này theo hướng dẫn kiểm thử khi cần kiểm chứng tình huống cạnh tranh; một lần bấm nhanh bằng tay chưa đủ chốt kết quả.

Thông báo mạng ở API client hiện nhắc cổng8080 của cấu hình phát triển. Khi chạy demo18080/15173, dùng URL thực từ `state.json` và kiểm cổng18080.

## 4. Phiếu đối chiếu cho người kiểm tra

Ghi thời điểm, commit/buildRoot, URL, trình duyệt, người kiểm tra và đường dẫn ảnh/log trước khi điền cột cuối. Đánh dấu Đạt/Không đạt/Chưa chạy theo kết quả quan sát. Các hàng dưới mô tả kỳ vọng, không xác nhận kết quả của một lần chạy mới.

| Mã | Thao tác hoặc dữ liệu vào | Kết quả mong đợi | Quan sát / minh chứng |
|---|---|---|---|
| D01 | Khách `/tours`, seed sạch, không sở thích | 12 public; trang 1 có10, trang 2 có2; giá tăng; score null | Chưa chạy |
| D02 | DN,3 ngày,max 5.000.000,còn chỗ, không sở thích | 101 →112 →113; giá 4,2 →4,9 →5 triệu | Chưa chạy |
| D03 | D02 + BEACH,CULTURE | 112 / 100 → 113 / 100 → 101 / 50; giá 4,9 →5 →4,2 triệu | Chưa chạy |
| D04 | Điểm đến Sa Pa, chỉ chọn Biển | Tour 109 vẫn có trong kết quả, score 0 | Chưa chạy |
| D05 | D03, số Tour mỗi trang 1, lần lượt sang trang | 112 →113 →101; sort trên tập đầy đủ trước phân trang | Chưa chạy |
| D06 | Xem `/tours/112`, chọn lịch | Giá4,9 triệu; ảnh tải; neo `#khoi-hanh`, bảng lịch hiển thị | Chưa chạy |
| D07 | `/tours/101`, đối chiếu seed offer 601/602 | Giá4,2 triệu; PENDING3 triệu không thành giá public | Chưa chạy |
| D08 | T101,max 4 triệu,còn chỗ | Kết quả rỗng; chuyếnCLOSED3,9 triệu bị loại | Chưa chạy |
| D09 | T110,còn chỗ true rồi false | Giá4,4 triệu rồi3,9 triệu; detail vẫn lấy giá chuyến còn chỗ4,4 triệu | Chưa chạy |
| D10 | GET detail105 và114 | 404 `TOUR_NOT_FOUND`, không lộ chi tiết public | Chưa chạy |
| D11 | PRODUCT tải 215, lưu BEACH+CULTURE, tải lại | PUT 200, DRAFT lưu đúng hai tag | Chưa chạy |
| D12 | Sau D11 xem Tour 101 | Public đọc201, tag BEACH/FOOD/RELAXATION | Chưa chạy |
| D13 | PRODUCT tải 201 rồi PUT trực tiếp | UI khóa; API409 `VERSION_LOCKED` | Chưa chạy |
| D14 | MANAGER xem215; PUT 215 và GET catalog nội bộ | GET tag200; UI chỉ xem; PUT 403; catalog403 | Chưa chạy |
| D15 | Không token gọi GET tag215 | 401 `UNAUTHENTICATED` | Chưa chạy |
| D16 | CUSTOMER hoặc ADMIN đăng nhập rồi vào nội bộ | Thông báo không có quyền nội bộ; API tag/catalog403 | Chưa chạy |
| D17 | PRODUCT tạo mã mới, tắt/bật | POST 201, PATCH 200; public catalog thay đổi sau tải lại | Chưa chạy |
| D18 | POST lại mã đã tồn tại | 422 `DUPLICATE_INTEREST_CODE` | Chưa chạy |
| D19 | GET tours?maxBudget=0 | 400 `INVALID_BUDGET`; UI min1 có thể chặn trước khi gửi | Chưa chạy |
| D20 | GET tours?destinationId=NO_SUCH_DEST | 422 `INVALID_DESTINATION`; dùng API do UI chỉ cho chọn danh mục | Chưa chạy |
| D21 | GET recommendations với UNKNOWN_DEMO hoặc FESTIVAL | 422 `INVALID_INTEREST_CODE`; FESTIVAL inactive | Chưa chạy |
| D22 | GET tours?page=0 hoặc size=11 | 400 `INVALID_PAGINATION` | Chưa chạy |
| D23 | GET tours?minBudget=1 hoặc sort=PRICE_ASC | 400 `INVALID_FILTER`, tham số chưa được hỗ trợ | Chưa chạy |
| D24 | PRODUCT PUT 215 với BEACH,UNKNOWN_DEMO | 422 `INVALID_INTEREST_CODE`, tập tag trước đó giữ nguyên | Chưa chạy |
| D25 | PRODUCT PUT 215 với BEACH,BEACH | 422 `DUPLICATE_INTEREST_CODE` | Chưa chạy |
| D26 | GET recommendations?interestCodes=BEACH&interestCodes=BEACH | Chuẩn hóa một mã, điểm không đếm hai lần | Chưa chạy |
| D27 | Từ khóa toàn khoảng trắng; chữ hoa/thường có dấu | Khoảng trắng như bỏ từ khóa; NFC và hoa/thường nhất quán, có phân biệt dấu | Chưa chạy |
| D28 | Từ khóa không khớp rồi Xóa lọc | Empty state rồi trở về 12 Tour | Chưa chạy |
| D29 | Offline rồi Online, bấm Thử lại | Request mới với bộ lọc đã áp dụng, khôi phục kết quả | Chưa chạy |
| D30 | TC31 phản hồi cũ sau reset/đổi điều kiện | Chỉ request mới cập nhật kết quả/error/loading | Chưa chạy |
| D31 |360/768/1440 px, bàn phím và bảng chi tiết/nội bộ | Thao tác chính sử dụng được, focus rõ, kết quả không bị che | Chưa chạy |
| D32 | Stop/Start -SkipBuild rồi đăng nhập lại | Seed và tài khoản tạo lại;215 về BEACH+FOOD; mục tạo thêm biến mất | Chưa chạy |

Đối chiếu mã lỗi theo [README](README.md) và source hiện tại. Số ca PASS trong báo cáo kiểm thử, lỗi còn mở và tiêu chí hiệu năng cần lấy từ báo cáo của đúng build; trình diễn UI chưa xác nhận SLA tải hoặc nghiệm thu thương mại.

## 5. Kiểm tra API trực tiếp khi cần xác minh

Dùng PowerShell7. Đọc mật khẩu từ credential do chính người chạy tạo, giữ token trong biến của terminal riêng. Các lệnh chọn ra dữ liệu nghiệp vụ hoặc HTTP/code. Khi chia sẻ màn hình, chỉ chiếu bảng kết quả, tránh tab request đăng nhập và nội dung biến xác thực.

Các khối lệnh dưới dùng runtime mặc định. Nếu đã dùng runtime khác, thay giá trị demoRuntime trong từng khối. Chạy khối dừng/reset tại thư mục gốc repository. Khi đổi cổng, dùng backendUrl và frontendUrl được lưu trong state.

### 5.1. Khách, xếp hạng và lỗi đầu vào

```powershell
$demoRuntime = Join-Path $env:LOCALAPPDATA 'TourDiscoveryDemo'
$demoState = Get-Content (Join-Path $demoRuntime 'state.json') -Raw | ConvertFrom-Json
$demoApi = $demoState.backendUrl + '/api/v1'
$rankQuery = '/tour-recommendations?destinationId=DN&durationDays=3&maxBudget=5000000&interestCodes=BEACH&interestCodes=CULTURE&availableOnly=true&page=1&size=10'
$rankResult = Invoke-RestMethod -Uri ($demoApi + $rankQuery)
$rankResult.data.items | Select-Object tourId, title, matchScore, priceFrom

$checks = @(
    '/tours/105',
    '/tours/114',
    '/tours?maxBudget=0',
    '/tours?destinationId=NO_SUCH_DEST',
    '/tour-recommendations?interestCodes=UNKNOWN_DEMO',
    '/tour-recommendations?interestCodes=FESTIVAL',
    '/tours?page=0',
    '/tours?size=11',
    '/tours?sort=PRICE_ASC',
    '/internal/tour-versions/215/interests'
)
foreach ($checkPath in $checks) {
    $checkResponse = Invoke-WebRequest -Uri ($demoApi + $checkPath) -SkipHttpErrorCheck
    $checkPayload = $checkResponse.Content | ConvertFrom-Json
    [pscustomobject]@{
        Request = $checkPath
        HTTP = [int]$checkResponse.StatusCode
        Code = $checkPayload.error.code
    }
}
```

Bảng xếp hạng mong đợi là 112 / 100 / 4900000, 113 / 100 / 5000000, 101 / 50 / 4200000. Mã HTTP/code theo D10, D15, D19–D23. API tiền và ID trả chuỗi JSON để giữ giá trị chính xác.

### 5.2. PRODUCT sửa bản nháp và kiểm khóa phiên bản

Thay `$demoRuntime` bằng runtime đang chạy. Credential phải được tạo bằng cùng tài khoản Windows. Khối dưới đưa215 về BEACH+CULTURE; ghi lại tập ban đầu trước khi chạy nếu muốn giữ một trạng thái demo khác.

```powershell
$demoRuntime = Join-Path $env:LOCALAPPDATA 'TourDiscoveryDemo'
$demoCredential = Join-Path $demoRuntime 'credential.clixml'
$demoState = Get-Content (Join-Path $demoRuntime 'state.json') -Raw | ConvertFrom-Json
$demoApi = $demoState.backendUrl + '/api/v1'
$productSession = $null
$productHeaders = $null
$loginBody = $null
try {
    $loginBody = @{
        username = 'product01'
        password = (Import-Clixml -LiteralPath $demoCredential).GetNetworkCredential().Password
    } | ConvertTo-Json
    $productSession = Invoke-RestMethod -Method Post -Uri ($demoApi + '/auth/login') `
        -ContentType 'application/json' -Body $loginBody
    $loginBody = $null
    $productHeaders = @{ Authorization = 'Bearer ' + $productSession.data.accessToken }

    $draftResult = Invoke-RestMethod -Method Put `
        -Uri ($demoApi + '/internal/tour-versions/215/interests') `
        -Headers $productHeaders -ContentType 'application/json' `
        -Body '{"interestCodes":["BEACH","CULTURE"]}'
    $draftResult.data | Select-Object tourVersionId, contentStatus
    $draftResult.data.interests | Select-Object code, name

    $lockedResponse = Invoke-WebRequest -Method Put `
        -Uri ($demoApi + '/internal/tour-versions/201/interests') `
        -Headers $productHeaders -ContentType 'application/json' `
        -Body '{"interestCodes":["BEACH"]}' -SkipHttpErrorCheck
    [pscustomobject]@{
        Operation = 'PRODUCT PUT201'
        HTTP = [int]$lockedResponse.StatusCode
        Code = ($lockedResponse.Content | ConvertFrom-Json).error.code
    }

    $publicTour = Invoke-RestMethod -Uri ($demoApi + '/tours/101')
    $publicTour.data | Select-Object tourId, tourVersionId, priceFrom
    $publicTour.data.interests | Select-Object code
} finally {
    $loginBody = $null
    $productHeaders = $null
    $productSession = $null
}
```

Kỳ vọng215 DRAFT lưu thành công; PUT 201 nhận 409 `VERSION_LOCKED`; public Tour 101 vẫn đọc201 và ba tag ban đầu. Có thể thay JSON ở lệnh PUT 215 để kiểm D24/D25 rồi GET 215 lại xác nhận dữ liệu giữ nguyên.

### 5.3. MANAGER chỉ đọc

```powershell
$demoRuntime = Join-Path $env:LOCALAPPDATA 'TourDiscoveryDemo'
$demoCredential = Join-Path $demoRuntime 'credential.clixml'
$demoState = Get-Content (Join-Path $demoRuntime 'state.json') -Raw | ConvertFrom-Json
$demoApi = $demoState.backendUrl + '/api/v1'
$managerSession = $null
$managerHeaders = $null
$loginBody = $null
try {
    $loginBody = @{
        username = 'manager01'
        password = (Import-Clixml -LiteralPath $demoCredential).GetNetworkCredential().Password
    } | ConvertTo-Json
    $managerSession = Invoke-RestMethod -Method Post -Uri ($demoApi + '/auth/login') `
        -ContentType 'application/json' -Body $loginBody
    $loginBody = $null
    $managerHeaders = @{ Authorization = 'Bearer ' + $managerSession.data.accessToken }
    $readResult = Invoke-RestMethod -Uri ($demoApi + '/internal/tour-versions/215/interests') `
        -Headers $managerHeaders
    $readResult.data | Select-Object tourVersionId, contentStatus

    $forbiddenResponse = Invoke-WebRequest -Method Put `
        -Uri ($demoApi + '/internal/tour-versions/215/interests') `
        -Headers $managerHeaders -ContentType 'application/json' `
        -Body '{"interestCodes":["BEACH"]}' -SkipHttpErrorCheck
    [pscustomobject]@{
        Operation = 'MANAGER PUT215'
        HTTP = [int]$forbiddenResponse.StatusCode
        Code = ($forbiddenResponse.Content | ConvertFrom-Json).error.code
    }
} finally {
    $loginBody = $null
    $managerHeaders = $null
    $managerSession = $null
}
```

Kỳ vọng GET 200 và PUT 403 `FORBIDDEN`; dùng lệnh không header ở5.1 để kiểm 401. `admin01` và `khach01` đăng nhập được nhưng API nội bộ trả 403. Muốn kiểm hai vai trò này, đăng nhập lại bằng tài khoản tương ứng và dùng GET nội bộ, đối chiếu D16.

## 6. Kết thúc, khôi phục và xử lý gián đoạn

Khi kết thúc, đăng xuất các phiên nội bộ. Nếu chỉ sửa tag215 và cần trả nhanh về seed, PRODUCT tải 215, chọn đúng **Biển + Ẩm thực**, lưu lại. Các mục tạo thêm có thể tắt. Để dựng lại toàn bộ dữ liệu ban đầu, dùng Stop/Start H2 demo.

```powershell
$demoRuntime = Join-Path $env:LOCALAPPDATA 'TourDiscoveryDemo'
$demoCredential = Join-Path $demoRuntime 'credential.clixml'
& '.\scripts\demo\Stop-Demo.ps1' -RuntimeRoot $demoRuntime
$demoState = Get-Content (Join-Path $demoRuntime 'state.json') -Raw | ConvertFrom-Json
& '.\scripts\demo\Start-Demo.ps1' -RuntimeRoot $demoRuntime -CredentialPath $demoCredential -BackendPort ([Uri]$demoState.backendUrl).Port -FrontendPort ([Uri]$demoState.frontendUrl).Port -SkipBuild
```

Sau khi Start báo sẵn sàng, đăng xuất phiên trình duyệt cũ rồi vào lại `/tours`. Kiểm12 Tour,215 trở về BEACH+FOOD, mục mới tạo biến mất, tài khoản đăng nhập bằng mật khẩu đã cấu hình. JWT cũ hết hiệu lực theo lần khởi động. Mỗi lần H2 được tạo lại, ngày chuyến/cửa sổ đặt được tính tương đối theo seed. Muốn demo mã nguồn vừa sửa, chạy Start qua bước build đầy đủ theo hướng dẫn.

| Tình huống | Cách tiếp tục |
|---|---|
| Cổng18080/15173 đang có tiến trình | Xem `state.json`, dùng Stop cho runtime do script tạo nếu đó là bản demo cũ; nếu là ứng dụng khác, chọn cổng khác qua tham số Start và cập nhật URL. |
| Thiếu credential hoặc đổi tài khoản Windows | Chạy `Set-DemoPassword.ps1` với đường dẫn ngoài repository, nhập lại mật khẩu riêng, rồi Start. |
| 401 sau restart | Đăng xuất rồi đăng nhập lại để lấy JWT mới. |
| Sai tổng số hoặc tag/điểm khác kịch bản | Kiểm seed sạch, bộ lọc hiện tại, trạng thái active của BEACH/CULTURE và đúng build/runtime; reset demo rồi chạy lại bước1. |
| Kết nối lỗi | Đưa DevTools về Online; kiểm hai URL, log runtime, cổng thực; dùng **Thử lại** với bộ lọc đã áp dụng. |
| Mã sở thích demo bị trùng | Dùng hậu tố mới hoặc reset H2 trước khi tạo lại. |
| Đang hỏi về Booking, thanh toán, AI dự đoán hoặc production | Chỉ phạm vi Module A ở đầu tài liệu và đối chiếu các phần mở rộng trong báo cáo dự án. Xác nhận nghiệm thu và hiệu năng theo hồ sơ của đúng môi trường/bản build. |

Nếu phát hiện lệch kết quả, ghi hàng kiểm tra, Actual, HTTP/code, thời điểm và ảnh/log vào phiếu. Khi quay lại buổi trình bày, ưu tiên những luồng đã kiểm tra thành công trên build đó và trình bày rõ vấn đề đang quan sát.
