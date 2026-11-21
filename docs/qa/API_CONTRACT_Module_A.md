# Hợp đồng API Module A

Căn cứ: A-FR-01–10, A-BR-01–10 và A-ADR-01–04 được người yêu cầu xác nhận ngày
18/11/2026. Hợp đồng dùng cho QA, không phải biên bản UAT thương mại.

## Đường dẫn và quyền

Tất cả đường dẫn có tiền tố `/api/v1`.

| Method | Path | Quyền |
| --- | --- | --- |
| GET | /interests | Công khai, chỉ tag active |
| GET | /destinations | Công khai, chỉ điểm đến active; type tùy chọn |
| GET | /tours | Công khai |
| GET | /tour-recommendations | Công khai |
| GET | /tours/{tourId} | Công khai, Tour ACTIVE/current PUBLISHED |
| POST | /auth/login | Thông tin đăng nhập hợp lệ |
| GET | /auth/me | JWT hợp lệ |
| GET | /internal/tour-versions/{versionId}/interests | PRODUCT, MANAGER |
| PUT | /internal/tour-versions/{versionId}/interests | PRODUCT, version DRAFT |
| GET | /internal/interests | PRODUCT theo A-FR-09 |
| POST | /internal/interests | PRODUCT |
| PATCH | /internal/interests/{interestId} | PRODUCT |

## Tra cứu và gợi ý

Chỉ hỗ trợ keyword, destinationId, durationDays, maxBudget, interestCodes lặp
lại, availableOnly, page, size. Tên khác, gồm sort/minBudget, trả 400 INVALID_FILTER.

- keyword: trim, NFC, tối đa 100; không phân biệt hoa thường, phân biệt dấu;
  %, _ và ! được so khớp như văn bản.
- destinationId: mã active sau trim/uppercase; lạ/inactive trả 422 INVALID_DESTINATION.
- durationDays: số nguyên >=1. maxBudget: số nguyên VND >0.
- interestCodes: trim/uppercase, bỏ rỗng, loại trùng; tối đa 10 mã duy nhất.
  Lạ/inactive trả 422 INVALID_INTEREST_CODE.
- availableOnly mặc định true. page từ 1. size 1–10, mặc định 10.
  Trang sau trang cuối trả items rỗng, không lỗi 500.

Chỉ lấy Tour ACTIVE có current PUBLISHED thuộc chính Tour đó, chuyến OPEN
trong cửa sổ đặt và active offer APPROVED thuộc chính chuyến. availableOnly=true
loại chuyến hết chỗ trước chọn giá thấp nhất. Schema dùng active_offer_id,
không có valid_from/valid_to; không tuyên bố đã hỗ trợ schema hiệu lực giá đề xuất.

Áp hard filters, chọn một chuyến rẻ nhất mỗi Tour, lấy tag active của published
version, tính điểm rồi sort toàn bộ tập trước phân trang. Điểm =
round(100*matchedCount/selectedUniqueCount,2). Không có sở thích: null score,
personalized=false. Sort có sở thích: điểm giảm, giá tăng, tourId số tăng;
không sở thích: giá tăng, tourId số tăng. Tour khớp 0 vẫn giữ nếu qua bộ lọc.

## JSON và thời điểm đọc

Envelope `{success,data,meta,error}`. Tra cứu: data.items/data.pagination
(page,size,totalItems,totalPages); meta.asOf ISO8601 có offset, sort, priceBasis,
availableOnly, appliedInterestCodes, personalized. ID bigint trong DTO là
chuỗi; tiền VND nguyên là chuỗi số. MatchScore là tỷ lệ trùng sở thích.

Chi tiết đọc lại giá/chuyến hiện hành ở một asOf trong request. Thiếu giá hợp
lệ thì priceFrom=null, không tạo 0đ giả. GET không tạo Booking hoặc trừ chỗ.

## Ghi sở thích và lỗi

PUT `{interestCodes:[...]}` thay tập tag trong một transaction. Bản không DRAFT
trả 409 VERSION_LOCKED. Mã lạ/inactive trả 422 INVALID_INTEREST_CODE; mã trùng
trong body PUT trả 422 DUPLICATE_INTEREST_CODE, khác chuẩn hóa query công khai.
Tag bị tắt giữ mapping cũ, không được gắn mới và không tính điểm; bật lại khôi
phục hiệu lực mapping đã giữ.

401 UNAUTHENTICATED, 403 FORBIDDEN, 404 TOUR_NOT_FOUND/VERSION_NOT_FOUND/
INTEREST_NOT_FOUND; 400 INVALID_FILTER/INVALID_BUDGET/INVALID_PAGINATION.
Lỗi 500 trả INTERNAL_ERROR chung cùng requestId/timestamp, không có SQL,
DB URL, stack trace hoặc token trong response. Minh chứng phải che thông tin xác thực.

TC38 chưa có ngưỡng nghiệm thu được xác nhận. Số đo baseline không tự tạo SLA.
