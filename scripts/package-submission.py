"""Build separate DB deliverables and runnable source ZIPs; never include prior back deliverables."""
import argparse
from pathlib import Path, PurePosixPath
import posixpath
import re
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'scripts/db'))
from settings import load_settings

REQUIRED = ('EnjoyTrip_db.mwb', 'EnjoyTrip_db.sql', 'EnjoyTrip_db.png')
FORBIDDEN = {'.git', 'bin', 'target', 'node_modules', '__pycache__', '.vscode', '.idea', 'uploads', 'logs', 'temp'}
SOURCE_FILES = (
    'pom.xml', 'mvnw', 'mvnw.cmd', 'run-local.ps1', '.env.example', '.gitignore',
    'scripts/Import-ProjectEnv.ps1', 'scripts/test.ps1',
    'scripts/db/settings.py', 'scripts/db/run-sql.py', 'scripts/db/setup-test.py',
    'scripts/db/http-smoke.py', 'scripts/db/prepare-source.py',
)


def included(path):
    relative = path.relative_to(ROOT)
    if any(part.lower() in FORBIDDEN for part in relative.parts):
        return False
    if path.name.startswith('.env') and path.name != '.env.example':
        return False
    return path.suffix.lower() not in ('.log', '.pyc', '.bak', '.backup', '.zip')


def collect_db():
    folder = ROOT / 'deliverables/db'
    # Flatten only this submission's DB folder. No src, docs/back or deliverables/back traversal.
    return {p.relative_to(folder).as_posix(): p for p in folder.rglob('*') if p.is_file() and included(p)}


def collect_source():
    result = {}
    for folder in ('src', '.mvn'):
        for file in (ROOT / folder).rglob('*'):
            if file.is_file() and included(file):
                result['dbtrip/' + file.relative_to(ROOT).as_posix()] = file
    for name in SOURCE_FILES:
        result['dbtrip/' + name] = ROOT / name
    result['dbtrip/README.md'] = ROOT / 'docs/db/07-source-package.md'
    return result


def validate(archive_path, kind):
    with zipfile.ZipFile(archive_path) as archive:
        if archive.testzip() is not None:
            raise RuntimeError('ZIP integrity check failed')
        names = set(archive.namelist())
        for name in names:
            parts = PurePosixPath(name).parts
            assert not any(part.lower() in FORBIDDEN for part in parts), name
            assert 'back' not in parts, 'Previous backend folder: ' + name
            assert not any(part.startswith('.env') and part != '.env.example' for part in parts), name
        if kind == 'db':
            assert all(name in names for name in REQUIRED)
            assert 'README.md' in names and 'reference/local-run.md' in names
            assert sum(name.startswith('screenshots/') and name.endswith('.png') for name in names) == 19
            assert not any(name.endswith('.java') or name.endswith('/pom.xml') or name.startswith(('src/', 'docs/', 'deliverables/')) for name in names)
            for extension in ('.mwb',):
                assert sum(name.endswith(extension) for name in names) == 1, 'Duplicate mandatory model'
        else:
            assert 'dbtrip/src/main/java/kr/co/enjoytrip/config/Database.java' in names
            assert 'dbtrip/pom.xml' in names and 'dbtrip/README.md' in names
            assert all('dbtrip/' + name in names for name in SOURCE_FILES)
            assert not any(part in ('docs', 'deliverables', 'screenshots', 'reference') for name in names for part in PurePosixPath(name).parts)
            assert not any(name.endswith('.mwb') or name.endswith('EnjoyTrip_db.sql') for name in names)
        # Every packaged Markdown file must resolve its own local links, with no other ZIP required.
        for name in names:
            if not name.endswith('.md'):
                continue
            content = archive.read(name).decode('utf-8-sig')
            for _, target in re.findall(r'\[([^\]]+)\]\(([^)]+)\)', content):
                if target.startswith(('https://', 'http://')):
                    continue
                local = target.split('#', 1)[0]
                resolved = posixpath.normpath(posixpath.join(posixpath.dirname(name), local)) if local else name
                assert resolved in names or any(candidate.startswith(resolved.rstrip('/') + '/') for candidate in names), (name, target)


def write_zip(destination, files, kind, secrets):
    temporary = destination.with_suffix('.zip.tmp')
    try:
        with zipfile.ZipFile(temporary, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
            for name, source in sorted(files.items()):
                data = source.read_bytes()
                if any(secret in data for secret in secrets):
                    raise RuntimeError('Private setting found in submission candidate: ' + str(source.relative_to(ROOT)))
                archive.writestr(name, data)
        validate(temporary, kind)
        return temporary
    except Exception:
        temporary.unlink(missing_ok=True)
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--names', nargs=2, default=['개발자', '사용자'])
    parser.add_argument('--output-dir', type=Path, default=ROOT / 'submission/db-final')
    args = parser.parse_args()
    names = sorted(args.names)
    if any(not re.fullmatch(r'[가-힣]{2,10}', name) for name in names):
        parser.error('Expected two Korean names')
    basename = 'EnjoyTrip_db__' + '_'.join(names)
    folder = args.output_dir.resolve()
    folder.mkdir(parents=True, exist_ok=True)
    settings = load_settings()
    secrets = [value.encode('utf-8') for key, value in settings.items()
               if ('KEY' in key or 'PASSWORD' in key) and len(value) >= 12 and value != 'replace-me']
    outputs = [(folder / (basename + '.zip'), collect_db(), 'db'),
               (folder / (basename + '_소스코드.zip'), collect_source(), 'source')]
    prepared = []
    try:
        for destination, files, kind in outputs:
            prepared.append((write_zip(destination, files, kind, secrets), destination))
        for temporary, destination in prepared:
            temporary.replace(destination)
            print(f'Created: {destination.relative_to(ROOT)} ({destination.stat().st_size:,} bytes)')
    finally:
        for temporary, _ in prepared:
            temporary.unlink(missing_ok=True)
    print('Verified: DB-only deliverables and runnable source are separate; all Markdown links resolve in each ZIP.')
    print('Excluded from both: prior back folders, .git/bin, private env, runtime data and build output.')


if __name__ == '__main__':
    main()
