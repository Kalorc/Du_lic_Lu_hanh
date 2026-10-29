-- =====================================================================
-- Du lieu mau. Moc thoi gian tinh tuong doi theo CURRENT_TIMESTAMP
-- de booking window luon hop le khi demo.
-- =====================================================================

-- ------------------------- DIEM DEN -------------------------
INSERT INTO destinations (destination_id, name, slug, destination_type, parent_destination_id, province_name, region, country_code, latitude, longitude, is_active) VALUES
 ('MIENTRUNG','Miền Trung','mien-trung','REGION',NULL,NULL,'Miền Trung','VN',NULL,NULL,TRUE),
 ('MIENBAC','Miền Bắc','mien-bac','REGION',NULL,NULL,'Miền Bắc','VN',NULL,NULL,TRUE),
 ('MIENNAM','Miền Nam','mien-nam','REGION',NULL,NULL,'Miền Nam','VN',NULL,NULL,TRUE),
 ('DN','Đà Nẵng','da-nang','CITY','MIENTRUNG','Đà Nẵng','Miền Trung','VN',16.047079,108.206230,TRUE),
 ('HOIAN','Hội An','hoi-an','CITY','MIENTRUNG','Quảng Nam','Miền Trung','VN',15.880060,108.338047,TRUE),
 ('HUE','Huế','hue','CITY','MIENTRUNG','Thừa Thiên Huế','Miền Trung','VN',16.463713,107.590866,TRUE),
 ('NT','Nha Trang','nha-trang','CITY','MIENTRUNG','Khánh Hòa','Miền Trung','VN',12.238791,109.196749,TRUE),
 ('DLT','Đà Lạt','da-lat','CITY','MIENNAM','Lâm Đồng','Tây Nguyên','VN',11.940419,108.458313,TRUE),
 ('PQ','Phú Quốc','phu-quoc','CITY','MIENNAM','Kiên Giang','Miền Nam','VN',10.289889,103.983459,TRUE),
 ('SGN','TP. Hồ Chí Minh','tp-ho-chi-minh','CITY','MIENNAM','TP. Hồ Chí Minh','Miền Nam','VN',10.762622,106.660172,TRUE),
 ('HN','Hà Nội','ha-noi','CITY','MIENBAC','Hà Nội','Miền Bắc','VN',21.027764,105.834160,TRUE),
 ('SAPA','Sa Pa','sa-pa','CITY','MIENBAC','Lào Cai','Miền Bắc','VN',22.336215,103.843895,TRUE),
 ('BANAHILLS','Bà Nà Hills','ba-na-hills','ATTRACTION','DN','Đà Nẵng','Miền Trung','VN',15.995278,107.988333,TRUE);

-- ------------------------- DANH MUC SO THICH (10 muc theo dac ta) -------------------------
INSERT INTO tour_interests (interest_id, code, name, description, is_active, display_order) VALUES
 (1,'BEACH','Biển','Tour biển, đảo',TRUE,1),
 (2,'MOUNTAIN','Núi','Núi, cao nguyên',TRUE,2),
 (3,'CULTURE','Văn hóa','Di sản, trải nghiệm văn hóa',TRUE,3),
 (4,'NATURE','Thiên nhiên','Cảnh quan, sinh thái',TRUE,4),
 (5,'ADVENTURE','Khám phá','Hoạt động trải nghiệm',TRUE,5),
 (6,'RELAXATION','Nghỉ dưỡng','Nghỉ ngơi, thư giãn',TRUE,6),
 (7,'FOOD','Ẩm thực','Khám phá món ăn',TRUE,7),
 (8,'FAMILY','Gia đình','Tour phù hợp nhóm gia đình',TRUE,8),
 (9,'HISTORY','Lịch sử','Di tích, lịch sử',TRUE,9),
 (10,'CITY','Đô thị','Trải nghiệm thành phố',TRUE,10),
 (11,'FESTIVAL','Lễ hội','Sự kiện, lễ hội địa phương (đang tắt để thử nghiệm is_active)',FALSE,11);

-- ------------------------- TOUR -------------------------
INSERT INTO tours (tour_id, tour_code, slug, lifecycle_status, current_published_version_id, revision) VALUES
 (101,'T101','da-nang-bien-va-am-thuc','ACTIVE',NULL,1),
 (102,'T102','da-nang-nghi-duong-cao-cap','ACTIVE',NULL,1),
 (103,'T103','da-lat-san-may','ACTIVE',NULL,1),
 (104,'T104','da-nang-ba-na-4-ngay','ACTIVE',NULL,1),
 (105,'T105','da-nang-gia-soc','ACTIVE',NULL,0),
 (106,'T106','hoi-an-pho-co-va-am-thuc','ACTIVE',NULL,1),
 (107,'T107','hue-di-san','ACTIVE',NULL,1),
 (108,'T108','phu-quoc-bien-xanh','ACTIVE',NULL,1),
 (109,'T109','sa-pa-chinh-phuc-fansipan','ACTIVE',NULL,1),
 (110,'T110','nha-trang-lan-bien','ACTIVE',NULL,1),
 (111,'T111','ha-noi-city-tour','ACTIVE',NULL,1),
 (112,'T112','da-nang-hoi-an-tron-goi','ACTIVE',NULL,1),
 (113,'T113','da-nang-bien-va-van-hoa','ACTIVE',NULL,1),
 (114,'T114','da-nang-ban-cu-luu-tru','ARCHIVED',NULL,1);

-- ------------------------- PHIEN BAN NOI DUNG TOUR -------------------------
INSERT INTO tour_versions (tour_version_id, tour_id, version_no, content_status, title, summary, description,
                           origin_destination_id, duration_days, nights, itinerary_json, included_services_json,
                           excluded_services_json, published_at) VALUES
 (201,101,1,'PUBLISHED','Đà Nẵng biển và ẩm thực','Ba ngày tắm biển Mỹ Khê và ăn đặc sản miền Trung.',
   'Hành trình kết hợp nghỉ biển và khám phá ẩm thực Đà Nẵng - Hội An.','SGN',3,2,
   '[{"day":1,"title":"Đến Đà Nẵng, tắm biển Mỹ Khê"},{"day":2,"title":"Hội An - phố cổ và ẩm thực đường phố"},{"day":3,"title":"Chợ Hàn, mua đặc sản, tiễn sân bay"}]',
   '["Vé máy bay khứ hồi","Khách sạn 4 sao","Ăn 6 bữa chính","Hướng dẫn viên"]',
   '["Chi phí cá nhân","Tip hướng dẫn viên"]', CURRENT_TIMESTAMP),
 (215,101,2,'DRAFT','Đà Nẵng biển và ẩm thực (bản 2)','Bản nháp đang chuẩn bị cho mùa cao điểm.',
   'Bản nháp để minh họa quy tắc: phiên bản mới đang soạn nhưng bản đã công bố vẫn được dùng.','SGN',3,2,
   '[]','[]','[]', NULL),
 (202,102,1,'PUBLISHED','Đà Nẵng nghỉ dưỡng cao cấp','Resort 5 sao sát biển, spa mỗi ngày.',
   'Gói nghỉ dưỡng cao cấp dành cho khách ưu tiên sự thư giãn.','SGN',3,2,'[]','[]','[]', CURRENT_TIMESTAMP),
 (203,103,1,'PUBLISHED','Đà Lạt săn mây','Săn mây Cầu Đất, thác Datanla, chợ đêm.',
   'Tour cao nguyên cho khách thích khí hậu mát và thiên nhiên.','SGN',3,2,'[]','[]','[]', CURRENT_TIMESTAMP),
 (204,104,1,'PUBLISHED','Đà Nẵng - Bà Nà bốn ngày','Bốn ngày gồm Bà Nà Hills và Cầu Vàng.',
   'Tour dài hơn một ngày so với tour tiêu chuẩn.','SGN',4,3,'[]','[]','[]', CURRENT_TIMESTAMP),
 (205,105,1,'PENDING','Đà Nẵng giá sốc','Bản nội dung đang chờ duyệt, chưa được công bố.',
   'Dùng để minh họa tour bị loại vì chưa công bố nội dung.','SGN',3,2,'[]','[]','[]', NULL),
 (206,106,1,'PUBLISHED','Hội An phố cổ và ẩm thực','Đèn lồng, nhà cổ, cao lầu và mì Quảng.',
   'Tour văn hóa - ẩm thực tại Hội An.','SGN',3,2,'[]','[]','[]', CURRENT_TIMESTAMP),
 (207,107,1,'PUBLISHED','Huế di sản','Đại Nội, lăng tẩm, ca Huế trên sông Hương.',
   'Tour lịch sử - văn hóa tại Huế.','SGN',3,2,'[]','[]','[]', CURRENT_TIMESTAMP),
 (208,108,1,'PUBLISHED','Phú Quốc biển xanh','Bốn ngày đảo ngọc, lặn ngắm san hô.',
   'Tour biển đảo phù hợp gia đình.','SGN',4,3,'[]','[]','[]', CURRENT_TIMESTAMP),
 (209,109,1,'PUBLISHED','Sa Pa chinh phục Fansipan','Bản Cát Cát, đỉnh Fansipan, chợ vùng cao.',
   'Tour núi cho khách thích khám phá.','HN',3,2,'[]','[]','[]', CURRENT_TIMESTAMP),
 (210,110,1,'PUBLISHED','Nha Trang lặn biển','Hòn Mun, lặn ngắm san hô, tắm bùn khoáng.',
   'Tour biển kết hợp hoạt động trải nghiệm.','SGN',3,2,'[]','[]','[]', CURRENT_TIMESTAMP),
 (211,111,1,'PUBLISHED','Hà Nội city tour','Hai ngày phố cổ, Hồ Tây, bún chả.',
   'Tour đô thị ngắn ngày.','SGN',2,1,'[]','[]','[]', CURRENT_TIMESTAMP),
 (212,112,1,'PUBLISHED','Đà Nẵng - Hội An trọn gói','Ba ngày gộp hai điểm đến nổi bật miền Trung.',
   'Tour liên kết nhiều điểm đến trong một phiên bản.','SGN',3,2,'[]','[]','[]', CURRENT_TIMESTAMP),
 (213,113,1,'PUBLISHED','Đà Nẵng biển và văn hóa','Ba ngày hai đêm, biển Mỹ Khê và di sản Chăm.',
   'Tour cân bằng giữa nghỉ biển và trải nghiệm văn hóa.','SGN',3,2,
   '[{"day":1,"title":"Biển Mỹ Khê"},{"day":2,"title":"Bảo tàng Chăm, Ngũ Hành Sơn"},{"day":3,"title":"Hội An"}]',
   '["Khách sạn 4 sao","Ăn 5 bữa chính","Hướng dẫn viên"]','["Vé máy bay","Chi phí cá nhân"]', CURRENT_TIMESTAMP),
 (214,114,1,'PUBLISHED','Đà Nẵng bản cũ lưu trữ','Tour đã ngừng bán.',
   'Dùng để minh họa tour ARCHIVED không xuất hiện trong kết quả công khai.','SGN',3,2,'[]','[]','[]', CURRENT_TIMESTAMP);

UPDATE tours SET current_published_version_id = 201 WHERE tour_id = 101;
UPDATE tours SET current_published_version_id = 202 WHERE tour_id = 102;
UPDATE tours SET current_published_version_id = 203 WHERE tour_id = 103;
UPDATE tours SET current_published_version_id = 204 WHERE tour_id = 104;
UPDATE tours SET current_published_version_id = 206 WHERE tour_id = 106;
UPDATE tours SET current_published_version_id = 207 WHERE tour_id = 107;
UPDATE tours SET current_published_version_id = 208 WHERE tour_id = 108;
UPDATE tours SET current_published_version_id = 209 WHERE tour_id = 109;
UPDATE tours SET current_published_version_id = 210 WHERE tour_id = 110;
UPDATE tours SET current_published_version_id = 211 WHERE tour_id = 111;
UPDATE tours SET current_published_version_id = 212 WHERE tour_id = 112;
UPDATE tours SET current_published_version_id = 213 WHERE tour_id = 113;
UPDATE tours SET current_published_version_id = 214 WHERE tour_id = 114;
-- Tour 105 khong co phien ban nao duoc cong bo.

-- ------------------------- DIEM DEN CUA PHIEN BAN -------------------------
INSERT INTO tour_version_destinations (tour_version_id, destination_id, stop_order, is_primary) VALUES
 (201,'DN',1,TRUE),(201,'HOIAN',2,FALSE),
 (215,'DN',1,TRUE),
 (202,'DN',1,TRUE),
 (203,'DLT',1,TRUE),
 (204,'DN',1,TRUE),(204,'BANAHILLS',2,FALSE),
 (205,'DN',1,TRUE),
 (206,'HOIAN',1,TRUE),
 (207,'HUE',1,TRUE),
 (208,'PQ',1,TRUE),
 (209,'SAPA',1,TRUE),
 (210,'NT',1,TRUE),
 (211,'HN',1,TRUE),
 (212,'DN',1,TRUE),(212,'HOIAN',2,FALSE),
 (213,'DN',1,TRUE),
 (214,'DN',1,TRUE);

-- ------------------------- GAN SO THICH CHO PHIEN BAN -------------------------
INSERT INTO tour_version_interest_tags (tour_version_id, interest_id) VALUES
 (201,1),(201,7),(201,6),          -- BEACH, FOOD, RELAXATION
 (215,1),(215,7),                  -- ban nhap
 (202,6),(202,1),                  -- RELAXATION, BEACH
 (203,2),(203,4),                  -- MOUNTAIN, NATURE
 (204,5),(204,10),                 -- ADVENTURE, CITY
 (205,1),(205,7),
 (206,3),(206,7),(206,9),          -- CULTURE, FOOD, HISTORY
 (207,9),(207,3),                  -- HISTORY, CULTURE
 (208,1),(208,6),(208,8),          -- BEACH, RELAXATION, FAMILY
 (209,2),(209,5),(209,4),          -- MOUNTAIN, ADVENTURE, NATURE
 (210,1),(210,5),                  -- BEACH, ADVENTURE
 (211,10),(211,3),(211,7),         -- CITY, CULTURE, FOOD
 (212,1),(212,3),(212,7),          -- BEACH, CULTURE, FOOD
 (213,1),(213,3),                  -- BEACH, CULTURE
 (213,11),                         -- FESTIVAL: interest is_active=FALSE -> khong duoc tinh diem
 (214,1);

-- ------------------------- HINH ANH -------------------------
INSERT INTO tour_media (media_id, tour_version_id, media_type, media_url, alt_text, caption, sort_order, is_cover) VALUES
 (501,201,'IMAGE','https://images.example.com/tours/da-nang-bien-am-thuc-cover.jpg','Bãi biển Mỹ Khê','Biển Mỹ Khê lúc bình minh',1,TRUE),
 (502,201,'IMAGE','https://images.example.com/tours/da-nang-cho-han.jpg','Chợ Hàn',NULL,2,FALSE),
 (503,213,'IMAGE','https://images.example.com/tours/da-nang-bien-van-hoa-cover.jpg','Ngũ Hành Sơn','Ngũ Hành Sơn',1,TRUE),
 (504,206,'IMAGE','https://images.example.com/tours/hoi-an-den-long-cover.jpg','Phố cổ Hội An',NULL,1,TRUE),
 (505,203,'IMAGE','https://images.example.com/tours/da-lat-cau-dat-cover.jpg','Đồi chè Cầu Đất',NULL,1,TRUE),
 (506,208,'IMAGE','https://images.example.com/tours/phu-quoc-cover.jpg','Bãi Sao Phú Quốc',NULL,1,TRUE),
 (507,209,'IMAGE','https://images.example.com/tours/sa-pa-cover.jpg','Fansipan',NULL,1,TRUE),
 (508,210,'IMAGE','https://images.example.com/tours/nha-trang-cover.jpg','Hòn Mun',NULL,1,TRUE),
 (509,212,'IMAGE','https://images.example.com/tours/da-nang-hoi-an-cover.jpg','Cầu Rồng',NULL,1,TRUE);

-- ------------------------- CHUYEN KHOI HANH -------------------------
INSERT INTO tour_departures (departure_id, departure_code, tour_id, tour_version_id,
       starts_at, ends_at, booking_opens_at, booking_closes_at, sales_status,
       total_seats, sellable_capacity, available_seats, hold_seats, confirmed_seats, active_offer_id) VALUES
 -- Tour 101: ba chuyen dang ban, gia khac nhau
 (301,'T101-D1',101,201, DATEADD('DAY',17,CURRENT_TIMESTAMP), DATEADD('DAY',19,CURRENT_TIMESTAMP),
      DATEADD('DAY',-30,CURRENT_TIMESTAMP), DATEADD('DAY',14,CURRENT_TIMESTAMP),'OPEN',30,30,12,2,16,NULL),
 (302,'T101-D2',101,201, DATEADD('DAY',24,CURRENT_TIMESTAMP), DATEADD('DAY',26,CURRENT_TIMESTAMP),
      DATEADD('DAY',-30,CURRENT_TIMESTAMP), DATEADD('DAY',21,CURRENT_TIMESTAMP),'OPEN',30,30,20,0,10,NULL),
 (303,'T101-D3',101,201, DATEADD('DAY',31,CURRENT_TIMESTAMP), DATEADD('DAY',33,CURRENT_TIMESTAMP),
      DATEADD('DAY',-30,CURRENT_TIMESTAMP), DATEADD('DAY',28,CURRENT_TIMESTAMP),'OPEN',30,30,25,0,5,NULL),
 -- Chuyen da dong ban (minh hoa loai bo theo sales_status)
 (304,'T101-D4',101,201, DATEADD('DAY',40,CURRENT_TIMESTAMP), DATEADD('DAY',42,CURRENT_TIMESTAMP),
      DATEADD('DAY',10,CURRENT_TIMESTAMP), DATEADD('DAY',38,CURRENT_TIMESTAMP),'CLOSED',30,30,30,0,0,NULL),
 -- Tour 113
 (314,'T113-D1',113,213, DATEADD('DAY',20,CURRENT_TIMESTAMP), DATEADD('DAY',22,CURRENT_TIMESTAMP),
      DATEADD('DAY',-20,CURRENT_TIMESTAMP), DATEADD('DAY',18,CURRENT_TIMESTAMP),'OPEN',25,25,9,1,15,NULL),
 -- Tour 102 (vuot ngan sach)
 (321,'T102-D1',102,202, DATEADD('DAY',18,CURRENT_TIMESTAMP), DATEADD('DAY',20,CURRENT_TIMESTAMP),
      DATEADD('DAY',-20,CURRENT_TIMESTAMP), DATEADD('DAY',16,CURRENT_TIMESTAMP),'OPEN',20,20,15,0,5,NULL),
 -- Tour 103 (sai dia diem)
 (331,'T103-D1',103,203, DATEADD('DAY',15,CURRENT_TIMESTAMP), DATEADD('DAY',17,CURRENT_TIMESTAMP),
      DATEADD('DAY',-25,CURRENT_TIMESTAMP), DATEADD('DAY',13,CURRENT_TIMESTAMP),'OPEN',25,25,18,0,7,NULL),
 -- Tour 104 (sai thoi luong)
 (341,'T104-D1',104,204, DATEADD('DAY',21,CURRENT_TIMESTAMP), DATEADD('DAY',24,CURRENT_TIMESTAMP),
      DATEADD('DAY',-25,CURRENT_TIMESTAMP), DATEADD('DAY',19,CURRENT_TIMESTAMP),'OPEN',25,25,20,0,5,NULL),
 -- Tour 105 (noi dung chua cong bo)
 (351,'T105-D1',105,205, DATEADD('DAY',19,CURRENT_TIMESTAMP), DATEADD('DAY',21,CURRENT_TIMESTAMP),
      DATEADD('DAY',-10,CURRENT_TIMESTAMP), DATEADD('DAY',17,CURRENT_TIMESTAMP),'OPEN',25,25,22,0,3,NULL),
 -- Tour 106
 (361,'T106-D1',106,206, DATEADD('DAY',12,CURRENT_TIMESTAMP), DATEADD('DAY',14,CURRENT_TIMESTAMP),
      DATEADD('DAY',-30,CURRENT_TIMESTAMP), DATEADD('DAY',10,CURRENT_TIMESTAMP),'OPEN',30,30,14,0,16,NULL),
 -- Tour 107
 (371,'T107-D1',107,207, DATEADD('DAY',16,CURRENT_TIMESTAMP), DATEADD('DAY',18,CURRENT_TIMESTAMP),
      DATEADD('DAY',-30,CURRENT_TIMESTAMP), DATEADD('DAY',14,CURRENT_TIMESTAMP),'OPEN',30,30,21,0,9,NULL),
 -- Tour 108
 (381,'T108-D1',108,208, DATEADD('DAY',25,CURRENT_TIMESTAMP), DATEADD('DAY',28,CURRENT_TIMESTAMP),
      DATEADD('DAY',-30,CURRENT_TIMESTAMP), DATEADD('DAY',23,CURRENT_TIMESTAMP),'OPEN',24,24,11,0,13,NULL),
 -- Tour 109
 (391,'T109-D1',109,209, DATEADD('DAY',22,CURRENT_TIMESTAMP), DATEADD('DAY',24,CURRENT_TIMESTAMP),
      DATEADD('DAY',-30,CURRENT_TIMESTAMP), DATEADD('DAY',20,CURRENT_TIMESTAMP),'OPEN',28,28,17,0,11,NULL),
 -- Tour 110: chuyen re nhat lai HET CHO -> minh hoa availableOnly
 (401,'T110-D1',110,210, DATEADD('DAY',14,CURRENT_TIMESTAMP), DATEADD('DAY',16,CURRENT_TIMESTAMP),
      DATEADD('DAY',-30,CURRENT_TIMESTAMP), DATEADD('DAY',12,CURRENT_TIMESTAMP),'OPEN',26,26,13,0,13,NULL),
 (402,'T110-D2',110,210, DATEADD('DAY',28,CURRENT_TIMESTAMP), DATEADD('DAY',30,CURRENT_TIMESTAMP),
      DATEADD('DAY',-30,CURRENT_TIMESTAMP), DATEADD('DAY',26,CURRENT_TIMESTAMP),'OPEN',26,26,0,0,26,NULL),
 -- Tour 111
 (411,'T111-D1',111,211, DATEADD('DAY',10,CURRENT_TIMESTAMP), DATEADD('DAY',11,CURRENT_TIMESTAMP),
      DATEADD('DAY',-30,CURRENT_TIMESTAMP), DATEADD('DAY',8,CURRENT_TIMESTAMP),'OPEN',35,35,24,0,11,NULL),
 -- Tour 112
 (421,'T112-D1',112,212, DATEADD('DAY',19,CURRENT_TIMESTAMP), DATEADD('DAY',21,CURRENT_TIMESTAMP),
      DATEADD('DAY',-30,CURRENT_TIMESTAMP), DATEADD('DAY',17,CURRENT_TIMESTAMP),'OPEN',30,30,16,0,14,NULL),
 -- Tour 114 (tour ARCHIVED)
 (431,'T114-D1',114,214, DATEADD('DAY',20,CURRENT_TIMESTAMP), DATEADD('DAY',22,CURRENT_TIMESTAMP),
      DATEADD('DAY',-30,CURRENT_TIMESTAMP), DATEADD('DAY',18,CURRENT_TIMESTAMP),'OPEN',30,30,20,0,10,NULL),
 -- Chuyen chua mo cua so dat (booking_opens_at o tuong lai)
 (441,'T106-D2',106,206, DATEADD('DAY',60,CURRENT_TIMESTAMP), DATEADD('DAY',62,CURRENT_TIMESTAMP),
      DATEADD('DAY',30,CURRENT_TIMESTAMP), DATEADD('DAY',58,CURRENT_TIMESTAMP),'OPEN',30,30,30,0,0,NULL);

-- ------------------------- PHIEN BAN GIA -------------------------
INSERT INTO departure_offer_versions (offer_id, departure_id, version_no, approval_status,
       adult_price, child_price, infant_price, currency, policy_json, approved_at) VALUES
 -- Chuyen 301: v1 DA DUYET 4.200.000; v2 CHO DUYET 3.000.000 -> he thong phai dung 4.200.000
 (601,301,1,'APPROVED',4200000,3150000,0,'VND','{"deposit":"30%","cancelBefore7Days":"Hoàn 70%"}',CURRENT_TIMESTAMP),
 (602,301,2,'PENDING', 3000000,2250000,0,'VND','{"deposit":"30%"}',NULL),
 (603,302,1,'APPROVED',4600000,3450000,0,'VND','{"deposit":"30%"}',CURRENT_TIMESTAMP),
 (604,303,1,'APPROVED',5200000,3900000,0,'VND','{"deposit":"30%"}',CURRENT_TIMESTAMP),
 (605,304,1,'APPROVED',3900000,2900000,0,'VND','{"deposit":"30%"}',CURRENT_TIMESTAMP),
 (614,314,1,'APPROVED',5000000,3750000,0,'VND','{"deposit":"30%","cancelBefore7Days":"Hoàn 70%"}',CURRENT_TIMESTAMP),
 (621,321,1,'APPROVED',6000000,4500000,0,'VND','{"deposit":"50%"}',CURRENT_TIMESTAMP),
 (631,331,1,'APPROVED',3500000,2600000,0,'VND','{"deposit":"30%"}',CURRENT_TIMESTAMP),
 (641,341,1,'APPROVED',4500000,3400000,0,'VND','{"deposit":"30%"}',CURRENT_TIMESTAMP),
 (651,351,1,'APPROVED',3800000,2850000,0,'VND','{"deposit":"30%"}',CURRENT_TIMESTAMP),
 (661,361,1,'APPROVED',2800000,2100000,0,'VND','{"deposit":"30%"}',CURRENT_TIMESTAMP),
 (671,371,1,'APPROVED',3200000,2400000,0,'VND','{"deposit":"30%"}',CURRENT_TIMESTAMP),
 (681,381,1,'APPROVED',6500000,4900000,0,'VND','{"deposit":"50%"}',CURRENT_TIMESTAMP),
 (691,391,1,'APPROVED',4100000,3100000,0,'VND','{"deposit":"30%"}',CURRENT_TIMESTAMP),
 (701,401,1,'APPROVED',4400000,3300000,0,'VND','{"deposit":"30%"}',CURRENT_TIMESTAMP),
 (702,402,1,'APPROVED',3900000,2900000,0,'VND','{"deposit":"30%"}',CURRENT_TIMESTAMP),
 (711,411,1,'APPROVED',1900000,1400000,0,'VND','{"deposit":"20%"}',CURRENT_TIMESTAMP),
 (721,421,1,'APPROVED',4900000,3700000,0,'VND','{"deposit":"30%"}',CURRENT_TIMESTAMP),
 (731,431,1,'APPROVED',4000000,3000000,0,'VND','{"deposit":"30%"}',CURRENT_TIMESTAMP),
 (741,441,1,'DRAFT',   2700000,2000000,0,'VND','{"deposit":"30%"}',NULL);

-- Gan phien ban gia hien hanh cho tung chuyen
UPDATE tour_departures SET active_offer_id = 601 WHERE departure_id = 301;
UPDATE tour_departures SET active_offer_id = 603 WHERE departure_id = 302;
UPDATE tour_departures SET active_offer_id = 604 WHERE departure_id = 303;
UPDATE tour_departures SET active_offer_id = 605 WHERE departure_id = 304;
UPDATE tour_departures SET active_offer_id = 614 WHERE departure_id = 314;
UPDATE tour_departures SET active_offer_id = 621 WHERE departure_id = 321;
UPDATE tour_departures SET active_offer_id = 631 WHERE departure_id = 331;
UPDATE tour_departures SET active_offer_id = 641 WHERE departure_id = 341;
UPDATE tour_departures SET active_offer_id = 651 WHERE departure_id = 351;
UPDATE tour_departures SET active_offer_id = 661 WHERE departure_id = 361;
UPDATE tour_departures SET active_offer_id = 671 WHERE departure_id = 371;
UPDATE tour_departures SET active_offer_id = 681 WHERE departure_id = 381;
UPDATE tour_departures SET active_offer_id = 691 WHERE departure_id = 391;
UPDATE tour_departures SET active_offer_id = 701 WHERE departure_id = 401;
UPDATE tour_departures SET active_offer_id = 702 WHERE departure_id = 402;
UPDATE tour_departures SET active_offer_id = 711 WHERE departure_id = 411;
UPDATE tour_departures SET active_offer_id = 721 WHERE departure_id = 421;
UPDATE tour_departures SET active_offer_id = 731 WHERE departure_id = 431;
UPDATE tour_departures SET active_offer_id = 741 WHERE departure_id = 441;

-- ------------------------- DAT LAI BO DEM IDENTITY -------------------------
ALTER TABLE tours                     ALTER COLUMN tour_id         RESTART WITH 1000;
ALTER TABLE tour_versions             ALTER COLUMN tour_version_id RESTART WITH 1000;
ALTER TABLE tour_interests            ALTER COLUMN interest_id     RESTART WITH 100;
ALTER TABLE tour_media                ALTER COLUMN media_id        RESTART WITH 1000;
ALTER TABLE tour_departures           ALTER COLUMN departure_id    RESTART WITH 1000;
ALTER TABLE departure_offer_versions  ALTER COLUMN offer_id        RESTART WITH 1000;
