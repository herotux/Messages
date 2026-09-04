# Minimal Commons extraction helper
from pathlib import Path
import re, shutil
import xml.etree.ElementTree as ET

ROOT=Path.cwd(); C=Path('/tmp/commons/commons/src/main'); DEST=ROOT/'app/src/main'
app=list((DEST/'kotlin').rglob('*.kt'))+list((DEST/'kotlin').rglob('*.java'))
texts=[p.read_text(errors='ignore') for p in app]
manifest=(DEST/'AndroidManifest.xml').read_text(errors='ignore') if (DEST/'AndroidManifest.xml').exists() else ''
src=list((C/'kotlin').rglob('*.kt'))+list((C/'java').rglob('*.java'))
by_pkg={}; decl={}
for p in src:
 t=p.read_text(errors='ignore'); m=re.search(r'^\s*package\s+([\w.]+)',t,re.M)
 if not m: continue
 pkg=m.group(1); by_pkg.setdefault(pkg,[]).append(p)
 # Match normal and extension declarations. The old expression only matched
 # `fun name(...)`; Commons contains many extension declarations such as
 # `fun Cursor.getLongValue(...)`, so those were silently omitted from the
 # extracted subset and surfaced as unresolved references in the app.
 decl[p]=set(re.findall(
     r'^\s*(?:(?:public|private|internal|protected|data|sealed|open|abstract|enum|value|annotation|suspend|inline|infix|operator|tailrec)\s+)*(?:class|interface|object|typealias|fun|val|var)\s+(?:[\w.<>?,\[\]]+\.)?([A-Za-z_]\w*)',
     t, re.M
 ))
sel=set(); q=[]
def add(p):
 if p not in sel: sel.add(p); q.append(p)
def imports(t): return re.findall(r'^\s*import\s+(org\.fossify\.commons\.[\w.]+)(?:\s+as\s+\w+)?',t,re.M)
def resolve(imp,t):
 a=imp.split('.'); pkg='.'.join(a[:-1]); name=a[-1]
 for p in by_pkg.get(pkg,[]):
  if name in decl[p] or p.stem==name: add(p)
for t in texts+[manifest]:
 for imp in imports(t): resolve(imp,t)
 for pkg in re.findall(r'^\s*import\s+(org\.fossify\.commons\.[\w.]+)\.\*',t,re.M):
  for p in by_pkg.get(pkg,[]):
   if any(re.search(r'\b'+re.escape(n)+r'\b',t) for n in decl[p]): add(p)

# SimpleActivity in Messages inherits BaseSimpleActivity, whose superclass
# EdgeToEdgeActivity is in the same Commons package and therefore has no import.
# Seed these roots explicitly; recursive imports below then pull their actual
# transitive dependencies without vendoring unrelated Commons activities.
for root in ('BaseSimpleActivity.kt', 'EdgeToEdgeActivity.kt'):
 for p in src:
  if p.stem == Path(root).stem and p.parent.name == 'activities':
   add(p)
   break

while q:
 p=q.pop(); t=p.read_text(errors='ignore')
 for imp in imports(t): resolve(imp,t)
 for pkg in re.findall(r'^\s*import\s+(org\.fossify\.commons\.[\w.]+)\.\*',t,re.M):
  for r in by_pkg.get(pkg,[]):
   if any(re.search(r'\b'+re.escape(n)+r'\b',t) for n in decl[r]): add(r)
for p in sel:
 base=C/'kotlin' if (C/'kotlin') in p.parents else C/'java'; out=DEST/('kotlin' if base.name=='kotlin' else 'java')/p.relative_to(base)
 out.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(p,out)
need=set()
for t in texts+[manifest]+[p.read_text(errors='ignore') for p in sel]: need |= set(re.findall(r'org\.fossify\.commons\.R\.(\w+)\.(\w+)',t))
res=list((C/'res').rglob('*')); copied=set(); skipped=set(); modified=set()

# Resources owned by Messages must not be replaced by Commons. In particular,
# bank logos are generated/maintained by tools/import_iranian_bank_logos.py.
# Only bank_sepah.png is intentionally kept as PNG; all other bank logo
# resources are app-owned as well and therefore must never be vendored from Commons.
def is_app_owned_bank_resource(p):
 return p.is_file() and p.stem.startswith('bank_')

# Android resource identity is type + resource name, not the complete filename.
# For example bank_sina.png and bank_sina.xml are both drawable/bank_sina.
existing_keys=set()
for p in (DEST/'res').rglob('*'):
 if p.is_file() and p.parent.name.startswith(('drawable','mipmap','layout','xml')):
  existing_keys.add((p.parent.name.split('-')[0], p.stem))

# Values resources share files, so conflicts must be removed item-by-item rather
# than treating the whole values XML file as one resource.
VALUES_TAGS={'item','string','color','dimen','style','attr','declare-styleable','plurals','string-array','integer-array','bool','integer','fraction'}
def values_keys(path):
 out=[]
 try:
  root=ET.parse(path).getroot()
  for child in root:
   name=child.attrib.get('name')
   if name:
    out.append((child.tag.split('}')[-1], name))
 except Exception:
  pass
 return out
for p in (DEST/'res').rglob('*'):
 if p.is_file() and p.parent.name.startswith('values'):
  for tag,name in values_keys(p):
   existing_keys.add((tag,name))

def copy_values(p):
 try:
  root=ET.parse(p).getroot()
 except Exception:
  if p.name in ('strings.xml','colors.xml'):
   skipped.add(p)
  return
 changed=False
 for child in list(root):
  tag=child.tag.split('}')[-1]
  name=child.attrib.get('name')
  if not name or (tag,name) not in existing_keys:
   if name:
    existing_keys.add((tag,name))
   continue
  root.remove(child)
  changed=True
 if len(root) == 0:
  skipped.add(p)
  return
 out=DEST/'res'/p.relative_to(C/'res')
 out.parent.mkdir(parents=True,exist_ok=True)
 ET.ElementTree(root).write(out,encoding='utf-8',xml_declaration=True)
 copied.add(p); modified.add(p)

def copyres(p):
 if is_app_owned_bank_resource(p):
  skipped.add(p); return
 out=DEST/'res'/p.relative_to(C/'res')
 key=(p.parent.name.split('-')[0], p.stem)
 if out.exists() or key in existing_keys:
  skipped.add(p); return
 out.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(p,out); copied.add(p); existing_keys.add(key)
for typ,name in need:
 for p in res:
  if not p.is_file(): continue
  if is_app_owned_bank_resource(p):
   continue
  if p.parent.name.startswith('values'):
   if any(tag == typ and nm == name for tag,nm in values_keys(p)): copy_values(p)
  elif typ in ('layout','drawable','mipmap','xml') and p.stem==name:
   copyres(p)
print('SELECTED',len(sel),'RESOURCES',len(copied),'SKIPPED_EXISTING_RESOURCES',len(skipped))
for p in sorted(sel): print('SRC',p.relative_to(C))
for p in sorted(copied): print('RES',p.relative_to(C/'res'))
for p in sorted(skipped): print('SKIP',p.relative_to(C/'res'))
