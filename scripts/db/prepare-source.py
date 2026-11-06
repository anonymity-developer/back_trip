"""Create a non-destructive, MySQL 8-compatible source loader; originals stay unchanged."""
import argparse
import hashlib
from pathlib import Path
import re


def prepare(schema: str, dump: str) -> str:
    source = re.search(r'CREATE SCHEMA IF NOT EXISTS `([A-Za-z0-9_]+)`', schema)
    if not source or not re.search(r'^USE ' + re.escape(source.group(1)) + r';', dump, re.MULTILINE):
        raise ValueError('Expected matching source schema and dump database names.')
    # Normalize the loader's staging schema without changing the supplied files.
    schema = schema.replace('`' + source.group(1) + '`', '`trip_source`')
    dump = dump.replace('`' + source.group(1) + '`', '`trip_source`')
    dump = re.sub(r'^USE ' + re.escape(source.group(1)) + r';', 'USE trip_source;', dump, flags=re.MULTILINE)
    # Generated SQL does not need source-specific export header comments.
    schema = re.sub(r'^--[^\n]*(?:\n|$)', '', schema, flags=re.MULTILINE)
    dump = re.sub(r'^--[^\n]*(?:\n|$)', '', dump, flags=re.MULTILINE)
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
    parser.add_argument('--schema-file', type=Path, help='Source DDL; defaults to source-dir/trip-schema.sql')
    parser.add_argument('--dump-file', type=Path, help='Source data; defaults to source-dir/trip-dump.sql')
    parser.add_argument('--output', type=Path, default=Path('target/db/source.sql'))
    args = parser.parse_args()
    paths = [args.schema_file or args.source_dir / 'trip-schema.sql',
             args.dump_file or args.source_dir / 'trip-dump.sql']
    inputs = [p.read_bytes() for p in paths]
    result = prepare(*(data.decode('utf-8-sig') for data in inputs))
    hashes = ''.join(f'-- SHA256 source-{index}: {hashlib.sha256(data).hexdigest()}\n'
                     for index, data in enumerate(inputs, 1))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(hashes + result, encoding='utf-8')
    print(f'Generated {args.output}; originals unchanged. Target schema: trip_source (new only).')


if __name__ == '__main__':
    main()
