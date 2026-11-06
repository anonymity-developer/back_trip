"""Prepare the dedicated enjoytrip_test DB on an explicitly chosen local MySQL port."""
import argparse
from pathlib import Path
import secrets
import shutil
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--port', type=int, required=True)
    parser.add_argument('--mysql', default=shutil.which('mysql'))
    parser.add_argument('--user', default='root', help='Administrative user for the isolated instance')
    parser.add_argument('--import-source', action='store_true', help='Requires already loaded trip_source')
    args = parser.parse_args()
    if not args.mysql or not 1 <= args.port <= 65535:
        parser.error('Specify a mysql client and valid port')
    root = Path(__file__).resolve().parents[2]
    resources = root / 'src/main/resources'
    env_file = root / 'target/db/app.env'
    previous = dict(line.split('=', 1) for line in env_file.read_text(encoding='utf-8').splitlines()
                    if '=' in line) if env_file.exists() else {}
    password = previous.get('ENJOYTRIP_DB_PASSWORD') or secrets.token_hex(24)
    admin_password = previous.get('ENJOYTRIP_ADMIN_PASSWORD') or secrets.token_hex(16)
    command = [args.mysql, '--no-defaults', '--host=127.0.0.1', f'--port={args.port}',
               f'--user={args.user}', '--default-character-set=utf8mb4', '--batch']

    def execute(sql):
        subprocess.run(command, input=sql.encode('utf-8'), check=True)

    schema = (resources / 'schema.sql').read_text(encoding='utf-8').replace('enjoytrip', 'enjoytrip_test')
    seed = (resources / 'db/seed/lookup-data.sql').read_text(encoding='utf-8').replace('USE enjoytrip;', 'USE enjoytrip_test;')
    execute(schema + '\n' + seed)
    # Do not change the password of an existing account. Reuse only this helper's local env file.
    execute(f"CREATE USER IF NOT EXISTS 'enjoytrip_verify'@'localhost' IDENTIFIED BY '{password}';\n"
            "GRANT SELECT,INSERT,UPDATE,DELETE ON enjoytrip_test.* TO 'enjoytrip_verify'@'localhost';")
    if args.import_source:
        sql = (resources / 'db/import/import-trip-source.sql').read_text(encoding='utf-8')
        execute(sql.replace('USE enjoytrip;', 'USE enjoytrip_test;'))
    uploads = root / 'target/db/uploads'
    uploads.mkdir(parents=True, exist_ok=True)
    env_file.parent.mkdir(parents=True, exist_ok=True)
    env_file.write_text('\n'.join([
        f'ENJOYTRIP_DB_URL=jdbc:mysql://127.0.0.1:{args.port}/enjoytrip_test?serverTimezone=Asia/Seoul&characterEncoding=UTF-8',
        'ENJOYTRIP_DB_USER=enjoytrip_verify', f'ENJOYTRIP_DB_PASSWORD={password}',
        'ENJOYTRIP_ADMIN_ID=admin_verify', 'ENJOYTRIP_ADMIN_EMAIL=admin-verify@example.test',
        f'ENJOYTRIP_ADMIN_PASSWORD={admin_password}', f'ENJOYTRIP_UPLOAD_DIR={uploads.as_posix()}',
        'ENJOYTRIP_TOUR_API_KEY=', 'ENJOYTRIP_WEATHER_API_KEY=', 'ENJOYTRIP_DATA_GO_API_KEY=',
        'ENJOYTRIP_SYNC_ENABLED=false', ''
    ]), encoding='utf-8')
    print(f'Prepared enjoytrip_test; private local settings: {env_file.relative_to(root)}. Existing .env unchanged.')


if __name__ == '__main__':
    main()
