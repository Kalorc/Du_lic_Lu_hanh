"""Identify source without inventing a commit for the initially untracked repository."""
from pathlib import Path
import hashlib
import json
import sys
from datetime import datetime, timezone

repo = Path(__file__).resolve().parents[2]
files = []
for parent in ('backend/src', 'frontend/src', 'frontend/tests', 'frontend/public', 'demo-assets', 'scripts/demo'):
    files.extend(p for p in (repo / parent).rglob('*') if p.is_file())
for name in ('backend/pom.xml', 'frontend/package.json', 'frontend/package-lock.json',
             'frontend/index.html', 'frontend/vite.config.js', 'frontend/playwright.config.js',
             '.editorconfig', '.gitignore', '.env.example'):
    files.append(repo / name)
entries = [{'path': p.relative_to(repo).as_posix(), 'sha256': hashlib.sha256(p.read_bytes()).hexdigest()}
           for p in sorted(files)]
identity = hashlib.sha256(json.dumps(entries, sort_keys=True).encode()).hexdigest()
result = {'sourceId': 'source-sha256:' + identity,
          'recordedAt': datetime.now(timezone.utc).isoformat(),
          'git': 'fix/module-a-qa; no commits; original source was untracked',
          'scope': 'Application/build/test files, local public assets and demo launch scripts. Reports and generated artifacts excluded.', 'files': entries}
output = Path(sys.argv[1])
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
print(result['sourceId'])
