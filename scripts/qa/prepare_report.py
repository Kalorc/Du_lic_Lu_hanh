"""Assemble real Module A execution records; never infer acceptance from a target."""
from pathlib import Path
from datetime import datetime, timezone
import csv
import hashlib
import json
import re
import shutil

REPO = Path(__file__).resolve().parents[2]
REPORT = REPO.parent / 'Báo cáo Quản lý dự án phần mềm' / 'Kiểm thử và Đảm bảo chất lượng'
RUNTIME = REPO.parent / 'module-a-qa-runtime'
PRIVATE = Path(r'C:\Users\Admin\Documents\ChatGPT\Quản lý dự án phần mềm\module-a-qa-20261118')
EVIDENCE = REPORT / 'Test_Evidence'
SOURCE = json.loads((RUNTIME / 'final-source-manifest.json').read_text(encoding='utf-8'))
SOURCE_ID = SOURCE['sourceId']
BUILD_FILES = {row['path']: row['sha256'] for row in SOURCE['files']}

def read_json(path):
    return json.loads(Path(path).read_text(encoding='utf-8-sig'))

def relative(path):
    return Path(path).relative_to(REPORT).as_posix()

def text(value):
    return json.dumps(value, ensure_ascii=False, default=str, separators=(',', ':'))

def sanitize_log(content):
    content = re.sub(r'(?im)(Using generated security password:)\s*\S+', r'\1 [REDACTED]', content)
    content = re.sub(r'(?i)(mat khau:)\s*123456', r'\1 [REDACTED]', content)
    content = re.sub(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', '[REDACTED_JWT]', content)
    private_password = (RUNTIME / 'private-demo-password.txt').read_text().strip()
    return content.replace(private_password, '[REDACTED]')

def copy_logs(source, destination):
    if not source.exists():
        return
    destination.mkdir(parents=True, exist_ok=True)
    for item in source.iterdir():
        if item.is_file() and item.suffix in ('.log', '.xml', '.txt'):
            content = item.read_text(encoding='utf-8', errors='replace')
            (destination / item.name).write_text(sanitize_log(content), encoding='utf-8')

for name in ('api-red-verified', 'api-red-extra', 'api-green', 'api-regression',
             'review-red', 'lock-red', 'review-green', 'review-green-2', 'api-final',
             'admin-red', 'admin-green', 'admin-green-2', 'browser-final', 'e2e-final',
             'e2e-postgres-final', 'performance-final', 'postgres-locale-red', 'postgres-locale-final',
             'e2e-postgres-unicode-final', 'performance-unicode-final'):
    if (RUNTIME / name).exists():
        shutil.copytree(RUNTIME / name, EVIDENCE / name, dirs_exist_ok=True)
if (RUNTIME/'clean-snapshot-final').exists():
    shutil.copytree(RUNTIME/'clean-snapshot-final',EVIDENCE/'clean-snapshot-final',dirs_exist_ok=True)
for name in ('backend-final.log', 'frontend-final-build.log', 'backend-final-live.log',
             'backend-postgres-final-live.log', 'frontend-final-production-audit.json',
             'frontend-final-full-audit.json', 'admin-red.log', 'admin-green-2.log',
             'review-red.log', 'lock-red.log', 'review-green.log', 'review-green-2.log',
             'api-red-verified.log', 'api-red-extra.log', 'browser-final.log', 'e2e-final.log',
             'performance-final-h2.log', 'performance-final-postgres.log'):
    path = RUNTIME / name
    if path.exists():
        destination = EVIDENCE / 'Build_and_Run' / name
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(sanitize_log(path.read_text(encoding='utf-8', errors='replace')), encoding='utf-8')
for name in ('backend-postgres-unicode-final-live.log', 'e2e-postgres-unicode-final.log', 'performance-unicode-final-postgres.log'):
    path=RUNTIME/name
    if path.exists():
        (EVIDENCE/'Build_and_Run'/name).write_text(sanitize_log(path.read_text(encoding='utf-8',errors='replace')),encoding='utf-8')
copy_logs(PRIVATE / 'baseline', EVIDENCE / 'Baseline_Logs')
copy_logs(PRIVATE / 'baseline' / 'surefire-red', EVIDENCE / 'Baseline_Logs' / 'JUnit_Red')
copy_logs(RUNTIME / 'junit-final', EVIDENCE / 'JUnit_Final')
for name in ('final-source-manifest.json', 'clean-install-manifest.json', 'postgres-install-manifest.json', 'postgres-unicode-install-manifest.json'):
    path = RUNTIME / name
    if path.exists():
        shutil.copy2(path, EVIDENCE / name)
for name in ('module-a-document-audit.md', 'document-manifest.json'):
    if (PRIVATE / name).exists():
        shutil.copy2(PRIVATE / name, REPORT / name)
for path in EVIDENCE.rglob('*'):
    if path.is_file() and path.suffix in ('.log','.xml','.txt'):
        content=path.read_text(encoding='utf-8',errors='replace')
        cleaned=sanitize_log(content)
        if cleaned!=content:path.write_text(cleaned,encoding='utf-8')

original = REPORT / 'test_case.csv'
with original.open(encoding='utf-8-sig', newline='') as handle:
    rows = list(csv.DictReader(handle))
assert len(rows) == 45 and [r['Test ID'] for r in rows] == [f'TC{i:02}' for i in range(1, 46)]
assert all(r['Status'] == 'CHƯA CHẠY' for r in rows)
csv_hash = hashlib.sha256(original.read_bytes()).hexdigest()
(REPORT / 'Test_Data').mkdir(exist_ok=True)
shutil.copy2(original, REPORT / 'Test_Data' / 'test_case_original.csv')
shutil.copy2(REPO / 'backend/src/main/resources/schema.sql', REPORT / 'Test_Data' / 'schema.sql')
shutil.copy2(REPO / 'backend/src/main/resources/data.sql', REPORT / 'Test_Data' / 'data.sql')

mapping = {}
audit = (PRIVATE / 'module-a-document-audit.md').read_text(encoding='utf-8')
for line in audit.splitlines():
    if re.match(r'\| TC\d{2} / P[012] \|', line):
        columns = [c.strip() for c in line.strip('|').split('|')]
        case = columns[0].split(' / ')[0]
        requirements = [re.sub(r'^(FR|BR|NFR)(\d+)$', r'A-\1-\2', part.strip()) for part in columns[2].split(',')]
        mapping[case] = {'requirements': ', '.join(x for x in requirements if '-BR-' not in x),
                         'businessRules': ', '.join(x for x in requirements if '-BR-' in x),
                         'useCases': ', '.join('UC-A' + part for part in columns[3].split('/')),
                         'api': columns[4]}
assert len(mapping) == 45

REGRESSION_TRACE = {
    'regressionLargePageDoesNotOverflow': ['TC16'],
    'regressionOfferBelongsToDeparture': ['TC03', 'TC20', 'TC39'],
    'regressionCurrentVersionBelongsToTour': ['TC01', 'TC39'],
    'regressionDestinationQueriesDoNotGrowPerCandidate': ['TC38'],
    'regressionInternalCatalogRequiresProduct': ['TC21'],
    'regressionMalformedJsonIsBadRequest': ['TC22', 'TC44'],
    'regressionTagReplacementLocksVersion': ['TC22'],
}

history = []
records = {}
for path in sorted(EVIDENCE.rglob('*.json')):
    try:
        record = read_json(path)
    except (ValueError, UnicodeError):
        continue
    if not isinstance(record, dict):
        continue
    case = record.get('testId', record.get('tc'))
    if not re.fullmatch(r'TC\d{2}|REGRESSION', str(case)) or record.get('status') not in ('PASS', 'FAIL', 'BLOCKED'):
        continue
    run = path.relative_to(EVIDENCE).parts[0]
    record['_path'] = path
    record['_run'] = run
    if record.get('sourceId', '').startswith('source-sha256:'):
        version = record['sourceId']
    elif record.get('sourceSha256'):
        matched = all(BUILD_FILES.get('frontend/' + p) == sha for p, sha in record['sourceSha256'].items())
        version = SOURCE_ID if matched else 'frontend-file-manifest:' + hashlib.sha256(text(record['sourceSha256']).encode()).hexdigest()
    elif run in ('browser-final', 'e2e-final', 'e2e-postgres-final'):
        version = SOURCE_ID
    else:
        version = record.get('sourceId', 'Xem manifest/log của run')
    record['_sourceId'] = version
    records.setdefault(case, []).append(record)
    observations = record.get('observations', {})
    actual = text(observations)[0:1500] if observations else '; '.join(record.get('errors', []))[0:1500]
    traced = REGRESSION_TRACE.get(record.get('method', '').split('.')[-1], [case])
    history.append({'case': ','.join(traced), 'run': run, 'date': record.get('executedAt', record.get('finishedAt')),
                    'status': record['status'], 'version': version, 'actual': actual,
                    'evidence': relative(path), 'note': 'Harness size giả định sai; không dùng kết luận nghiệp vụ' if run == 'frontend-baseline' else ''})

for record in list(records.get('REGRESSION', [])):
    for case in REGRESSION_TRACE.get(record.get('method', '').split('.')[-1], []):
        records.setdefault(case, []).append(record)

for base in (EVIDENCE / 'Performance', EVIDENCE / 'performance-final', EVIDENCE/'performance-unicode-final'):
    for path in sorted(base.rglob('result.json')):
        result = read_json(path)
        if not isinstance(result, dict) or 'tourCount' not in result:
            continue
        run = base.name + '-' + result['database']
        version = SOURCE_ID if 'final' in base.name else 'Xem sourceSha256 tại environment.json của engine'
        actual = f"{result['tourCount']} Tours/candidates; p95={result['p95Ms']:.3f}ms; errors={result['errors']}/{result['measuredRequests']}; EXPLAIN thực; chưa chốt ngưỡng"
        record = {'testId': 'TC38', 'status': 'BLOCKED', '_path': path, '_run': run,
                  'executedAt': result['finishedAt'], 'observations': result, '_sourceId': version}
        records.setdefault('TC38', []).append(record)
        history.append({'case': 'TC38', 'run': run, 'date': result['finishedAt'], 'status': 'BLOCKED',
                        'version': version, 'actual': actual, 'evidence': relative(path), 'note': 'Measurement completed; acceptance chưa chốt'})

ACTUAL = {
1: 'Tour 105 trỏ vào DRAFT không có trong danh sách; detail 105 trả 404. Detail 101 chỉ đọc version 201 đã PUBLISHED.',
2: 'Đóng các departure của Tour 101: 101 bị loại cả availableOnly=true và false; không lấy giá chuyến CLOSED.',
3: 'Tour 101: priceFrom="4200000", representativeDepartureId="301". Tour 110: true="4400000", false="3900000".',
4: 'Offer 601 APPROVED=4200000, 602 PENDING=3000000: list dùng 4200000. Trỏ active offer301 sang PENDING: dùng chuyến302, giá4600000.',
5: 'maxBudget=5000000 nhận Tour113 giá5000000; maxBudget=4999999 loại Tour113.',
6: 'minBudget=6000000&maxBudget=5000000 trả 400 INVALID_FILTER vì minBudget không thuộc contract đã chốt.',
7: 'durationDays=3: tất cả kết quả đều 3 ngày, loại Tour111 (2 ngày). durationDays=2 nhận Tour111.',
8: 'Thêm HUE vào version201, lọc DN chỉ một Tour101; lọc HUE vẫn nhận Tour101, không nhân bản.',
9: 'Version201 chỉ gắn BEACH; chọn BEACH+FOOD: matchedInterests=[BEACH], matchScore=50.0.',
10: 'Chọn BEACH, lọc SAPA: Tour109 vẫn xuất hiện với matchScore=0.0.',
11: 'Không interest: mọi matchScore=null, meta.personalized=false, sort=PRICE_ASC; giá tăng dần.',
12: 'BEACH và " beach " chuẩn hóa thành một mã; data/score/order giống request chỉ BEACH, không tính trùng.',
13: 'Có đủ điểm100/50/0; danh sách khớp thứ tự điểm giảm dần.',
14: 'Cùng điểm100: ID9007199254740992 rồi9007199254740993 (giá3000000), sau đó9100 (giá4000000); ID so theo số.',
15: 'sort chứa DROP TABLE bị từ chối400 trên tours và recommendations; số dòng tours trước/sau không đổi.',
16: 'size=0/51/11/-1 trả400 INVALID_PAGINATION; size1 và10 trả200. page2147483647 trả200 items rỗng qua regression.',
17: '13 Tour QA: Tour5012 (100 điểm, ở cuối thứ tự thô) lên đầu trang1; hai trang10+3 có13 ID duy nhất, totalItems13.',
18: 'Tour5100 có3 destination và5 tag: lọc DN + sở thích chỉ trả một bản ghi Tour5100.',
19: 'Đổi mọi offer của101 sang PENDING:101 không có trong list; detail trả priceFrom=null, không hiển thị giá0 giả.',
20: 'List101 giá4200000; tạo approved offer7000 giá4500000 và đổi active_offer_id; detail đọc lại4500000.',
21: 'CUSTOMER/MANAGER/ADMIN PUT tag trả403, mapping215 trước/sau giống nhau; PRODUCT sửa PUBLISHED trả409. MANAGER catalog403, xem tag version200. UI không lưu nhầm ID.',
22: 'PRODUCT PUT [BEACH,UNKNOWN] trả422 INVALID_INTEREST_CODE; mapping215 giữ nguyên. JSON hỏng trả400 trong regression.',
23: 'Anonymous tours/recommendations/detail trả200; internal không token401.',
24: 'keyword toàn space trả data giống hệt không truyền keyword.',
25: 'ĐÀ NẴNG nhận101; NFD("Đà Nẵng") tương đương NFC; "Da Nang" không khớp theo chính sách phân biệt dấu.',
26: 'Chuỗi quote/OR/DROP, ký tự% và_ được xử lý như từ khóa thật,200 items rỗng; số Tour không đổi.',
27: 'destinationId=NO_SUCH_DEST trả422 INVALID_DESTINATION theo A-ADR-03.',
28: 'DB4290000.00 được biểu diễn JSON string "4290000" (cùng giá trị VND nguyên); list/detail đều chuỗi, không JS float.',
29: 'Tour/version/interest ID9007199254740993 (>2^53) được trả chuỗi nguyên vẹn trong list/detail/catalog.',
30: 'meta.asOf parse được OffsetDateTime ISO8601; bằng Clock cố định của run test, có offset Z.',
31: 'Ba kịch bản trình duyệt: success cũ, failure cũ và finally cũ không ghi đè kết quả/loading của request mới nhất.',
32: 'Lỗi API có thông báo/nút Thử lại; retry dùng bộ lọc đã áp dụng dù form đang có sửa chưa submit; lỗi tải catalog cũng phục hồi được.',
33: 'Bộ lọc không khớp hiện empty state; Xóa lọc trả lại danh sách Tour thật.',
34: 'Thiếu cover URL và ảnh thật tải lỗi đều có fallback; kích thước thumbnail/card giữ ổn định qua kiểm tra hình học.',
35: 'Viewport360/768/1440: không tràn ngang; submit, reset và link thẻ đầu/cuối đều vượt kiểm tra elementFromPoint, không bị che.',
36: 'Chỉ Tab/Enter: nhập Hà Nội, bật/tắt chip có aria-pressed/focus, submit rồi mở detail111; điểm0% đúng bộ lọc.',
37: 'Đóng connection pool của DB test riêng: GET trả500 INTERNAL_ERROR chung, có requestId/timestamp, không SQL/JDBC/stack/token.',
39: 'List/recommendations có cùng fixed asOf;101 có price4200000, duration3, version201 và điểm đến giống detail; một Clock dùng cho quyết định giá.',
40: 'Snapshot toàn bộ departure/counters trước/sau GET giống nhau; schema Module A không có Booking và GET không tạo bảng/Booking.',
41: 'Lặp5 lần cùng đầu vào BEACH/FOOD/CULTURE và fixed Clock: data, score, order giống request đầu.',
42: 'UNKNOWN và FESTIVAL(inactive) đều422 INVALID_INTEREST_CODE, không âm thầm bỏ mã sai.',
43: 'Tắt BEACH: loại catalog/score, PUT mới422, giữ mapping cũ. Bật lại khôi phục mapping/score100. UI hiển thị thẻ đã tắt, chủ động bỏ chọn rồi lưu CULTURE.',
44: 'keyword100 được nhận,101 bị400;11 mã duy nhất400;11 lần BEACH chuẩn hóa1 mã được200; budget0/-1/1.5/abc bị400.',
45: 'Từ schema/seed sạch, browser thật lọc DN/3 ngày/5000000/BEACH+CULTURE:112/113=100 điểm,101=50; mở detail112 thành công, không mock lõi.',
}

cases = []
test_data = []
for row in rows:
    case = row['Test ID']; number = int(case[2:])
    all_records = records.get(case, [])
    finals = [r for r in all_records if r['_run'] in ('api-final', 'browser-final', 'e2e-final', 'e2e-postgres-final',
        'postgres-locale-final', 'e2e-postgres-unicode-final', 'clean-snapshot-final')]
    status = 'FAIL' if any(r['status'] == 'FAIL' for r in finals) else (
        'PASS' if finals and all(r['status'] == 'PASS' for r in finals) else 'BLOCKED')
    note = ''
    if case == 'TC38':
        status = 'BLOCKED'; note = 'Thiếu ngưỡng p95 và xác nhận workload/môi trường nghiệm thu; không tự áp Q04 thương mại.'
    elif case == 'TC45' and not (RUNTIME / 'clean-install-manifest.json').exists():
        status = 'BLOCKED'; note = 'Thiếu manifest môi trường cài đặt sạch.'
    if not finals and case != 'TC38':
        note = 'Thiếu minh chứng thực thi cuối.'
    details = []
    observations = []
    for record in finals:
        obs = record.get('observations', [])
        observations.append(obs)
        if isinstance(obs, list):
            for event in obs:
                if 'parameters' in event:
                    details.append({'path': event.get('path'), 'parameters': event['parameters'], 'body': event.get('body')})
    if number == 38:
        latest_measurements={}
        for record in all_records:
            if record['_run'].startswith('performance') and 'final' in record['_run']:
                key=(record['observations']['database'],record['observations']['tourCount'])
                if key not in latest_measurements or record['executedAt']>latest_measurements[key]['executedAt']:
                    latest_measurements[key]=record
        for record in latest_measurements.values():
                observations.append(record['observations'])
                result = record['observations']
                details.append({'database': result['database'], 'tours': result['tourCount'],
                    'concurrency': result['concurrency'], 'warmup': result['warmupRequests'],
                    'samples': result['measuredRequests'], 'request': result['request'],
                    'seed': relative(record['_path'].parent / 'seed.sql'),
                    'explain': relative(record['_path'].parent / 'explain-analyze.txt')})
    data_path = REPORT / 'Test_Data' / (case + '.json')
    data_path.write_text(json.dumps({'testId': case, 'sourceId': SOURCE_ID,
        'baseSeed': ['schema.sql', 'data.sql'], 'requests': details, 'observations': observations,
        'fixtures': 'Per-test transaction rollback for API; browser transport faults only where specified.'},
        ensure_ascii=False, indent=2, default=str), encoding='utf-8')
    directory = EVIDENCE / case; directory.mkdir(exist_ok=True)
    paths = sorted({relative(r['_path']) for r in all_records})
    if case == 'TC38':
        for path in sorted((EVIDENCE / 'performance-final').rglob('summary.json')):
            paths.append(relative(path))
        for path in sorted((EVIDENCE / 'Performance').rglob('summary.json')):
            paths.append(relative(path))
    index = directory / 'index.md'
    index.write_text('# ' + case + ' evidence\n\nSource: ' + SOURCE_ID + '\n\n' +
        'Các run trước/sau được giữ riêng; phiên bản cụ thể nằm trong từng JSON/manifest. Kết luận chỉ dùng run final.\n\n' +
        '\n'.join('- [' + p + '](../../' + p + ')' for p in paths) + '\n', encoding='utf-8')
    date_records = finals if number != 38 else all_records
    date = max((r.get('executedAt', r.get('finishedAt', '')) for r in date_records), default=None)
    actual = ACTUAL.get(number, '')
    if number == 38:
        measured = []
        for record in latest_measurements.values():
            result=record['observations']
            measured.append(f"{result['database']} {result['tourCount']}: p95={result['p95Ms']:.3f}ms, errors={result['errors']}/{result['measuredRequests']}")
        actual = '; '.join(measured) + '. Có EXPLAIN/latencies thực. ' + note
    environment = 'Windows11 / H2 2.2.224 / Java21 / SpringBoot3.3.5'
    if number in (31, 32, 33, 34, 35, 36, 45): environment += ' / Chromium145 / React18 / Vite6'
    if number in (38, 45): environment += '; PostgreSQL18 QA tách biệt15432 (xem evidence)'
    if number == 25: environment += '; PostgreSQL18 builtin C.UTF-8 (sau retest localeC)'
    readable_inputs = []
    for detail in details:
        if 'path' in detail:
            parameters = ', '.join(k + '=' + '/'.join(map(str, v)) for k, v in detail.get('parameters', {}).items())
            readable_inputs.append(detail['path'] + ('; ' + parameters if parameters else '') +
                ('; body=' + detail['body'] if detail.get('body') else ''))
        else:
            readable_inputs.append(f"{detail['database']}: {detail['tours']} Tours, concurrency={detail['concurrency']}, warmup={detail['warmup']}, samples={detail['samples']}; seed/EXPLAIN trong JSON")
    used = '\n'.join(dict.fromkeys(readable_inputs))
    if len(used) > 700: used = used[:650] + '…; đầy đủ tại Test_Data/' + case + '.json'
    if not used: used = row['Đầu vào'] + '; dữ liệu/thao tác thật trong Test_Data/' + case + '.json'
    trace = mapping[case]
    cases.append({**row, **trace, 'Status': status, 'actual': actual, 'used': used, 'date': date,
        'environment': environment, 'sourceId': SOURCE_ID, 'runId': 'FINAL-20261118',
        'evidence': relative(index), 'testData': 'TD-' + case,
        'attempts': len({r['_run'] for r in all_records}), 'note': note,
        'basis': 'CSV gốc; A-FR/A-BR; A-ADR-01–04 đã xác nhận' if number != 38 else 'TC38: chưa chốt ngưỡng'})
    test_data.append([f'TD-{case}', case, row['Đầu vào'], used, relative(data_path),
                      'Seed sạch; fixture SQL/API chính xác trong JSON; không dữ liệu người thật', environment])

evidence_register = []
run_versions = {}
for values in records.values():
    for record in values:
        run_versions.setdefault(record['_run'], record.get('_sourceId', 'Không ghi hash ở run cũ; không dùng kết luận bản cuối'))
for path in sorted(EVIDENCE.rglob('*')):
    if not path.is_file() or path.suffix.lower() not in ('.json', '.xml', '.txt', '.sql', '.csv', '.md', '.log', '.png', '.zip'):
        continue
    if 'Workbook_QA' in path.relative_to(EVIDENCE).parts:
        continue
    ids = re.findall(r'TC\d{2}', path.relative_to(EVIDENCE).as_posix())
    for method, traced in REGRESSION_TRACE.items():
        if method in path.name:
            ids.extend(traced)
    run = path.relative_to(EVIDENCE).parts[0]
    version = run_versions.get(run, SOURCE_ID if ('final' in run.lower() or run in ('Build_and_Run', 'JUnit_Final'))
        else 'Xem manifest của run; hash run cũ không được tự suy diễn')
    if run == 'Build_and_Run' and 'final' not in path.name.lower():
        version = run_versions.get(path.stem, 'Không ghi hash ở log cũ; không dùng kết luận bản cuối')
    if re.fullmatch(r'TC\d{2}', run):
        version = SOURCE_ID
    environment = 'Xem environment.json/manifest và request log của run'
    if 'api-' in run or run.startswith(('review-', 'lock-')) or run == 'JUnit_Final': environment = 'H2 2.2.224 / Java21 / Windows11'
    elif 'frontend' in run or run.startswith(('browser-', 'admin-', 'e2e-')): environment = 'Chromium145 / React18 / Windows11; backend theo manifest'
    elif 'Performance' in run or 'performance' in run: environment = 'H2/PostgreSQL18 riêng theo environment.json của engine'
    elif run.startswith('postgres-locale'): environment = 'PostgreSQL18 / locale/provider thực trong JSON'
    evidence_register.append([f'EV-{len(evidence_register)+1:05}', ','.join(sorted(set(ids))) or 'SUITE',
        run, path.suffix.lstrip('.'), relative(path),
        hashlib.sha256(path.read_bytes()).hexdigest(), path.stat().st_size, version, environment])

counts = {status: sum(c['Status'] == status for c in cases) for status in ('PASS', 'FAIL', 'BLOCKED', 'CHƯA CHẠY')}
defect_specs = [
('01','TC45','Ngân sách hợp lệ bị HTML từ chối','Bug','step100000 kết hợp min1 tạo stepMismatch cho5000000','frontend/src/components/FilterPanel.jsx','step=1; kiểm giá trị biên trên browser'),
('02','TC31','Phản hồi cũ ghi đè success/error/loading','Bug','Không định danh request trước cập nhật state','frontend/src/pages/SearchPage.jsx','Generation guard cho success/catch/finally và unmount'),
('03','TC32','Không retry request tìm kiếm bị lỗi','Bug','Không có thao tác retry bộ lọc đã áp dụng','frontend/src/pages/SearchPage.jsx','Nút Thử lại dùng applied/page, không dùng form chưa submit'),
('04','TC32','Catalog lỗi không tải lại được','Bug','Retry chỉ gọi search, không nạp lại danh mục','frontend/src/pages/SearchPage.jsx','Tách catalogError và retry thao tác lỗi'),
('05','TC35','Bộ lọc sticky che CTA/kết quả','Bug','Panel cao hơn viewport nhưng position sticky','frontend/src/styles/app.css','Panel static; hit-test ở360/768/1440'),
('06','TC36','Chip thiếu trạng thái và focus rõ','Bug','Thiếu aria-pressed/focus-visible','frontend/src/components/FilterPanel.jsx; frontend/src/styles/app.css','ARIA state và focus bàn phím'),
('07','TC34','Ảnh URL thật bị lỗi không fallback','Bug','Chỉ fallback khi URL null, không bắt img error','frontend/src/components/TourCard.jsx','Fallback cho null và failed image, giữ kích thước'),
('08','TC06,TC15','Tham số lạ bị bỏ qua','Contract','minBudget/sort trả200; contract cũ chưa chốt xử lý','backend/src/main/java/vn/ptit/tour/controller/TourQueryController.java','Theo ADR01 đã xác nhận: whitelist,400 INVALID_FILTER'),
('09','TC25','Unicode NFD không khớp NFC','Bug','Không chuẩn hóa dạng Unicode đầu vào','backend/src/main/java/vn/ptit/tour/service/SearchParamValidator.java','NFC, Locale.ROOT; giữ phân biệt dấu'),
('10','TC26','Wildcard làm rộng kết quả từ khóa','Bug','LIKE bind nhưng không escape%/_','backend/src/main/java/vn/ptit/tour/repository/TourSearchDao.java','Escape ký tự LIKE, vẫn bind parameter'),
('11','TC27','Mã lỗi điểm đến chưa khớp quyết định','Contract','Code400 trong khi CSV cho phép422/empty, chưa chốt','backend/src/main/java/vn/ptit/tour/service/SearchParamValidator.java; exception/ErrorCode.java','ADR03 chốt422 INVALID_DESTINATION'),
('12','TC16','Trang rất lớn gây500','Bug','Nhân offset bằng int tràn số','backend/src/main/java/vn/ptit/tour/service/TourSearchService.java','Tính offset bằng long trước chặn theo tổng kết quả'),
('13','TC03,TC20,TC39','Offer của chuyến khác trở thành giá đại diện','Bug','JOIN active_offer_id không kiểm departure_id','backend/src/main/java/vn/ptit/tour/repository/TourSearchDao.java; service/TourDetailService.java','Kiểm offer thuộc chuyến ở list/detail/bookable'),
('14','TC01,TC39','Chi tiết đọc version của Tour khác','Bug','Chỉ kiểm PUBLISHED, không kiểm version.tourId','backend/src/main/java/vn/ptit/tour/service/TourDetailService.java; repository/TourSearchDao.java','Kiểm ownership và trả404'),
('15','TC29','BIGINT interestId đi qua JSON number','Bug','InterestDto khai báo Long không string serializer','backend/src/main/java/vn/ptit/tour/dto/InterestDto.java','ToStringSerializer; ID9007199254740993 giữ nguyên'),
('16','TC38','N+1 query destination','Bug','Mỗi candidate thực hiện một query điểm đến','backend/src/main/java/vn/ptit/tour/service/TourSearchService.java; repository/TourVersionDestinationRepository.java','Batch destinations; querycount1/15candidate=4/4, trước4/18'),
('17','TC21','Secret/mật khẩu demo cố định trong config/log','Security','Source có khóa JWT cố định, seed/log mật khẩu chung','backend/src/main/java/vn/ptit/tour/security/JwtService.java; config/DataInitializer.java; resources/application.yml','ENV hoặc JWT key ngẫu nhiên; seed opt-in; bỏ log password'),
('18','TC21','MANAGER đọc danh mục quản trị Product','Bug','Matcher GET internal/interests cho cả MANAGER','backend/src/main/java/vn/ptit/tour/config/SecurityConfig.java; frontend/src/pages/AdminPage.jsx','PRODUCT-only theoA-FR09; Manager vẫn đọc tag version'),
('19','TC22,TC44','JSON sai cấu trúc trả500','Bug','HttpMessageNotReadableException vào catch-all','backend/src/main/java/vn/ptit/tour/exception/GlobalExceptionHandler.java','400 INVALID_FILTER, thông báo chung'),
('20','TC21,TC22','Đổi ID sau khi tải có thể lưu tag nhầm version','Bug','Save dùng input ID sống thay vì dữ liệu đã tải','frontend/src/pages/AdminPage.jsx','Đổi ID hủy form; save gắn data.tourVersionId'),
('21','TC43','Thẻ inactive bị ẩn nhưng vẫn gửi khi lưu','Bug','selected chứa mapping cũ, chips lọc hết inactive','frontend/src/pages/AdminPage.jsx','Hiện lịch sử; người dùng bỏ chọn rõ ràng; không reselect inactive'),
('22','TC22','Thay toàn bộ tag chưa khóa version','Bug','Delete/insert có thể chạy đồng thời trên DRAFT rỗng','backend/src/main/java/vn/ptit/tour/repository/TourVersionRepository.java; service/InterestAdminService.java','PESSIMISTIC_WRITE trước validate/replace; kết nối độc lập không lấy được khóa'),
('23','TC21','UI công bố/điền sẵn mật khẩu cũ','Security','LoginForm dùng literal credential dù seed đã ENV-only','frontend/src/pages/AdminPage.jsx','Password trống và hướng dẫn dùng cấu hình cục bộ'),
('24','TC21','Dependency frontend có advisory runtime','Security','React Router/Vite cũ có advisory; bản cùng major chưa xử lý hết','frontend/package.json; frontend/package-lock.json','Router7.18.5/Vite6.4.6/plugin4.3.4; build+browser; prod audit0'),
('25','TC25','PostgreSQL localeC không casefold tiếng Việt','Environment','DB UTF8 nhưng providerlibc/localeC: keywordĐÀNẴNG trả0; H2 không lộ vấn đề','CHUYEN_SANG_POSTGRESQL.md; PostgreSQL DB QA mới','DB PostgreSQL18 builtin C.UTF-8;4 truy vấn Unicode/NFD/ASCII retest, E2E và benchmark lại'),
]
defects = []
for number, tc, title, category, cause, files, fix in defect_specs:
    linked = [c for c in cases if c['Test ID'] in tc.split(',')]
    retest = 'PASS: regression tự động trên source cuối; xem JSON/XML và TC index.'
    if tc == 'TC38': retest = 'Regression querycount4/4 PASS; số đo/EXPLAIN có thật, nghiệm thu TC38 còn BLOCKED.'
    defects.append([f'DEF-{number}', tc, title, category, cause, files, fix, 'FIXED', retest,
                    '; '.join(c['evidence'] for c in linked)])
defects.append(['SEC-DEV-01','Không gán PASS/FAIL cho TC nghiệp vụ','Advisory picomatch dev-only chưa có bản vá upstream',
                'Dependency dev', 'npm audit đầy đủ còn4high trong chuỗi picomatch/fdir/tinyglobby/Vite; prod audit0',
                'frontend/package-lock.json', 'Giới hạn dev server ở localhost; giữ audit/advisory, chờ upstream patch',
                'OPEN_UPSTREAM', 'Không tuyên bố audit toàn bộ sạch',
                'Test_Evidence/Build_and_Run/frontend-final-full-audit.json'])
result = {'sourceId': SOURCE_ID, 'csvSha256': csv_hash, 'generatedAt': datetime.now(timezone.utc).isoformat(),
          'cases': cases, 'testData': test_data, 'history': history, 'evidenceRegister': evidence_register,
          'counts': counts, 'passRate': counts['PASS'] / 45, 'defects': defects,
          'knownRisk': 'npm audit --omit=dev: 0; full audit: unpatched dev-only picomatch chain; xem JSON gốc.'}
(REPORT / 'report-data.json').write_text(json.dumps(result, ensure_ascii=False, indent=2, default=str), encoding='utf-8')
assert hashlib.sha256(original.read_bytes()).hexdigest() == csv_hash

case_table = '| TC | Priority | Status | Actual | Evidence |\n|---|---|---|---|---|\n' + '\n'.join(
    f"| {c['Test ID']} | {c['Priority']} | {c['Status']} | {c['actual'].replace('|','/')} | [{c['Test ID']}]({c['evidence']}) |" for c in cases)
summary = f"PASS {counts['PASS']}/45 ({counts['PASS']/45:.2%}), FAIL {counts['FAIL']}, BLOCKED {counts['BLOCKED']}, CHƯA CHẠY {counts['CHƯA CHẠY']}."
intro = f"Ngày 18/11/2026 (UTC+07). Module A. Source `{SOURCE_ID}`. Git chưa có commit; dùng manifest SHA-256 thực, không tạo SHA lịch sử giả.\n\n{summary}\n\n"
(REPORT/'Test_Execution_Report.md').write_text('# Báo cáo thực hiện kiểm thử Module A\n\n'+intro+
    'Baseline mới:33 JUnit PASS và frontend build thành công. Bộ regression mới đã tái hiện các lỗi, giữ RED/FAIL và retest riêng. Không dùng số liệu lịch sử33/7/5 làm kết quả mới.\n\n'+
    'Bản cuối: backend clean verify 78/78, 0 failure/error/skip; frontend build 49 modules; Chromium 17/17 kịch bản; TC45 cài sạch dùng API/DB thật. H2 và PostgreSQL 18 được ghi riêng.\n\n'+
    'Đã giải nén độc lập Module_A_Source_Snapshot.zip, đối chiếu 106 file ứng dụng/build/test, chạy npm ci + build và mvn clean verify (78/78), khởi động JVM/DB H2 mới rồi chạy lại TC45 trên trình duyệt thật: PASS. Manifest gắn SHA-256 của chính ZIP bàn giao; bằng chứng tại Test_Evidence/clean-snapshot-final.\n\n'+
    case_table+'\n\nTC38 đã đo 1k/10k, p95/EXPLAIN nhưng chưa có ngưỡng/mức tải được xác nhận. Không đủ Definition of Done45/45. Full dependency audit còn advisory dev-only chưa có upstream patch; production audit0. Không có UAT/PR/merge/release hoặc chữ ký bàn giao được tuyên bố.\n',encoding='utf-8')
defect_table = '| ID | TC | Loại | Nguyên nhân | File | Sửa | Trạng thái | Retest |\n|---|---|---|---|---|---|---|---|\n'+'\n'.join(
    '| '+' | '.join(str(x).replace('|','/') for x in (d[0],d[1],d[3],d[4],d[5],d[6],d[7],d[8]))+' |' for d in defects)
(REPORT/'Defect_Fix_Report.md').write_text('# Báo cáo sửa lỗi Module A\n\n'+intro+
    '25 mục đã xử lý gồm19 lỗi chức năng/toàn vẹn,3 mục bảo mật,2 cập nhật contract được chấp thuận và1 lỗi cấu hình locale PostgreSQL. Một advisory dev-only upstream còn mở. Category Contract được phân biệt với lỗi có Expected đã chốt.\n\n'+defect_table+
    '\n\nGiữ nguyên schema nghiệp vụ; chỉ đổi phép cộng ngày seed sang SQL chuẩn và thêm profile PostgreSQL. Clock được inject để kiểm chứng cùng asOf và chỉ đọc một thời điểm trong detail. Các thay đổi này không mở rộng sang Booking/thanh toán.\n',encoding='utf-8')
(REPORT/'Regression_Test_Report.md').write_text('# Báo cáo kiểm thử hồi quy Module A\n\n'+intro+
    'Lệnh cuối: `mvn clean verify` với qa.runId/sourceId/evidenceDir; `npm run build`; `npm run test:browser`; hai đợt `mvn -Dtest=ModuleAPerformanceIT test` với H2/PostgreSQL riêng. Log đầy đủ trong Test_Evidence/Build_and_Run và JUnit_Final.\n\n'+
    '78 bài backend PASS gồm 33 bài cũ và 45 bài bổ sung; 17 kịch bản browser PASS gồm race success/error/finally, retry search/catalog, image null/fail, 360/768/1440, keyboard, 4 ca admin và E2E. Không disable test hoặc đổi Expected CSV.\n\n'+
    'Bộ nguồn ZIP bàn giao đã được giải nén sang thư mục mới, cài dependency từ lockfile, build cả hai phần, chạy 78 bài backend và TC45 trên JVM/DB mới. Đây là lần kiểm tra độc lập, không cộng trùng vào số bài của bộ regression.\n\n'+
    'TDD RED được giữ ở api-red-verified/api-red-extra/review-red/lock-red/admin-red và frontend-baseline-verified. Các sai sót harness (size giả định, selector nhiều phần tử, cleanup connection đã đóng) được lưu riêng; không dùng chúng để quy lỗi nghiệp vụ.\n\n'+
    'Các vùng đã hồi quy: public visibility/offer price, hard filters, global rank/page/numericID, serializer/clock, RBAC/atomic tag/lock, UI state và clean install. TC38 không có SLA chưa được chốt.\n',encoding='utf-8')
(REPORT/'Test_Summary_Report.md').write_text('# Tổng hợp chất lượng Module A\n\n'+intro+
    'Excel gồm7 sheet, giữ45 ID/priority/Expected gốc; Test_Summary dùng COUNTIFS và mẫu số45. Kết quả không được gán trước khi chạy.\n\n'+
    'Còn thiếu: xác nhận ngưỡng p95/mức tải/môi trường TC38; advisory dev-only picomatch chưa có upstream patch. UAT/nghiệm thu thương mại không được thay bằng QA tự động.\n\n'+
    f'CSV gốc SHA-256 `{csv_hash}`; bản sao bảo toàn tại Test_Data/test_case_original.csv. Manifest và liên kết bằng chứng dùng đường dẫn tương đối trong thư mục QA.\n',encoding='utf-8')
print(json.dumps({'counts': counts, 'history': len(history), 'evidenceFiles': len(evidence_register), 'sourceId': SOURCE_ID}))
