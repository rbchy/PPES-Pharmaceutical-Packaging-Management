import re, sys
L = sys.argv[1]
reg = open(f'{L}/src/main/java/com/ppes/model/TableRegistry.java').read()
tables = {}; cur = None
for m in re.finditer(r'new TableDef\("(\w+)",[^;]*?"(\w+)"\)|new FieldDef\("(\w+)",\s*"[^"]*",\s*FieldType\.(\w+)\)((?:\s*\.\w+\([^)]*\))*)', reg):
    if m.group(1):
        cur = m.group(1); tables[cur] = {'pk': m.group(2), 'f': {}}
    else:
        tables[cur]['f'][m.group(3)] = (m.group(4), m.group(5))
scripts = ['PPES_extended_fields_schema.sql', 'PPES_extended_fields_2_schema.sql', 'PPES_carton_tracking_schema.sql',
           'PPES_lot_closing_schema.sql', 'PPES_shift_production_schema.sql', 'PPES_work_order_packing_schema.sql']
added = {}; created = set()
for s in scripts:
    sql = re.sub(r'--[^\n]*', '', open(f'{L}/{s}').read())
    for m in re.finditer(r'CREATE TABLE IF NOT EXISTS (\w+)', sql, re.I): created.add(m.group(1))
    for m in re.finditer(r'ALTER TABLE (\w+)(.*?);', sql, re.S | re.I):
        for c in re.findall(r'ADD COLUMN\s+(\w+)', m.group(2), re.I):
            added.setdefault(m.group(1), set()).add(c)
TYPES = {'TEXT': 'VARCHAR(150)', 'TEXTAREA': 'TEXT', 'PASSWORD': 'VARCHAR(255)', 'NUMBER': 'INT',
         'DECIMAL': 'DECIMAL(14,3)', 'DATE': 'DATE', 'TIME': 'TIME', 'DATETIME': 'DATETIME',
         'SELECT': 'VARCHAR(50)', 'FK': 'INT', 'LOOKUP': 'VARCHAR(150)'}
out = ['-- ============================================================',
       '-- PPES base schema (run FIRST, before the PPES_*_schema.sql scripts)',
       '-- Generated from src/main/java/com/ppes/model/TableRegistry.java.',
       '-- The original base script was never committed; this rebuilds the',
       '-- core tables WITHOUT the columns/tables that the extended-fields and',
       '-- module scripts add, so those scripts still apply cleanly on top.',
       '-- DatabaseSchemaIT checks every TableRegistry column exists afterwards.',
       '-- ============================================================', '',
       'CREATE DATABASE IF NOT EXISTS ppes CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;', 'USE ppes;', '']
for t, d in tables.items():
    if t in created: continue
    cols = [f'  `{d["pk"]}` INT AUTO_INCREMENT PRIMARY KEY']; fks = []
    for f, (ty, mods) in d['f'].items():
        if f in added.get(t, ()): continue
        req = ' NOT NULL' if '.required(true)' in mods else ''
        if t == 'users' and f == 'username': req += ' UNIQUE'
        cols.append(f'  `{f}` {TYPES[ty]}{req}')
        if ty == 'FK':
            ref = re.search(r'refTable\("(\w+)"\)', mods).group(1)
            fks.append(f'  FOREIGN KEY (`{f}`) REFERENCES `{ref}`(`{tables[ref]["pk"]}`)')
    out.append(f'CREATE TABLE IF NOT EXISTS `{t}` (\n' + ',\n'.join(cols + fks) + '\n) ENGINE=InnoDB;\n')
print('\n'.join(out))
print('-- skipped (created by scripts):', sorted(created & set(tables)), file=sys.stderr)
print('-- columns added by scripts:', {k: sorted(v) for k, v in added.items()}, file=sys.stderr)
