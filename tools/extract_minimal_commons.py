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

def declared_names(t):
    return set(re.findall(
        r'^\s*(?:(?:public|private|internal|protected|data|sealed|open|abstract|enum|value|annotation|suspend|inline|infix|operator|tailrec)\s+)*'
        r'(?:class|interface|object|typealias|fun|val|var)\s+'
        r'(?:<[^\n>]+>\s+)?'
        r'(?:[\w.<>?,\[\]]+\.)?([A-Za-z_]\w*)',
        t, re.M
    ))

for p in src:
    t=p.read_text(errors='ignore'); m=re.search(r'^\s*package\s+([\w.]+)',t,re.M)
    if not m: continue
    pkg=m.group(1); by_pkg.setdefault(pkg,[]).append(p)
    decl[p]=declared_names(t)

sel=set(); q=[]
def add(p):
    if p not in sel: sel.add(p); q.append(p)

def imports(t): return re.findall(r'^\s*import\s+(org\.fossify\.commons\.[\w.]+)(?:\s+as\s+\w+)?',t,re.M)

def resolve(imp,t):
    a=imp.split('.'); pkg='.'.join(a[:-1]); name=a[-1]
    for p in by_pkg.get(pkg,[]):
        if name in decl[p] or p.stem==name:
            add(p)

def resolve_same_package(p,t):
    m=re.search(r'^\s*package\s+([\w.]+)',t,re.M)
    if not m: return
    for candidate in by_pkg.get(m.group(1),[]):
        if candidate == p: continue
        if any(re.search(r'\b'+re.escape(name)+r'\b',t) for name in decl[candidate]):
            add(candidate)

for t in texts+[manifest]:
    for imp in imports(t): resolve(imp,t)
    for pkg in re.findall(r'^\s*import\s+(org\.fossify\.commons\.[\w.]+)\.\*',t,re.M):
        for p in by_pkg.get(pkg,[]):
            if any(re.search(r'\b'+re.escape(n)+r'\b',t) for n in decl[p]): add(p)

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
    resolve_same_package(p,t)

for p in sel:
    base=C/'kotlin' if (C/'kotlin') in p.parents else C/'java'; out=DEST/('kotlin' if base.name=='kotlin' else 'java')/p.relative_to(base)
    out.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(p,out)

need=set()
for t in texts+[manifest]+[p.read_text(errors='ignore') for p in sel]:
    need |= set(re.findall(r'org\.fossify\.commons\.R\.(\w+)\.(\w+)',t))

# ViewBinding/DataBinding references do not appear as R.layout references.
# Derive the corresponding layout names, e.g. ActivityAppLockBinding -> activity_app_lock.
binding_layouts=set()
for t in [p.read_text(errors='ignore') for p in sel]:
    for cls in re.findall(r'\b([A-Z][A-Za-z0-9_]*)Binding\b',t):
        name=cls[:-7]
        snake=re.sub(r'(?<!^)([A-Z])',r'_\1',name).lower()
        binding_layouts.add(snake)

res=list((C/'res').rglob('*')); copied=set(); skipped=set()

def is_app_owned_bank_resource(p):
    return p.is_file() and p.stem.startswith('bank_')

VALUES_TAGS={'item','string','color','dimen','style','attr','declare-styleable','plurals','string-array','integer-array','bool','integer','fraction'}

def values_children(path):
    try:
        root=ET.parse(path).getroot()
        return root, list(root)
    except Exception:
        return None, []

def resource_key(p):
    return (p.parent.name.split('-')[0], p.stem)

def child_key(child):
    tag=child.tag.split('}')[-1]
    name=child.attrib.get('name')
    return (tag,name)

def merge_values_resource(p, wanted):
    src_root, children=values_children(p)
    if src_root is None: return
    wanted_children=[c for c in children if child_key(c) in wanted]
    if not wanted_children: return
    out=DEST/'res'/p.relative_to(C/'res'); out.parent.mkdir(parents=True,exist_ok=True)
    if out.exists():
        dst_root,dst_children=values_children(out)
        if dst_root is None: return
        existing={child_key(c) for c in dst_children}
        changed=False
        for child in wanted_children:
            key=child_key(child)
            if key not in existing:
                dst_root.append(child); existing.add(key); changed=True
        if changed:
            ET.ElementTree(dst_root).write(out,encoding='utf-8',xml_declaration=True)
            copied.add(p)
        else:
            skipped.add(p)
    else:
        root=ET.Element(src_root.tag, src_root.attrib)
        for child in wanted_children: root.append(child)
        ET.ElementTree(root).write(out,encoding='utf-8',xml_declaration=True)
        copied.add(p)

def copyres(p):
    if is_app_owned_bank_resource(p): skipped.add(p); return
    out=DEST/'res'/p.relative_to(C/'res')
    if out.exists(): skipped.add(p); return
    out.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(p,out); copied.add(p)

# Copy only the Commons resources actually referenced by the selected code.
for typ,name in need:
    for p in res:
        if not p.is_file() or is_app_owned_bank_resource(p): continue
        if p.parent.name.startswith('values'):
            merge_values_resource(p,{(typ,name)})
        elif typ in ('layout','drawable','mipmap','xml') and p.stem==name:
            copyres(p)

# Copy layouts required by generated ViewBinding/DataBinding classes.
for name in binding_layouts:
    for p in res:
        if p.is_file() and not is_app_owned_bank_resource(p) and p.stem==name and p.parent.name.startswith('layout'):
            copyres(p)

print('SELECTED',len(sel),'RESOURCES',len(copied),'SKIPPED_EXISTING_RESOURCES',len(skipped))
for p in sorted(sel): print('SRC',p.relative_to(C))
for p in sorted(copied): print('RES',p.relative_to(C/'res'))
for p in sorted(skipped): print('SKIP',p.relative_to(C/'res'))
