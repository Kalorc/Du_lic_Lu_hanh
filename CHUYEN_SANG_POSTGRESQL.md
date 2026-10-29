# Chuyển từ H2 sang PostgreSQL

H2 được chọn làm mặc định để chạy được ngay khi demo. Khi cần chạy thật trên
PostgreSQL, làm ba bước sau.

## 1. Thêm driver vào `backend/pom.xml`

```xml
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <scope>runtime</scope>
</dependency>
```

## 2. Đổi `backend/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tourdb
    driver-class-name: org.postgresql.Driver
    username: postgres
    password: your-password
  sql:
    init:
      mode: always          # đổi sang 'never' sau lần nạp đầu tiên
```

## 3. Sửa `schema.sql` cho đúng kiểu dữ liệu gốc của đặc tả

| Trong `schema.sql` (H2) | Đổi thành (PostgreSQL) |
|---|---|
| `BIGINT PRIMARY KEY AUTO_INCREMENT` | `BIGSERIAL PRIMARY KEY` |
| `TIMESTAMP WITH TIME ZONE` | `TIMESTAMPTZ` *(cùng nghĩa, viết ngắn hơn)* |
| `itinerary_json TEXT` và các cột `*_json` | `JSONB` |
| `CREATE INDEX ix_media_version …` | `CREATE UNIQUE INDEX uq_media_one_cover ON tour_media(tour_version_id) WHERE is_cover = TRUE;` |

Ràng buộc "tối đa một ảnh `is_cover` mỗi phiên bản" là partial unique index mà H2
không hỗ trợ, nên bản H2 kiểm tra ràng buộc này ở tầng Service. Trên PostgreSQL
thì nên để CSDL tự bảo đảm bằng index trên.

Nếu chuyển các cột `*_json` sang `JSONB`, cần thêm annotation cho entity
`TourVersion`:

```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "itinerary_json")
private String itineraryJson;
```

Phần còn lại của mã nguồn không cần sửa: toàn bộ SQL trong `TourSearchDao` dùng
cú pháp chuẩn (`ROW_NUMBER() OVER`, `EXISTS`, `COALESCE`) nên chạy được trên cả
hai hệ.
