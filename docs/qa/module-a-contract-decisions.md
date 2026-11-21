# Quyết định hợp đồng Module A

Ngày lập: 18/11/2026. Trạng thái: **A-ADR-01–04 đã được người yêu cầu chấp nhận trong cuộc trò chuyện ngày 18/11/2026. A-ADR-05 còn chờ ngưỡng và mức tải nghiệm thu.**

Căn cứ: SRS Module, Danh mục FR_NFR_BR module, Kế hoạch và chiến lược kiểm thử, test_case.csv và LP-03/LP-06/LP-07. Không thay đổi Expected gốc trong CSV. Các quy tắc đã mô tả rõ trong A-BR-05/06/07/08/09/10 được dùng làm căn cứ kiểm tra kỹ thuật; không suy diễn thành phê duyệt thương mại.

## Các quyết định và đề xuất

| Mã | Test | Đề xuất | Phương án khác và tác động |
| --- | --- | --- | --- |
| A-ADR-01 | TC06, TC15 | Giữ API chỉ có keyword, destinationId, durationDays, maxBudget, interestCodes, availableOnly, page, size. Từ chối query parameter không hỗ trợ bằng 400 INVALID_FILTER, gồm minBudget và sort. | Bổ sung bộ lọc ngân sách tối thiểu/sort sẽ mở rộng contract, DTO, DAO, giao diện và test. Không thực hiện khi chưa được xác nhận. |
| A-ADR-02 | TC25 | Tìm kiếm Unicode không phân biệt hoa thường, có phân biệt dấu. Chuẩn hóa NFC đầu vào. Không tự bỏ dấu tiếng Việt. | Tìm không dấu đòi hỏi quy tắc chuyển đổi bổ sung và chỉ mục phù hợp PostgreSQL. |
| A-ADR-03 | TC27 | destinationId không tồn tại/inactive trả 422 INVALID_DESTINATION. | 200 empty cũng được CSV cho phép, nhưng ít phản hồi về đầu vào không hợp lệ hơn. Cần chốt trước khi sửa vì code ban đầu trả 400. |
| A-ADR-04 | TC43 | Khi tắt tag: giữ mapping lịch sử; loại khỏi danh mục công khai và tính điểm; từ chối gắn mới bằng 422; bật lại khôi phục hiệu lực mapping cũ. | Xóa mapping cũ làm mất lịch sử và cần migration. |
| A-ADR-05 | TC38 | Đo H2 và PostgreSQL riêng (nếu có), 1.000/10.000 Tour tổng hợp, API recommendations với BEACH/FOOD, size=10, concurrency=1, 20 warmup và 100 mẫu mỗi dataset; lưu p95, lỗi và EXPLAIN. Chỉ đánh giá đạt khi ngưỡng p95 cùng workload/môi trường được xác nhận. | Có thể đổi workload, concurrency và số mẫu theo yêu cầu. Không tự đặt SLA để đánh dấu PASS. |

## Các điểm đã có quy tắc làm căn cứ kỹ thuật

- TC12: trim, uppercase, loại trùng trước đếm giới hạn và tính điểm (A-BR-05).
- TC42: mã sở thích lạ/inactive trả 422 INVALID_INTEREST_CODE (A-BR-10).
- TC28: tiền VND nguyên trong DB, xuất chuỗi chữ số, không mất độ chính xác. `4290000.00` là đầu vào thử giá trị tiền; không tự đổi schema VND thành số lẻ khi chưa có yêu cầu. Kiểm chứng chuỗi biểu diễn chính xác cùng giá trị số.
- TC16/44: page từ 1, size 1–10, keyword tối đa 100 sau trim, tối đa 10 mã duy nhất; lỗi giới hạn trả 400.

## Phạm vi ảnh hưởng

API: bổ sung kiểm tra tên query, mã lỗi điểm đến; giữ nguyên tập tham số. Frontend: chỉ gửi tham số hỗ trợ, hiển thị lỗi hiện có. Database: không đổi schema nghiệp vụ cho bốn quyết định đầu. Tests: thêm request/response, NFC và kiểm tra mapping trước/sau. RTM và Excel lưu cả Expected CSV và quyết định dùng để đối chiếu. TC38 giữ BLOCKED nếu thiếu ngưỡng nghiệm thu.

## Xác nhận

Người yêu cầu chọn “Chấp nhận các đề xuất” đối với câu hỏi A-ADR-01–04 trong cuộc trò chuyện ngày 18/11/2026. Đây là căn cứ triển khai kỹ thuật của đợt kiểm thử này; không thay thế biên bản nghiệm thu thương mại. Câu hỏi ngưỡng p95/mức tải TC38 vẫn đang chờ phản hồi. Không ghi chữ ký hoặc tên người phê duyệt chưa được cung cấp.
