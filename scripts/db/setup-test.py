"""Prepare DAO test data using the project's single .env (no separate account/settings file)."""
import argparse
import shutil
import subprocess
from settings import PROJECT_ROOT, load_settings, mysql_connection


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mysql', default=shutil.which('mysql'))
    parser.add_argument('--with-attractions', action='store_true', help='Copy missing attractions from the configured app DB')
    args = parser.parse_args()
    if not args.mysql:
        parser.error('mysql client not found')
    command, environment, database = mysql_connection(load_settings(), args.mysql)
    if database == 'enjoytrip_test':
        parser.error('The shared .env must point to the app DB; enjoytrip_test is reserved for DAO tests')
    resources = PROJECT_ROOT / 'src/main/resources'
    schema = (resources / 'schema.sql').read_text(encoding='utf-8').replace('enjoytrip', 'enjoytrip_test')
    seed = (resources / 'db/seed/lookup-data.sql').read_text(encoding='utf-8').replace('USE enjoytrip;', 'USE enjoytrip_test;')
    subprocess.run(command, input=(schema + '\n' + seed).encode('utf-8'), env=environment, check=True)
    if args.with_attractions:
        columns = 'content_id,title,content_type,area_code,sigungu_code,address,description,latitude,longitude,image_url,source_updated_at'
        select_columns = ','.join('s.' + column for column in columns.split(','))
        sql = (f'INSERT INTO enjoytrip_test.attractions({columns}) SELECT {select_columns} '
               f'FROM `{database}`.attractions s LEFT JOIN enjoytrip_test.attractions t ON t.content_id=s.content_id '
               'WHERE t.id IS NULL; SELECT ROW_COUNT() AS added_test_attractions;')
        subprocess.run(command, input=sql.encode('utf-8'), env=environment, check=True)
    print('Prepared enjoytrip_test using shared .env credentials. No .env or account was created/changed.')


if __name__ == '__main__':
    main()
