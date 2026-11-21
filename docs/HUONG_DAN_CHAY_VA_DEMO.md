# Hướng dẫn chạy và demo Module A

Ngày kiểm chứng: **21/11/2026**, Windows, UTC+07. Dùng [kịch bản 5–7 phút](KICH_BAN_DEMO_TRUOC_GIANG_VIEN.md). Bản demo này chỉ có tra cứu, gợi ý theo quy tắc và quản trị sở thích; không có đặt tour/thanh toán.

## 1. Môi trường đã kiểm chứng

| Thành phần | Phiên bản thực tế |
|---|---|
| Java | Eclipse Temurin 21.0.9+10 LTS |
| Maven | 3.9.12 |
| Node.js / npm | 20.17.0 / 10.8.2; package yêu cầu Node >=20 |
| Backend | Spring Boot 3.3.5, H2 2.2.224 |
| Frontend | React 18.3.1, Vite 6.4.6, React Router 7.18.5 |
| Browser test | Playwright 1.58.2, Chromium đã cài trên máy |
| Shell | PowerShell 7.6.9 trên Windows; `java`, `mvn`, `node`, `npm` có trong PATH |

Demo dùng **H2 memory**, không cần cài PostgreSQL hay chạy SQL bằng tay. `schema.sql` + `data.sql` dựng lại DB khi khởi động: 14 Tour, 15 phiên bản, 15 cover; 12 Tour công khai. Tour105 còn PENDING và Tour114 ARCHIVED vẫn bị ẩn. Ngày khởi hành/cửa sổ đặt được tính tương đối theo lúc khởi tạo. PostgreSQL là lựa chọn riêng, xem [hướng dẫn PostgreSQL](CHUYEN_SANG_POSTGRESQL.md); lệnh demo dưới đây không kết nối vào DB PostgreSQL hiện có.

## 2. Chuẩn bị mật khẩu và chạy lần đầu

Mở PowerShell tại repository. Các biến đường dẫn dưới đây chính là vị trí đã dùng kiểm chứng trên máy này:

```powershell
Set-Location 'E:\2.PTIT\QLDA\Du_lic_Lu_hanh'
$demoRuntime = 'E:\2.PTIT\QLDA\module-a-demo-runtime-20261121\local-demo'
$demoCredential = Join-Path $demoRuntime 'credential.clixml'
& '.\scripts\demo\Set-DemoPassword.ps1' -CredentialPath $demoCredential
& '.\scripts\demo\Start-Demo.ps1' -RuntimeRoot $demoRuntime -CredentialPath $demoCredential
```

Ở bước đặt mật khẩu, nhập **mật khẩu do bạn chọn, tối thiểu 12 ký tự** vào lời nhắc ẩn. Không có mật khẩu mặc định và không cần JWT secret. Script lưu credential bằng Windows DPAPI, chỉ tài khoản Windows hiện tại giải mã được; tệp nằm ngoài repository. Không chép credential hoặc thư mục runtime vào Git/bộ nộp bài.

`Start-Demo.ps1` tạo bản sao build mới trong runtime, rồi thực thi và kiểm tra exit code của:

1. `mvn clean verify` trong bản sao `backend`: chạy toàn bộ 83 bài backend, đóng gói JAR.
2. `npm ci` trong bản sao `frontend`: cài đúng package-lock.
3. `npm run build`: tạo `dist`.
4. Chạy JAR với profile **demo**, bind **127.0.0.1:18080**, DB H2 memory riêng; truyền mật khẩu qua biến môi trường của tiến trình.
5. Chạy Vite tại **127.0.0.1:15173**, proxy `/api` sang backend18080; chờ hai URL trả HTTP200.

Bản sao build giúp cài mới dependency mà không xóa `node_modules` của phiên phát triển đang chạy. Mỗi lần chạy không có `-SkipBuild` tạo một bản build riêng. Cần Internet ở lần cài dependency đầu tiên; ảnh Tour đã lưu local nên không gọi nguồn ảnh ngoài khi trình diễn. Hai cổng phải trống; script báo lỗi và giữ nguyên tiến trình khác nếu có xung đột. Không dừng các ứng dụng sẵn có ở 8080/5173.

Kết quả mong đợi: in `Website`, `Dang nhap`, `Logs/state`; không in mật khẩu/token.

| Địa chỉ | Kết quả |
|---|---|
| http://127.0.0.1:15173/tours | Danh sách Tour có ảnh, bộ lọc, trang1/2 |
| http://127.0.0.1:15173/admin | Đăng nhập nội bộ |
| http://127.0.0.1:18080/api/v1/tours?page=1&size=10 | JSON success, 10 items, totalItems12 |

`state.json` ghi PID/thời điểm khởi động, `buildRoot` và URL. Log ở runtime: `backend-build.log`, `npm-ci.log`, `frontend-build.log`, `backend.stdout.log`, `backend.stderr.log`, `frontend.stdout.log`, `frontend.stderr.log`. Profile demo tắt H2 Console. Các tiến trình chạy nền; không cần để mở hai cửa sổ terminal.

## 3. Đăng nhập và phân quyền

Mở `/admin`, nhập tài khoản, nhập mật khẩu đã đặt ở bước2, chọn **Đăng nhập**. Muốn chuyển vai trò: chọn **Đăng xuất**, rồi đăng nhập tài khoản khác.

| Tài khoản | Vai trò | Được phép | Không được phép trong Module A |
|---|---|---|---|
| Không cần tài khoản | GUEST | Tra cứu, lọc, gợi ý, chi tiết public | API nội bộ:401 |
| `product01` | PRODUCT | Xem/tạo/bật/tắt danh mục; xem tag; thay tag của DRAFT215 | Sửa tag PUBLISHED201:409 VERSION_LOCKED |
| `manager01` | MANAGER | Xem tag phiên bản215 hoặc201 | Không có nút Lưu và danh mục Product; PUT bị403 |
| `khach01` | CUSTOMER | Các chức năng công khai | Không có quyền nội bộ:403 |
| `admin01` | ADMIN | Đăng nhập, dùng chức năng công khai | Không tự có quyền PRODUCT/MANAGER; không có màn hình quản trị tài khoản trong Module A |

Chỉ profile **demo/test** mới có bean tạo tài khoản mẫu; mật khẩu không rỗng vẫn là điều kiện bắt buộc. Chỉ cấu hình `APP_DEMO_PASSWORD` ở profile mặc định/production không bật việc seed tài khoản. JUnit dùng fixture riêng trong test resources, không phải credential triển khai. JWT demo dùng khóa ngẫu nhiên theo lần khởi động, token cũ hết hiệu lực sau restart.

## 4. Dừng, khởi động lại và đặt lại

Từ repository, dùng lại `$demoRuntime` và `$demoCredential` ở bước2:

```powershell
& '.\scripts\demo\Stop-Demo.ps1' -RuntimeRoot $demoRuntime
& '.\scripts\demo\Start-Demo.ps1' -RuntimeRoot $demoRuntime -CredentialPath $demoCredential -SkipBuild
```

`-SkipBuild` chỉ dùng sau một lần build thành công; sử dụng lại `buildRoot` đã ghi. Stop chỉ dừng PID có đúng thời điểm khởi tạo và executable đã ghi, không dừng toàn bộ Java/Node. Muốn lấy thay đổi mã nguồn mới, bỏ `-SkipBuild`.

**H2 memory được tạo lại sau mỗi restart:** tag DRAFT215 trở về BEACH+FOOD; danh mục mẫu và số Tour trở về ban đầu. Đây là reset demo, không phải lưu dữ liệu lâu dài. Tài khoản được tạo lại với cùng vai trò và mật khẩu lấy từ credential đã lưu. Đã kiểm tra đăng nhập và dữ liệu sau restart. Đăng xuất phiên cũ trong trình duyệt rồi đăng nhập lại vì JWT cũ hết hiệu lực.

Đổi/quên mật khẩu: chạy lại `Set-DemoPassword.ps1` ở bước2, sau đó Stop và Start. Không sửa password hash bằng SQL. Cách reset này chỉ dành cho H2 demo cô lập; không dùng để reset DB PostgreSQL có dữ liệu cần giữ.

## 5. Chạy kiểm thử trên bản demo đang chạy

Các lệnh build/test backend và frontend đã được script thực thi trong bản sao sạch. Muốn chạy lại backend riêng: `mvn clean verify` trong `backend`; dùng build copy nếu muốn tách khỏi phiên đang phát triển.

Để chạy toàn bộ kiểm thử trình duyệt, đọc `buildRoot` từ state. Credential chỉ được truyền vào môi trường tiến trình kiểm thử, giữ nguyên cả khoảng trắng nếu mật khẩu có chúng; không tạo tệp plaintext:

```powershell
$demoState = Get-Content (Join-Path $demoRuntime 'state.json') -Raw | ConvertFrom-Json
$testLocationChanged = $false
try {
    $env:MODULE_A_DEMO_PASSWORD = (Import-Clixml -LiteralPath $demoCredential).GetNetworkCredential().Password
    $env:MODULE_A_BASE_URL = 'http://127.0.0.1:15173'
    $env:MODULE_A_ADMIN_API_URL = 'http://127.0.0.1:18080/api/v1'
    $env:MODULE_A_EVIDENCE_DIR = Join-Path $demoRuntime 'browser-evidence'
    Push-Location (Join-Path $demoState.buildRoot 'frontend')
    $testLocationChanged = $true
    npm run test:browser
    if ($LASTEXITCODE -ne 0) { throw 'Browser tests failed; inspect the evidence and fix before demo.' }
} finally {
    if ($testLocationChanged) { Pop-Location }
    Remove-Item Env:MODULE_A_DEMO_PASSWORD -ErrorAction SilentlyContinue
}
```

Trên máy hiện tại Chromium đã sẵn sàng. Nếu máy khác chưa có browser, Playwright sẽ báo yêu cầu cài browser tương ứng; đừng coi việc build frontend là đã chạy browser test.

Bộ trình duyệt gồm 17 ca hồi quy cũ và 9 ca mới: 4 kiểm tra ảnh/tên điểm đến; 5 ca kiểm tra bảo vệ ảnh/metadata nội bộ, toàn bộ ảnh/public detail, diễn tập9 bước, responsive chi tiết/nội bộ và tạo/tắt danh mục trên UI. Các ca ảnh thiếu/lỗi chỉ sửa fixture cover hoặc chặn request ảnh; không đổi Expected để che lỗi. Các test đăng nhập tắt trace/video, ảnh chụp che input mật khẩu.

## 6. Trước khi lên trình bày

1. Build, chạy kiểm thử và xem kết quả; kiểm tra cả hai URL readiness.
2. Restart bằng `-SkipBuild` để có dữ liệu mẫu sạch, rồi đăng xuất trình duyệt cũ.
3. Mở `/tours`, xác nhận 12 Tour, hai trang và ảnh tải thành công; mở `/admin` ở tab khác, chuẩn bị tài khoản PRODUCT/MANAGER cùng mật khẩu riêng.
4. Dùng kịch bản đã diễn tập: DN,3 ngày,5.000.000đ, BEACH+CULTURE →112/113/101; DRAFT215, PUBLISHED201.

Ảnh và giấy phép: [attribution công khai](frontend/public/images/credits.html) chỉ gồm12 Tour public. [Manifest đầy đủ14 Tour](demo-assets/credits-complete.json) và4 biến thể ảnh của105/114 nằm ngoài frontend public; không phục vụ ảnh hay metadata Tour chưa công khai cho khách. Tổng14 ảnh khác nhau,28 file WebP, khoảng3,75MB; bản card17–95KB, detail68–293KB. Ảnh chỉ minh họa điểm đến, không cam kết resort/dịch vụ ngoài nội dung Tour.

Kết quả/ảnh chụp/hash nguồn: [báo cáo kiểm chứng demo](docs/demo/DEMO_VERIFICATION_REPORT.md). Hồ sơ QA cũ giữ nguyên; TC38 vẫn chờ tiêu chí hiệu năng, không được chuyển thành PASS bởi thay đổi giao diện này.
