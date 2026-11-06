"""Run with MySQLWorkbench.exe --run-script <this-file> --quit-when-done.

Uses Workbench's native SQL parser/model writer; never connects to a database.
"""
from pathlib import Path
import json
import traceback
import grt

ROOT = Path(__file__).resolve().parents[2]
DESTINATION = ROOT / 'deliverables/db'


def export():
    grt.modules.Workbench.newDocument()
    document = grt.root.wb.doc
    document.info.caption = 'EnjoyTrip Database'
    document.info.author = ''
    document.info.project = 'dbtrip'
    model = document.physicalModels[0]
    result = grt.modules.MysqlSqlFacade.parseSqlScriptString(
        model.catalog, (ROOT / 'src/main/resources/schema.sql').read_text(encoding='utf-8'))
    if result != 0:
        raise RuntimeError('Workbench SQL parser reported errors')
    for schema in list(model.catalog.schemata):
        if not len(schema.tables):
            model.catalog.schemata.remove(schema)
    schema = model.catalog.schemata[0]
    if schema.name != 'enjoytrip' or len(schema.tables) != 12:
        raise RuntimeError('Expected exactly 12 application tables')
    diagram = model.addNewDiagram(0)
    diagram.name = 'EnjoyTrip - 12 tables'
    positions = {
        'sidos': (60, 60), 'guguns': (450, 60), 'contenttypes': (840, 60), 'news': (1230, 60),
        'attraction_images': (60, 420), 'attractions': (450, 420),
        'plan_stops': (840, 420), 'plans': (1230, 420),
        'password_reset_requests': (60, 1000), 'posts': (450, 1000),
        'hot_places': (840, 1000), 'users': (1230, 1000),
    }
    for table in schema.tables:
        x, y = positions[table.name]
        figure = diagram.placeTable(table, float(x), float(y))
        figure.name = table.name
        figure.color = '#D9EAF7' if table.name in ('sidos', 'guguns', 'contenttypes', 'attractions', 'attraction_images') else '#D9EAD3'
        figure.manualSizing = 1
        figure.width = 320
        figure.height = 80 + 24 * len(table.columns)
        figure.summarizeDisplay = 0
    for table in schema.tables:
        diagram.createConnectionsForTable(table)
    grt.modules.Workbench.saveModelAs(str(DESTINATION / 'EnjoyTrip_db.mwb'))
    grt.modules.Workbench.exportDiagramToPng(diagram, str(DESTINATION / 'EnjoyTrip_db.png'))
    facts = {'tables': {t.name: {'columns': len(t.columns), 'foreign_keys': len(t.foreignKeys)} for t in schema.tables},
             'figures': len(diagram.figures), 'relationships': len(diagram.connections), 'project': 'dbtrip'}
    (ROOT / 'target/db/workbench-export.json').write_text(json.dumps(facts, indent=2), encoding='utf-8')


try:
    export()
except Exception:
    (ROOT / 'target/db/workbench-export-error.txt').write_text(traceback.format_exc(), encoding='utf-8')
    raise
