"""Build and inspect the class submission ZIP without modifying the working .git directory."""
import argparse
from pathlib import Path, PurePosixPath
import re
import posixpath
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'scripts/db'))
from settings import load_settings

REQUIRED = ('EnjoyTrip_db.mwb', 'EnjoyTrip_db.sql', 'EnjoyTrip_db.png')
FORBIDDEN = {'.git', 'bin', 'target', 'node_modules', '__pycache__', '.vscode', '.idea', 'uploads', 'logs', 'temp'}


def included(path):
    relative = path.relative_to(ROOT)
    if any(part.lower() in FORBIDDEN for part in relative.parts):
        return False
    if path.name.startswith('.env') and path.name != '.env.example':
        return False
    return path.suffix.lower() not in ('.log', '.pyc', '.bak', '.backup', '.zip')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--names', nargs=2, default=['개발자', '사용자'])
    args = parser.parse_args()
    names = sorted(args.names)
    if any(not re.fullmatch(r'[가-힣]{2,10}', name) for name in names):
        parser.error('Expected two Korean names')
    destination = ROOT / 'submission' / ('EnjoyTrip_db__' + '_'.join(names) + '.zip')
    files = []
    for folder in ('src', 'docs', 'deliverables', 'scripts', '.mvn'):
        files.extend(p for p in (ROOT / folder).rglob('*') if p.is_file() and included(p))
    files.extend(ROOT / name for name in ('README.md', 'pom.xml', 'mvnw', 'mvnw.cmd', 'run-local.ps1', '.env.example', '.gitignore'))
    for name in REQUIRED:
        if not (ROOT / 'deliverables/db' / name).is_file():
            raise RuntimeError('Missing required file: ' + name)
    settings = load_settings()
    secrets = [value.encode('utf-8') for key, value in settings.items()
               if ('KEY' in key or 'PASSWORD' in key) and len(value) >= 12 and value != 'replace-me']
    # Inspect source bytes before writing. Small numeric DB passwords are protected by the strict file allowlist.
    for file in files:
        data = file.read_bytes()
        if any(secret in data for secret in secrets):
            raise RuntimeError('Private setting found in submission candidate: ' + str(file.relative_to(ROOT)))
    destination.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(destination, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for file in sorted(set(files)):
            archive.write(file, 'dbtrip/' + file.relative_to(ROOT).as_posix())
        # Keep mandatory files immediately discoverable as well as beside the showcase README.
        for name in REQUIRED:
            archive.write(ROOT / 'deliverables/db' / name, 'dbtrip/' + name)
    with zipfile.ZipFile(destination) as archive:
        if archive.testzip() is not None:
            raise RuntimeError('ZIP integrity check failed')
        names_in_zip = set(archive.namelist())
        for name in REQUIRED:
            assert 'dbtrip/' + name in names_in_zip
        for name in names_in_zip:
            parts = PurePosixPath(name).parts
            assert not any(part.lower() in FORBIDDEN for part in parts), name
            assert not any(part.startswith('.env') and part != '.env.example' for part in parts), name
        assert 'dbtrip/src/main/java/kr/co/enjoytrip/config/Database.java' in names_in_zip
        assert 'dbtrip/deliverables/db/README.md' in names_in_zip
        screenshots = [name for name in names_in_zip if name.startswith('dbtrip/deliverables/db/screenshots/') and name.endswith('.png')]
        assert len(screenshots) == 19, 'Expected exactly 19 refreshed screenshots'
        for readme in ('dbtrip/README.md', 'dbtrip/deliverables/db/README.md',
                       'dbtrip/deliverables/db/screenshots/README.md', 'dbtrip/deliverables/db/submission-checklist.md'):
            content = archive.read(readme).decode('utf-8-sig')
            for _, target in re.findall(r'\[([^\]]+)\]\(([^)]+)\)', content):
                if target.startswith(('https://', 'http://')):
                    continue
                local = target.split('#', 1)[0]
                resolved = posixpath.normpath(posixpath.join(posixpath.dirname(readme), local)) if local else readme
                assert resolved in names_in_zip or any(name.startswith(resolved.rstrip('/') + '/') for name in names_in_zip), (readme, target)
    print('Created:', destination.relative_to(ROOT))
    print('Verified: required MWB/SQL/PNG, source, 19 fresh screenshots, no .git/bin/private env/runtime files.')
    print('Bytes:', destination.stat().st_size)


if __name__ == '__main__':
    main()
