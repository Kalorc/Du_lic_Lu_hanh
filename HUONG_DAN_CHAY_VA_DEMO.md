# Hướng dẫn cài đặt, chạy thử và kiểm tra Module A

Dành cho thành viên nhóm, người nhận mã nguồn và giảng viên tự chạy Module A từ repository. Kết quả mong đợi dùng [data.sql](backend/src/main/resources/data.sql) và [danh mục dữ liệu mẫu](docs/demo/DU_LIEU_MAU.md).

## 1. Chuẩn bị mã nguồn và công cụ

Chọn nhánh chứa phiên bản cần chạy. Ví dụ với repository gốc của nhóm:

~~~powershell
git clone --branch fix/module-a-qa https://github.com/Kalorc/Du_lic_Lu_hanh.git
cd Du_lic_Lu_hanh
git rev-parse HEAD
git status --short
~~~

Sau khi PR được merge, có thể chọn main. Repository trên GitHub cần chứa các tệp của bộ hướng dẫn này trước khi người nhận clone và làm theo.

Mở PowerShell 7 và kiểm tra:

~~~powershell
$PSVersionTable.PSVersion
java -version
mvn -version
node --version
npm --version
~~~

Cần JDK 21, Maven 3.9+ và Node.js 20+. Java home trong mvn -version phải trỏ tới JDK 21. Cần mạng để tải dependency và Chromium lần đầu.

Xác nhận đang ở thư mục gốc:

~~~powershell
Test-Path .\backend\pom.xml
Test-Path .\frontend\package-lock.json
Test-Path .\scripts\demo\Start-Demo.ps1
Test-Path .\demo-assets\credits-complete.json
~~~

Bốn lệnh cần trả True. Nếu dùng ZIP nguồn, giải nén rồi kiểm tra cùng danh sách.

## 2. Chạy demo bằng script trên Windows

### Tạo credential cục bộ

~~~powershell
$demoRuntime = Join-Path $env:LOCALAPPDATA 'TourDiscoveryDemo'
$demoCredential = Join-Path $demoRuntime 'credential.clixml'
& .\scripts\demo\Set-DemoPassword.ps1 -CredentialPath $demoCredential
~~~

Nhập mật khẩu tối thiểu 12 ký tự tại lời nhắc ẩn. Bốn tài khoản mẫu dùng mật khẩu này. File credential mã hóa bằng DPAPI cho tài khoản Windows/máy hiện tại. Tạo credential mới trên máy khác. Runtime đặt ngoài thư mục mã nguồn.

### Build và khởi động

~~~powershell
& .\scripts\demo\Start-Demo.ps1 -RuntimeRoot $demoRuntime -CredentialPath $demoCredential
~~~

Script sao chép backend, frontend và demo-assets vào runtime. Nó chạy mvn clean verify, npm ci, npm run build, khởi động các tiến trình và chờ URL sẵn sàng. Lần đầu có thể cần vài phút.

| Thành phần | Mặc định |
| --- | --- |
| Frontend | http://127.0.0.1:15173 |
| Backend | http://127.0.0.1:18080 |
| Database | H2 memory, seed mới mỗi lần backend khởi động |
| Profile | demo |
| Frontend server | Vite, proxy /api sang backend |

Cấu hình cổng khác nếu cần:

~~~powershell
& .\scripts\demo\Start-Demo.ps1 -RuntimeRoot $demoRuntime -CredentialPath $demoCredential -BackendPort 18081 -FrontendPort 15174
~~~

Đọc địa chỉ thật từ state sau khi script báo thành công:

~~~powershell
$demoState = Get-Content (Join-Path $demoRuntime 'state.json') -Raw | ConvertFrom-Json
$demoState.frontendUrl
$demoState.backendUrl
$api = $demoState.backendUrl + '/api/v1'
$list = Invoke-RestMethod "$api/tours?page=1&size=10"
$list.data.pagination
$list.data.items | Select-Object tourId, title, priceFrom, matchScore
~~~

Seed mới cho totalItems=12, totalPages=2, trang đầu 10 Tour, bắt đầu bởi Tour 111 giá 1.900.000 đồng. Mở địa chỉ frontend rồi vào /tours. Đăng nhập /admin bằng product01 và mật khẩu đã đặt.

## 3. Dừng, khởi động lại và reset dữ liệu

~~~powershell
& .\scripts\demo\Stop-Demo.ps1 -RuntimeRoot $demoRuntime
& .\scripts\demo\Start-Demo.ps1 -RuntimeRoot $demoRuntime -CredentialPath $demoCredential -SkipBuild
~~~

H2 memory nạp lại schema.sql và data.sql khi backend khởi động. DRAFT 215 trở lại BEACH,FOOD. Mục danh mục tạo trong buổi thực hành bị xóa theo vòng đời database. Đăng xuất/đăng nhập lại để lấy token từ JVM mới.

-SkipBuild dùng bản sao đã build tại buildRoot trong state.json. Sau khi sửa code, dừng rồi chạy Start-Demo với bước build đầy đủ để cập nhật bản chạy. Nếu dùng cổng tùy chỉnh, truyền lại cả BackendPort và FrontendPort khi khởi động lại.

Script kiểm tra restart thực hiện sửa tag DRAFT, dừng/chạy lại và kiểm tra đăng nhập, giá, thứ tự và seed:

~~~powershell
& .\scripts\demo\Test-DemoRestart.ps1 -RuntimeRoot $demoRuntime -CredentialPath $demoCredential
~~~

Kết quả ghi ở restart-verification.json trong runtime. Nên reset sau kiểm thử có thao tác ghi trước khi trình bày bảng seed.

## 4. Chạy thủ công bằng hai terminal

Dùng backend 18080 và frontend 15173. Dừng demo script nếu đang chiếm các cổng này.

### Terminal 1 — backend

Chạy từ thư mục gốc:

~~~powershell
$manualCredential = [PSCredential]::new('demo', (Read-Host 'Mat khau demo' -AsSecureString))
$savedDemoPassword = $env:APP_DEMO_PASSWORD
Push-Location backend
try {
    $env:APP_DEMO_PASSWORD = $manualCredential.GetNetworkCredential().Password
    mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
} finally {
    $env:APP_DEMO_PASSWORD = $savedDemoPassword
    $manualCredential = $null
    Pop-Location
}
~~~

Nhấn Ctrl+C để dừng. Profile demo tạo tài khoản mẫu khi có APP_DEMO_PASSWORD. Spring đọc biến của tiến trình. .env.example là mẫu tên biến cấu hình; nếu dùng file .env thì người chạy cần nạp biến vào môi trường trước.

### Terminal 2 — frontend

~~~powershell
cd frontend
npm ci
if ($LASTEXITCODE -ne 0) { throw 'npm ci failed.' }
$savedProxy = $env:VITE_PROXY_TARGET
try {
    $env:VITE_PROXY_TARGET = 'http://127.0.0.1:18080'
    npm run dev -- --host 127.0.0.1 --port 15173 --strictPort
} finally { $env:VITE_PROXY_TARGET = $savedProxy }
~~~

Nhấn Ctrl+C để dừng frontend. Khối này dùng biến proxy cho Vite. Giá trị biến được đọc khi khởi động server.

### Bash trên Linux/macOS

Đứng tại thư mục gốc. Terminal backend:

~~~bash
read -r -s -p "Mat khau demo: " APP_DEMO_PASSWORD
echo
export APP_DEMO_PASSWORD
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo
unset APP_DEMO_PASSWORD
~~~

Terminal frontend:

~~~bash
cd frontend
npm ci
VITE_PROXY_TARGET=http://127.0.0.1:18080 npm run dev -- --host 127.0.0.1 --port 15173 --strictPort
~~~

Các script DPAPI dành cho Windows. Trên Linux/macOS, dùng môi trường shell của hai terminal và khởi động lại backend để reset H2.

## 5. Chạy thử API

### Tra cứu và gợi ý

~~~powershell
$api = 'http://127.0.0.1:18080/api/v1'
$query = 'destinationId=DN&durationDays=3&maxBudget=5000000&interestCodes=BEACH&interestCodes=CULTURE&availableOnly=true&page=1&size=10'
$ranked = Invoke-RestMethod "$api/tour-recommendations?$query"
$ranked.data.items | Select-Object tourId, title, priceFrom, matchScore
$ranked.meta
~~~

Mong đợi Tour 112, 113, 101 với giá và điểm trong README. interestCodes được lặp lại cho từng mã. ID và tiền trong DTO là chuỗi. Thử /tours?destinationId=NT&availableOnly=true rồi đổi thành false để đối chiếu giá Tour 110 lần lượt 4.400.000 và 3.900.000 đồng. Chi tiết Tour đọc lại giá với điều kiện còn chỗ.

### Đăng nhập và cập nhật tag

~~~powershell
$apiCredential = [PSCredential]::new('product01', (Read-Host 'Mat khau demo' -AsSecureString))
$login = $null
$headers = $null
$loginBody = $null
try {
    $loginBody = @{ username='product01'; password=$apiCredential.GetNetworkCredential().Password } | ConvertTo-Json
    $login = Invoke-RestMethod "$api/auth/login" -Method Post -ContentType 'application/json' -Body $loginBody
    $headers = @{ Authorization = 'Bearer ' + $login.data.accessToken }
    $draft = Invoke-RestMethod "$api/internal/tour-versions/215/interests" -Headers $headers
    $draft.data
    $body = @{ interestCodes=@('BEACH','CULTURE') } | ConvertTo-Json
    $updated = Invoke-RestMethod "$api/internal/tour-versions/215/interests" -Method Put -Headers $headers -ContentType 'application/json' -Body $body
    $updated.data
} finally {
    $headers = $null
    $login = $null
    $loginBody = $null
    $apiCredential = $null
}
~~~

PUT lại BEACH,FOOD hoặc reset demo để phục hồi seed. PUT phiên bản 201 trả 409 VERSION_LOCKED; MANAGER ghi tag trả 403; thiếu JWT trả 401. PowerShell phát sinh exception khi nhận HTTP lỗi. Trong catch, đọc Exception.Response.StatusCode và ErrorDetails.Message để kiểm tra kết quả.

## 6. Kiểm thử và lưu kết quả

### Backend

~~~powershell
Push-Location backend
try {
    mvn clean verify
    if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed.' }
} finally { Pop-Location }
~~~

Đọc số tests/failures/errors/skipped tại backend/target/surefire-reports. Các lớp QA ghi JSON vào backend/target/module-a-evidence. ModuleAPerformanceIT là phép đo riêng, chạy trong backend bằng mvn '-Dtest=ModuleAPerformanceIT' test. Dữ liệu đo tổng hợp khác seed trình diễn. Chốt workload/cấu hình/ngưỡng TC38 trước khi kết luận nghiệm thu hiệu năng.

### Trình duyệt

Khởi động demo ở mục 2 trước. Khối này dùng credential Windows, lấy URL từ state và lưu kết quả ngoài mã nguồn:

~~~powershell
$demoRuntime = Join-Path $env:LOCALAPPDATA 'TourDiscoveryDemo'
$demoCredential = Join-Path $demoRuntime 'credential.clixml'
$demoState = Get-Content (Join-Path $demoRuntime 'state.json') -Raw | ConvertFrom-Json
$savedBaseUrl = $env:MODULE_A_BASE_URL
$savedApiUrl = $env:MODULE_A_ADMIN_API_URL
$savedEvidence = $env:MODULE_A_EVIDENCE_DIR
$savedBrowserPassword = $env:MODULE_A_DEMO_PASSWORD
Push-Location frontend
try {
    npm ci
    if ($LASTEXITCODE -ne 0) { throw 'npm ci failed.' }
    npx playwright install chromium
    if ($LASTEXITCODE -ne 0) { throw 'Chromium install failed.' }
    $env:MODULE_A_BASE_URL = $demoState.frontendUrl
    $env:MODULE_A_ADMIN_API_URL = $demoState.backendUrl + '/api/v1'
    $env:MODULE_A_EVIDENCE_DIR = Join-Path $demoRuntime ('browser-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
    $env:MODULE_A_DEMO_PASSWORD = (Import-Clixml $demoCredential).GetNetworkCredential().Password
    npm run test:browser
    if ($LASTEXITCODE -ne 0) { throw 'Browser tests failed; inspect the recorded results.' }
} finally {
    $env:MODULE_A_DEMO_PASSWORD = $savedBrowserPassword
    $env:MODULE_A_BASE_URL = $savedBaseUrl
    $env:MODULE_A_ADMIN_API_URL = $savedApiUrl
    $env:MODULE_A_EVIDENCE_DIR = $savedEvidence
    Pop-Location
}
~~~

Các test quản trị tạo/sửa dữ liệu. Reset demo sau khi chạy trước khi so sánh seed. Thư mục evidence có playwright-results.json, ảnh, trace và dữ liệu từng ca. Rà soát thông tin xác thực trước khi chia sẻ log/trace. Để chạy riêng diễn tập, thay lệnh cuối bằng npm run test:browser -- tests/module-a-demo.spec.js.

Với cách chạy thủ công, nhập mật khẩu đã dùng cho backend và cấp MODULE_A_DEMO_PASSWORD cho riêng tiến trình test thay vì đọc file DPAPI; cấu hình các URL theo terminal thực tế.

## 7. Log và xử lý lỗi

| Hiện tượng | Kiểm tra và cách xử lý |
| --- | --- |
| Thiếu Java/Maven/Node | Cài công cụ đúng phiên bản, mở lại terminal, kiểm tra PATH và mvn -version. |
| npm.ps1 bị chặn | Dùng npm.cmd/npx.cmd trong lệnh thủ công hoặc PowerShell được quản trị cho phép chạy script. |
| File .ps1 từ ZIP bị chặn | Đọc nội dung script, kiểm tra thuộc tính file và dùng Unblock-File với script đã tin cậy nếu chính sách máy cho phép. |
| Cổng đang dùng | Xem Get-NetTCPConnection -State Listen, chọn bộ cổng khác bằng tham số Start-Demo. |
| Backend chưa sẵn sàng | Đọc backend-build.log, backend.stdout.log và backend.stderr.log tại runtime. |
| Frontend lỗi API | Đối chiếu backendUrl trong state, VITE_PROXY_TARGET và cổng thật; chạy lại Vite sau khi đổi biến. |
| Đăng nhập lỗi | Xác nhận profile demo, credential và mật khẩu của lần chạy; dừng/chạy lại sau khi đổi credential. |
| 401 sau restart | Đăng xuất và đăng nhập lại. |
| Seed khác bảng | Reset H2, kiểm tra bộ lọc, tag active, availableOnly và cửa sổ nhận đặt theo thời gian khởi động. |
| Playwright thiếu Chromium | Chạy npx playwright install chromium theo lockfile. |
| Demo vẫn dùng mã cũ | Dừng và chạy lại với bước build đầy đủ; xem buildRoot trong state. |
| -SkipBuild chưa có bản build | Chạy Start-Demo lần đầu với bước build đầy đủ. |

Runtime có state.json, demo-backend.jar và thư mục build-... chứa bản nguồn đã dùng. Log cài dependency/build được ghi riêng với log stdout/stderr của backend/frontend. Stop-Demo đối chiếu PID và thời điểm tạo trước khi dừng tiến trình đã ghi.

## 8. Chuẩn bị trình bày và bàn giao

1. Chọn commit, cài dependency và chạy kiểm thử.
2. Khởi động seed mới, kiểm tra 12 Tour và thứ tự 112–113–101.
3. Kiểm tra PRODUCT/MANAGER và quyền trước buổi trình bày.
4. Reset dữ liệu sau ca có ghi, mở sẵn /tours.
5. Thực hiện [kịch bản](KICH_BAN_DEMO_TRUOC_GIANG_VIEN.md), ghi kết quả thực tế và giữ minh chứng cần nộp.

Ghi mã commit, ngày chạy, phiên bản công cụ, môi trường, dữ liệu và các lỗi còn mở cùng kết quả. Dùng cùng các bước cho người nhận tự kiểm tra.
