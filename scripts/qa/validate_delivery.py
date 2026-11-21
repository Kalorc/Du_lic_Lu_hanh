"""Read-only validation of final workbook/evidence, plus an immutable source snapshot."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
from collections import Counter
from datetime import datetime, timezone
import csv
import hashlib
import json
import re
import xml.etree.ElementTree as ET
import openpyxl

REPO = Path(__file__).resolve().parents[2]
REPORT = REPO.parent / 'Báo cáo Quản lý dự án phần mềm' / 'Kiểm thử và Đảm bảo chất lượng'
RUNTIME = REPO.parent / 'module-a-qa-runtime'
data = json.loads((REPORT/'report-data.json').read_text(encoding='utf-8'))
manifest = json.loads((RUNTIME/'final-source-manifest.json').read_text(encoding='utf-8'))
assert data['sourceId'] == manifest['sourceId']
for record in manifest['files']:
    assert hashlib.sha256((REPO/record['path']).read_bytes()).hexdigest() == record['sha256'], record['path']

file = REPORT/'Test_Cases_Module_A_Nhom09_Completed.xlsx'
book = openpyxl.load_workbook(file, data_only=False)
cached = openpyxl.load_workbook(file, data_only=True)
assert book.sheetnames == ['Test_Cases','Test_Data','Test_Execution_History','Defect_Log','RTM','Evidence_Register','Test_Summary']
with (REPORT/'test_case.csv').open(encoding='utf-8-sig',newline='') as handle:
    original = list(csv.DictReader(handle))
for row, expected in enumerate(original, 5):
    sheet = book['Test_Cases']
    assert sheet.cell(row,1).value == expected['Test ID']
    assert sheet.cell(row,4).value == expected['Expected']
    assert sheet.cell(row,5).value == expected['Priority']
    assert sheet.cell(row,23).value == expected['Đầu vào']
    assert sheet.cell(row,13).value is not None, expected['Test ID']
    assert sheet.cell(row,16).value == data['sourceId']
    assert (REPORT/data['cases'][row-5]['evidence']).is_file()
assert Counter(book['Test_Cases'].cell(row,6).value for row in range(5,50)) == Counter({'PASS':44,'BLOCKED':1})
assert book['Test_Cases']['F42'].value == 'BLOCKED'
assert cached['Test_Summary']['B6'].value == 45
assert cached['Test_Summary']['B7'].value == 44
assert cached['Test_Summary']['B8'].value == 0
assert cached['Test_Summary']['B9'].value == 1
assert abs(cached['Test_Summary']['B11'].value-44/45) < 1e-12
assert cached['RTM']['E42'].value == 'BLOCKED'
assert len(book['Test_Cases'].tables) == 1
assert len(book['Test_Cases'].conditional_formatting) > 0
assert book['Test_Cases'].freeze_panes == 'C5', book['Test_Cases'].freeze_panes
errors=[]
for sheet in cached:
    for cells in sheet:
        for cell in cells:
            if cell.data_type == 'e': errors.append((sheet.title,cell.coordinate,cell.value))
assert not errors,errors[:10]
missing=[]; altered=[]
for entry in data['evidenceRegister']:
    path=REPORT/entry[4]
    if not path.is_file(): missing.append(entry[4])
    elif hashlib.sha256(path.read_bytes()).hexdigest()!=entry[5]: altered.append(entry[4])
assert not missing,missing[:10]
assert not altered,altered[:10]
assert hashlib.sha256((REPORT/'test_case.csv').read_bytes()).hexdigest() == data['csvSha256']
assert (REPORT/'test_case.csv').read_bytes() == (REPORT/'Test_Data/test_case_original.csv').read_bytes()

ns={'m':'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
with ZipFile(file) as zipped:
    for name in zipped.namelist():
        if name.endswith('.xml'): ET.fromstring(zipped.read(name))

junit_totals=Counter()
for path in (RUNTIME/'junit-final').glob('TEST-*.xml'):
    root=ET.parse(path).getroot()
    for key in ('tests','failures','errors','skipped'): junit_totals[key]+=int(root.attrib.get(key,0))
assert junit_totals == Counter(tests=78, failures=0,errors=0,skipped=0),junit_totals
browser=json.loads((RUNTIME/'browser-final/playwright-results.json').read_text())
assert browser['stats']['expected']==17 and browser['stats']['unexpected']==0
locale_result=json.loads((RUNTIME/'postgres-locale-final/TC25/unicode-casefold.json').read_text(encoding='utf-8'))
assert locale_result['status']=='PASS' and locale_result['environment']['locale']=='C.UTF-8'
assert all(item['assertionPassed'] for item in locale_result['observations'])

# Verify the delivered ZIP was independently extracted, installed and executed.
fresh = RUNTIME/'clean-snapshot-final'
fresh_install=json.loads((fresh/'install-manifest.json').read_text(encoding='utf-8'))
assert fresh_install['sourceId']==data['sourceId']
assert fresh_install['readyStatus']==200 and fresh_install['verifiedSourceFiles']==106
fresh_junit=Counter()
for path in (fresh/'junit').glob('TEST-*.xml'):
    root=ET.parse(path).getroot()
    for key in ('tests','failures','errors','skipped'):
        fresh_junit[key]+=int(root.attrib.get(key,0))
assert fresh_junit==Counter(tests=78,failures=0,errors=0,skipped=0),fresh_junit
fresh_browser=json.loads((fresh/'browser/playwright-results.json').read_text(encoding='utf-8'))
assert fresh_browser['stats']['expected']==1 and fresh_browser['stats']['unexpected']==0
assert 'BUILD SUCCESS' in (fresh/'backend-build.log').read_text(encoding='utf-8')
assert '49 modules transformed' in (fresh/'frontend-build.log').read_text(encoding='utf-8')
assert 'added 72 packages' in (fresh/'npm-ci.log').read_text(encoding='utf-8')

secret=(RUNTIME/'private-demo-password.txt').read_bytes().strip()
leaks=[]
for path in (REPORT/'Test_Evidence').rglob('*'):
    if not path.is_file():continue
    if path.suffix in ('.json','.xml','.txt','.md','.csv','.log') and secret in path.read_bytes():leaks.append(str(path))
    elif path.suffix=='.zip':
        with ZipFile(path) as zipped:
            for entry in zipped.namelist():
                if entry.endswith(('.json','.trace','.network','.txt')) and secret in zipped.read(entry):leaks.append(str(path)+'!'+entry)
assert not leaks,leaks

snapshot=REPORT/'Module_A_Source_Snapshot.zip'
source_files=[REPO/entry['path'] for entry in manifest['files']]
source_files += [REPO/'README.md',REPO/'CHUYEN_SANG_POSTGRESQL.md']
source_files += [p for p in (REPO/'scripts/qa').rglob('*') if p.is_file() and '__pycache__' not in p.parts]
source_files += [REPO/'docs/qa/API_CONTRACT_Module_A.md',REPO/'docs/qa/module-a-contract-decisions.md']
extraction=RUNTIME/'clean-snapshot-final/source-extraction.json'
if extraction.exists():
    extracted=json.loads(extraction.read_text(encoding='utf-8'))
    assert hashlib.sha256(snapshot.read_bytes()).hexdigest()==extracted['snapshotSha256'],'Keep the independently tested snapshot immutable'
    with ZipFile(snapshot) as zipped:
        for entry in manifest['files']:
            assert hashlib.sha256(zipped.read(entry['path'])).hexdigest()==entry['sha256'],entry['path']
else:
    with ZipFile(snapshot,'w',ZIP_DEFLATED) as zipped:
        for path in sorted(set(source_files)):
            assert secret not in path.read_bytes(),path
            zipped.write(path,path.relative_to(REPO).as_posix())
        zipped.writestr('qa-source-manifest.json',json.dumps(manifest,ensure_ascii=False,indent=2))
snapshot_info={'sourceId':data['sourceId'],'snapshotSha256':hashlib.sha256(snapshot.read_bytes()).hexdigest(),
               'sourceFileCount':len([p for p in ZipFile(snapshot).namelist() if p!='qa-source-manifest.json']),
               'snapshot':snapshot.name,
               'git':'No commits at task start or finish; source SHA-256 identifies the tested application.'}
assert fresh_install['snapshotSha256']==snapshot_info['snapshotSha256']
(REPORT/'Source_Snapshot_Manifest.json').write_text(json.dumps(snapshot_info,ensure_ascii=False,indent=2),encoding='utf-8')
result={'verifiedAt':datetime.now(timezone.utc).isoformat(),'sourceId':data['sourceId'],'cases':data['counts'],
        'junit':dict(junit_totals),'browser':{'passed':17,'failed':0},'sheets':7,
        'evidenceFilesChecked':len(data['evidenceRegister']),'missingEvidence':missing,'alteredEvidence':altered,
        'formulaErrors':errors,'formulaRecalculation':'Artifact engine mutated/restored TC38; saved cached counts44/1 and ratio44/45 verified',
        'postgresUnicode':'TC25 real HTTP four queries PASS with builtin C.UTF-8; RED localeC evidence retained',
        'independentSourceInstall':{'verifiedSourceFiles':106,'junit':dict(fresh_junit),'tc45':'PASS',
                                    'npmCi':'PASS','frontendBuild':'PASS','snapshotSha256':fresh_install['snapshotSha256']},
        'csvSha256':data['csvSha256'],'currentDemoCredentialLeaks':0,'snapshot':snapshot_info,
        'acceptance':'Not100%; TC38 awaiting approved latency/workload threshold; dev-only advisory remains open'}
(REPORT/'Delivery_Validation.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=False))
