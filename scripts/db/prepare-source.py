"""Create a non-destructive, MySQL 8-compatible source loader; originals stay unchanged."""
import argparse
import hashlib
from pathlib import Path
import re


def prepare(schema: str, dump: str) -> str:
    schema = schema.replace('CREATE SCHEMA IF NOT EXISTS `trip_source`', 'CREATE SCHEMA `trip_source`')
    schema = re.sub(r'^DROP TABLE IF EXISTS .*?;\s*$', '', schema, flags=re.MULTILINE)
    schema = schema.replace(
        'INDEX `gugun_code_idx` (`gugun_code` ASC) VISIBLE,',
        'INDEX `gugun_code_idx` (`gugun_code` ASC) VISIBLE,\n'
        '  UNIQUE KEY `uq_guguns_sido_code` (`sido_code`, `gugun_code`),')
    schema = schema.replace('FOREIGN KEY (`si_gun_gu_code`)',
                            'FOREIGN KEY (`area_code`, `si_gun_gu_code`)')
    schema = schema.replace('REFERENCES `trip_source`.`guguns` (`gugun_code`)',
                            'REFERENCES `trip_source`.`guguns` (`sido_code`, `gugun_code`)')
    if 'UNIQUE KEY `uq_guguns_sido_code`' not in schema:
        raise ValueError('Source schema layout changed; review before generating SQL.')
    if 'FOREIGN KEY (`area_code`, `si_gun_gu_code`)' not in schema:
        raise ValueError('Source foreign key was not replaced.')
    if not re.search(r'^USE trip_source;', dump, re.MULTILINE):
        raise ValueError('Unexpected source database.')
    return ('-- Generated loader. Use an isolated MySQL instance; do NOT use --force.\n'
            '-- CREATE SCHEMA intentionally fails if trip_source already exists.\n'
            '-- Changes: composite gugun key/FK, no DROP TABLE, utf8mb4 connection.\n'
            + schema + '\n' + dump.replace('SET NAMES utf8 ', 'SET NAMES utf8mb4 '))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-dir', type=Path, default=Path('temp'))
    parser.add_argument('--output', type=Path, default=Path('target/db/source.sql'))
    args = parser.parse_args()
    paths = [args.source_dir / name for name in ('trip-schema.sql', 'trip-dump.sql')]
    inputs = [p.read_bytes() for p in paths]
    result = prepare(*(data.decode('utf-8-sig') for data in inputs))
    hashes = ''.join(f'-- SHA256 {p.name}: {hashlib.sha256(data).hexdigest()}\n'
                     for p, data in zip(paths, inputs))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(hashes + result, encoding='utf-8')
    print(f'Generated {args.output}; originals unchanged. Target schema: trip_source (new only).')


if __name__ == '__main__':
    main()
