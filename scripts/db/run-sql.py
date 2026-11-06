"""Run UTF-8 SQL without PowerShell pipe re-encoding. Port is intentionally required."""
import argparse
from pathlib import Path
import shutil
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mysql', default=shutil.which('mysql'))
    parser.add_argument('--host', default='127.0.0.1')
    parser.add_argument('--port', type=int, required=True)
    parser.add_argument('--user', default='root')
    parser.add_argument('--file', type=Path, action='append', required=True)
    args = parser.parse_args()
    if not args.mysql:
        parser.error('mysql client not found; specify --mysql path/to/mysql.exe')
    if not 1 <= args.port <= 65535:
        parser.error('port must be between 1 and 65535')
    for path in args.file:
        if not path.is_file():
            parser.error(f'file not found: {path}')
    command = [args.mysql, '--no-defaults', f'--host={args.host}', f'--port={args.port}',
               f'--user={args.user}', '--default-character-set=utf8mb4', '--batch', '--show-warnings']
    # mysql reads MYSQL_PWD from the inherited environment if authentication is required.
    # Never print its value or include it in the process command line.
    for path in args.file:
        print(f'Executing {path} on {args.host}:{args.port}', flush=True)
        with path.open('rb') as sql:
            subprocess.run(command, stdin=sql, check=True)


if __name__ == '__main__':
    main()
