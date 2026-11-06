"""Run UTF-8 SQL using the project's single .env without PowerShell pipe re-encoding."""
import argparse
from pathlib import Path
import shutil
import subprocess
from settings import load_settings, mysql_connection


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mysql', default=shutil.which('mysql'))
    parser.add_argument('--file', type=Path, action='append', required=True)
    args = parser.parse_args()
    if not args.mysql:
        parser.error('mysql client not found; specify --mysql path/to/mysql.exe')
    for path in args.file:
        if not path.is_file():
            parser.error(f'file not found: {path}')
    command, environment, database = mysql_connection(load_settings(), args.mysql)
    command.append('--show-warnings')
    for path in args.file:
        print(f'Executing {path} using project .env (database: {database})', flush=True)
        with path.open('rb') as sql:
            subprocess.run(command, stdin=sql, env=environment, check=True)


if __name__ == '__main__':
    main()
