"""Shared settings reader for CLI helpers. Never prints credentials or API keys."""
import os
from pathlib import Path
import re
from urllib.parse import urlparse

PROJECT_ROOT = Path(__file__).resolve().parents[2]


def load_settings():
    result = {}
    for line in (PROJECT_ROOT / '.env').read_text(encoding='utf-8-sig').splitlines():
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        match = re.fullmatch(r'([A-Za-z_][A-Za-z0-9_]*)=(.*)', line)
        if not match:
            raise ValueError('Invalid .env line. Expected NAME=value.')
        key, value = match.groups()
        value = value.strip()
        if len(value) >= 2 and value[0] in "\"'" and value[-1] == value[0]:
            value = value[1:-1]
        result[key] = value
    return result


def mysql_connection(settings, executable):
    url = urlparse(settings['ENJOYTRIP_DB_URL'].removeprefix('jdbc:'))
    database = url.path.lstrip('/')
    if url.scheme != 'mysql' or not re.fullmatch(r'[A-Za-z0-9_]+', database):
        raise ValueError('Expected a single-database MySQL JDBC URL.')
    command = [executable, '--no-defaults', f'--host={url.hostname}', f'--port={url.port or 3306}',
               f'--user={settings["ENJOYTRIP_DB_USER"]}', '--default-character-set=utf8mb4', '--batch']
    environment = os.environ.copy()
    environment['MYSQL_PWD'] = settings['ENJOYTRIP_DB_PASSWORD']
    return command, environment, database
