"""Validate and package the actual 2026-11-21 Module A demo evidence."""
from pathlib import Path
from datetime import datetime, timezone, timedelta
from collections import Counter
from zipfile import ZipFile, ZIP_DEFLATED
import hashlib
import json
import os
import re
import shutil
import xml.etree.ElementTree as ET
from PIL import Image

REPO = Path(__file__).resolve().parents[2]
RUNTIME = REPO.parent / 'module-a-demo-runtime-20261121'
REPORT = REPO.parent / 'Báo cáo Quản lý dự án phần mềm' / 'Kiểm thử và Đảm bảo chất lượng' / 'Demo_Module_A_20261121'
EVIDENCE = REPORT / 'Test_Evidence'
REPORT.mkdir(parents=True, exist_ok=True)
EVIDENCE.mkdir(exist_ok=True)
source = json.loads((RUNTIME/'final-source-manifest.json').read_text(encoding='utf-8'))
state = json.loads((RUNTIME/'local-demo/state.json').read_text(encoding='utf-8-sig'))
build = Path(state['buildRoot'])
secret = os.environ.get('QA_DEMO_SECRET', '')
assert len(secret) >= 12, 'Provide private scanner input only through the process environment.'

def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def clean(value):
    value = re.sub(r'(?im)(Using generated security password:)\s*\S+', r'\1 [REDACTED]', value)
    value = re.sub(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', '[REDACTED_JWT]', value)
    return value.replace(secret, '[REDACTED]')

def copy_file(src, dest):
    src, dest = Path(src), Path(dest)
    dest.parent.mkdir(parents=True, exist_ok=True)
    if src.suffix.lower() in ('.log','.xml','.txt'):
        dest.write_text(clean(src.read_text(encoding='utf-8', errors='replace')), encoding='utf-8')
    else:
        shutil.copy2(src, dest)

def copy_tree(src, dest):
    src, dest = Path(src), Path(dest)
    for path in src.rglob('*'):
        if path.is_file(): copy_file(path, dest/path.relative_to(src))

copied_source_files = 0
for entry in source['files']:
    path = REPO/entry['path']
    assert sha(path) == entry['sha256'], entry['path']
    if entry['path'].startswith(('backend/','frontend/','demo-assets/')):
        assert sha(build/entry['path']) == entry['sha256'], 'Built copy differs: '+entry['path']
        copied_source_files += 1

totals = Counter()
for xml in (build/'backend/target/surefire-reports').glob('TEST-*.xml'):
    suite = ET.parse(xml).getroot()
    for key in ('tests','failures','errors','skipped'): totals[key] += int(suite.attrib.get(key, 0))
assert totals == Counter(tests=83, failures=0, errors=0, skipped=0), totals
browser = json.loads((RUNTIME/'browser-reviewed-final/playwright-results.json').read_text(encoding='utf-8'))
assert browser['stats']['expected'] == 26 and browser['stats']['unexpected'] == 0 and browser['stats']['skipped'] == 0
rehearsal = json.loads((RUNTIME/'browser-reviewed-final/demo-rehearsal.json').read_text(encoding='utf-8'))
assert rehearsal['status'] == 'PASS' and len(rehearsal['steps']) == 9
restart = json.loads((RUNTIME/'local-demo/restart-verification.json').read_text(encoding='utf-8-sig'))
assert restart['status'] == 'PASS' and restart['oldTokenStatus'] == 401 and len(restart['roleChecks']) == 4
rotation = json.loads((RUNTIME/'credential-reset-verification.json').read_text(encoding='utf-8-sig'))
assert rotation['status'] == 'PASS' and rotation['boundarySpacesPreserved']
assert not (RUNTIME/'private-demo-password.txt').exists()
for variant in ('dependency-audit-final.json','dependency-production-audit-final.json'):
    assert json.loads((RUNTIME/variant).read_text(encoding='utf-8-sig'))['metadata']['vulnerabilities']['total'] == 0

photos = json.loads((REPO/'demo-assets/credits-complete.json').read_text(encoding='utf-8'))['tours']
public_photos = json.loads((REPO/'frontend/public/images/credits.json').read_text(encoding='utf-8'))['tours']
assert len(photos) == 14 and len(public_photos) == 12
assert not {105,114}.intersection(p['tourId'] for p in public_photos)
assert len({p['detail']['sha256'] for p in photos}) == 14
image_count = 0
for photo in photos:
    assert photo['license'] and photo['sourcePage'] and photo['author']
    for variant in ('card','detail'):
        item = photo[variant]; path = REPO/item['file']
        assert sha(path) == item['sha256'] and path.stat().st_size == item['bytes']
        with Image.open(path) as image:
            image.load()
            assert image.size == (item['width'],item['height'])
        if photo['visibility'] == 'internal-seed':
            assert not (REPO/'frontend/public/images/tours'/path.name).exists()
            assert not (build/'frontend/dist/images/tours'/path.name).exists()
        image_count += 1
assert image_count == 28

copy_tree(build/'backend/target/surefire-reports', EVIDENCE/'Backend_Final/JUnit')
copy_tree(build/'backend/target/module-a-evidence', EVIDENCE/'Backend_Final/API')
copy_tree(RUNTIME/'browser-reviewed-final', EVIDENCE/'Browser_Final')
for name in ('ui-red','ui-gallery-red','privacy-red'):
    copy_tree(RUNTIME/name, EVIDENCE/'Before_After'/name)
for name in ('backend-baseline.log','backend-demo-red.log','backend-demo-green.log','browser-integration.log',
             'browser-reviewed-final.log','privacy-red.log','lifecycle-green-final.log',
             'dependency-audit-final.json','dependency-production-audit-final.json','credential-reset-verification.json'):
    copy_file(RUNTIME/name, EVIDENCE/'Logs'/name)
for name in ('backend-build.log','npm-ci.log','frontend-build.log','backend.stdout.log','backend.stderr.log',
             'frontend.stdout.log','frontend.stderr.log','state.json','restart-verification.json'):
    copy_file(RUNTIME/'local-demo'/name, EVIDENCE/'Runtime'/name)
copy_tree(RUNTIME/'document-audit', EVIDENCE/'Document_Audit')
copy_file(RUNTIME/'assets/tour-contact-sheet.jpg', EVIDENCE/'Images/tour-contact-sheet.jpg')
copy_file(RUNTIME/'final-source-manifest.json', REPORT/'Source_Manifest.json')

before = {x['path']:x['sha256'] for x in json.loads((RUNTIME/'baseline-source-manifest.json').read_text(encoding='utf-8'))}
selected = {entry['path'] for entry in source['files']}
selected.update(('README.md','CHUYEN_SANG_POSTGRESQL.md','HUONG_DAN_CHAY_VA_DEMO.md',
                 'KICH_BAN_DEMO_TRUOC_GIANG_VIEN.md','scripts/qa/source_manifest.py','scripts/qa/prepare_demo_delivery.py'))
changes=[]
for relative in sorted(selected):
    if before.get(relative) != sha(REPO/relative):
        changes.append({'path':relative,'change':'MODIFIED' if relative in before else 'ADDED',
                        'beforeSha256':before.get(relative),'afterSha256':sha(REPO/relative)})
(REPORT/'Changed_Files.json').write_text(json.dumps(changes,ensure_ascii=False,indent=2),encoding='utf-8')
changed_md = '# Danh sách file thay đổi — 21/11/2026\n\nSo với baseline đã giữ trước khi chỉnh sửa; không coi toàn bộ untracked là file mới.\n\n| File | Loại |\n|---|---|\n'
changed_md += '\n'.join(f"| `{c['path']}` | {c['change']} |" for c in changes)+'\n'
(REPORT/'Changed_Files.md').write_text(changed_md,encoding='utf-8')

now=datetime.now(timezone(timedelta(hours=7))).isoformat()
evidence_files=[]
for path in sorted(EVIDENCE.rglob('*')):
    if path.is_file(): evidence_files.append({'path':path.relative_to(REPORT).as_posix(),'sha256':sha(path),'bytes':path.stat().st_size})
(REPORT/'Evidence_Manifest.json').write_text(json.dumps(evidence_files,ensure_ascii=False,indent=2),encoding='utf-8')
run = {'verifiedAt':now,'sourceId':source['sourceId'],'sourceFilesChecked':len(source['files']),
       'copiedApplicationFilesChecked':copied_source_files,'junit':dict(totals),'browser':browser['stats'],
       'tourPhotos':14,'variantsDecodedAndHashed':28,'publicPhotos':12,'internalPhotos':2,
       'rehearsalSteps':9,'restart':restart,'credentialReset':rotation,
       'audit':{'full':0,'production':0},'evidenceFiles':len(evidence_files),
       'artifactHashes':{'backendJar':sha(RUNTIME/'local-demo/demo-backend.jar'),
                         'frontendAssets':{p.name:sha(p) for p in sorted((build/'frontend/dist/assets').iterdir())}},
       'versionBinding':'Raw JUnit/API sourceId working-tree retained. Source files in the built copy were compared with this actual manifest; no Git commit exists.',
       'scope':'Module A image/UI/demo regression; does not change the unresolved TC38 acceptance threshold.'}
(REPORT/'Verification_Result.json').write_text(json.dumps(run,ensure_ascii=False,indent=2),encoding='utf-8')

review = '''# Rà soát và kiểm thử sửa lỗi cuối

| Mục | Tái hiện | Sửa và kiểm định |
|---|---|---|
| PID/startup executable | Lệnh Stop thực tế từ chối Node vì state executable=null; test helper bị dừng ở PID đã tái sử dụng. | Ghi đường dẫn executable đã resolve; xác minh legacy theo creation time/command/executable; bỏ PID cũ, xóa record đã dừng. Test-DemoLifecycle PASS và Stop/Start thực tế PASS, không dừng dịch vụ khác. |
| Media/metadata chưa công khai | privacy-red thực tế thấy105/114 trong public credits và tải được ảnh. | Chuyển4 biến thể + manifest đầy đủ ra ngoài frontend. Privacy browser test kiểm cả URL ảnh và @fs; ảnh không được trả cho khách. |
| Credential driver | Driver cũ trim password; tệp input tạm tồn tại ngoài Git. | Driver đọc ENV chính xác; đổi credential có khoảng trắng biên, xác minh login200 và mật khẩu cũ401;26 browser tests PASS. Đã xóa tệp plaintext do đợt này tạo. |
| Tài liệu/kịch bản | Hướng dẫn PostgreSQL gợi nhập demo password ở profile không seed; bước5 chưa click anchor. | Bỏ hướng dẫn không hiệu lực; kịch bản/test đã click Xem lịch khởi hành và xác nhận hash URL. |

Rà soát độc lập phát hiện các mục trên; lần sửa cuối được xác minh bằng test và thực thi, không gán thêm phê duyệt reviewer, PR, merge, release hoặc UAT chưa có. Lượt tích hợp đầu có lỗi chờ dữ liệu trong test diễn tập (kiểm text DRAFT tĩnh trước khi chips xuất hiện); đã sửa điều kiện chờ, không đổi Expected nghiệp vụ hoặc che lỗi phần mềm.
'''
(REPORT/'Code_Review_Record.md').write_text(review,encoding='utf-8')
report_text = f'''# Kiểm chứng giao diện và demo Module A — 21/11/2026

Đã chạy bản demo local tại http://127.0.0.1:15173/tours, backend18080. Phiên bản nội dung: `{source['sourceId']}`; repository chưa có Git commit. Ngày giờ kiểm định: {now}.

| Kiểm tra | Kết quả thực tế |
|---|---|
| Baseline backend trước sửa |78 tests,0 failure/error/skip |
| Backend clean verify/package từ bản sao sạch |83 tests,0 failure/error/skip |
| Frontend npm ci/build |Thành công;51 modules; cài mới từ lockfile |
| Browser cuối |26/26 PASS,0 skipped; gồm17 hồi quy và9 ca mới |
| Ảnh |14 ảnh riêng/28 WebP;12 public,2 Tour chưa công khai giữ ảnh ngoài frontend; decode/hash/dimension và quyền nguồn đã kiểm |
| Danh sách/chi tiết |12 public Tours,2 trang; ảnh được kiểm trên tất cả12 detail;105/114 vẫn404 |
| Demo theo thứ tự |9 bước PASS;112=100%/4.900.000đ,113=100%/5.000.000đ,101=50%/4.200.000đ |
| Nội bộ |PRODUCT tạo/tắt danh mục và lưu DRAFT; public201 không đổi; PUBLISHED409,MANAGER403,anonymous401 |
| Restart/reset |Dừng/chạy lại bằng script PASS;4 vai trò đăng nhập200; token cũ401; DRAFT215 trở vềBEACH+FOOD;12 Tour/giá/thứ tự giữ đúng seed |
| Credential |DPAPI ngoài Git, process ENV cho test; đổi mật khẩu thực tế và xóa plaintext tạm; khoảng trắng biên được giữ nguyên |
| Dependency audit ngày21/11 |Full0,production0; số đo của đợt này, không sao chép trạng thái audit ngày18/11 |

Giao diện giữ React/Vite, tông xanh ngọc/nền sáng; card dùng ảnh640px/srcset, detail1280px; thiếu/lỗi ảnh dùng khung minh họa SVG có nhãn. Đã kiểm360/768/1440, focus/Tab/Enter, retry/race, trạng thái tải/rỗng/lỗi; xem ảnh chụp thực tế. Bộ lọc, điểm số, sort toàn cục, phân trang, vai trò và các ADR01–04 giữ nguyên. Gallery dùng cấu trúc media hiện có, không thêm API/booking/thanh toán.

Hướng dẫn chạy: [HUONG_DAN_CHAY_VA_DEMO.md](../../HUONG_DAN_CHAY_VA_DEMO.md). Kịch bản: [KICH_BAN_DEMO_TRUOC_GIANG_VIEN.md](../../KICH_BAN_DEMO_TRUOC_GIANG_VIEN.md). Thời lượng6 phút30 là dự kiến cho người thuyết trình; automation xác minh thứ tự/thao tác/kết quả, không giả thời lượng nói.

Hồ sơ bàn giao và log: [{REPORT.name}](<{REPORT.as_posix()}>). Xem Changed_Files.md/json, Source_Manifest.json, Evidence_Manifest.json, Verification_Result.json, Code_Review_Record.md và Test_Evidence. Minh chứng RED/FAIL và harness failure được giữ riêng, không dùng làm kết quả bản cuối. API JSON/JUnit giữ sourceId thô working-tree; manifest ngoài cùng được đối chiếu với từng file ứng dụng trong bản đã build, kèm SHA-256 JAR/dist, không sửa thô thành Git SHA giả.

Các tài liệu mã nguồn/cài đặt hiện hành đã được đọc và đối chiếu16 tệp. Bằng chứng cập nhật đặt riêng, không sửa hồ sơ45 ca cũ. TC38 vẫn thiếu tiêu chí tải/p95 được xác nhận; kết quả83/26 không được gọi là45/45 hoặc nghiệm thu thương mại.
'''
(REPO/'docs/demo').mkdir(exist_ok=True)
(REPO/'docs/demo/DEMO_VERIFICATION_REPORT.md').write_text(report_text,encoding='utf-8')
(REPORT/'DEMO_VERIFICATION_REPORT.md').write_text(report_text.replace('(../../HUONG_DAN_CHAY_VA_DEMO.md)',f'(<{(REPO/"HUONG_DAN_CHAY_VA_DEMO.md").as_posix()}>)').replace('(../../KICH_BAN_DEMO_TRUOC_GIANG_VIEN.md)',f'(<{(REPO/"KICH_BAN_DEMO_TRUOC_GIANG_VIEN.md").as_posix()}>)'),encoding='utf-8')

snapshot=REPORT/'Module_A_Demo_Source_20261121.zip'
package_files = set(selected)
package_files.update(('docs/qa/API_CONTRACT_Module_A.md','docs/qa/module-a-contract-decisions.md','docs/demo/DEMO_VERIFICATION_REPORT.md'))
with ZipFile(snapshot,'w',ZIP_DEFLATED) as zipped:
    for relative in sorted(package_files):
        zipped.write(REPO/relative,relative)
    zipped.writestr('demo-source-manifest.json',json.dumps(source,ensure_ascii=False,indent=2))
    zipped.writestr('demo-verification-result.json',json.dumps(run,ensure_ascii=False,indent=2))

leaks=[]
for root in (REPORT,REPO/'frontend/src',REPO/'backend/src',REPO/'scripts/demo'):
    for path in root.rglob('*'):
        if not path.is_file():continue
        if path.suffix.lower() in ('.json','.xml','.txt','.md','.log','.js','.jsx','.ps1','.yml','.properties','.html','.css','.sql'):
            if secret.encode() in path.read_bytes(): leaks.append(str(path))
        elif path.suffix.lower()=='.zip':
            with ZipFile(path) as zipped:
                for name in zipped.namelist():
                    if secret.encode() in zipped.read(name): leaks.append(str(path)+'!'+name)
assert not leaks, 'Credential scan failed; do not publish.'
for record in evidence_files:
    assert sha(REPORT/record['path']) == record['sha256'],record['path']
result={'sourceId':source['sourceId'],'snapshotSha256':sha(snapshot),'evidenceFilesChecked':len(evidence_files),
        'sourceFilesChecked':len(source['files']),'builtCopyFilesChecked':copied_source_files,'imagesVerified':28,
        'junit':dict(totals),'browserPassed':26,'credentialLeaks':0,'changedFiles':len(changes),
        'restart':'PASS','rehearsal':'PASS','verifiedAt':now}
(REPORT/'Delivery_Validation.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=False))
