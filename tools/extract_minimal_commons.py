from pathlib import Path
import re, shutil

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
 decl[p]=set(re.findall(r'^\s*(?:public\s+|private\s+|internal\s+|protected\s+|data\s+|sealed\s+|open\s+|abstract\s+|enum\s+|value\s+|annotation\s+)*(?:class|interface|object|typealias|fun|val|var)\s+([A-Za-z_]\w*)',t,re.M))
sel=set(); q=[]
def add(p):
 if p not in sel: sel.add(p); q.append(p)
def imports(t): return re.findall(r'^\s*import\s+(org\.fossify\.commons\.[\w.]+)(?:\s+as\s+\w+)?',t,re.M)
def resolve(imp,t):
 a=imp.split('.'); pkg='.'.join(a[:-1]); name=a[-1]
 for p in by_pkg.get(pkg,[]):
  if name in decl[p] or p.stem==name: add(p)
# Seeds from app and manifest.
for t in texts+[manifest]:
 for imp in imports(t): resolve(imp,t)
 for pkg in re.findall(r'^\s*import\s+(org\.fossify\.commons\.[\w.]+)\.\*',t,re.M):
  for p in by_pkg.get(pkg,[]):
   if any(re.search(r'\b'+re.escape(n)+r'\b',t) for n in decl[p]): add(p)
while q:
 p=q.pop(); t=p.read_text(errors='ignore')
 for imp in imports(t): resolve(imp,t)
 for pkg in re.findall(r'^\s*import\s+(org\.fossify\.commons\.[\w.]+)\.\*',t,re.M):
  for r in by_pkg.get(pkg,[]):
   if any(re.search(r'\b'+re.escape(n)+r'\b',t) for n in decl[r]): add(r)
# Copy source closure.
for p in sel:
 base=C/'kotlin' if (C/'kotlin') in p.parents else C/'java'; out=DEST/('kotlin' if base.name=='kotlin' else 'java')/p.relative_to(base)
 out.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(p,out)
# Resource references from app + selected source.
need=set()
for t in texts+[manifest]+[p.read_text(errors='ignore') for p in sel]:
 need |= set(re.findall(r'org\.fossify\.commons\.R\.(\w+)\.(\w+)',t))
res=list((C/'res').rglob('*')); copied=set()
def copyres(p):
 out=DEST/'res'/p.relative_to(C/'res'); out.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(p,out); copied.add(p)
for typ,name in need:
 for p in res:
  if not p.is_file(): continue
  if typ in ('layout','drawable','mipmap','xml') and p.stem==name: copyres(p)
  elif p.parent.name.startswith('values') and re.search(r'<'+re.escape(typ)+r'\b[^>]*\bname=["\']'+re.escape(name)+r'["\']',p.read_text(errors='ignore')): copyres(p)
print('SELECTED',len(sel),'RESOURCES',len(copied))
for p in sorted(sel): print('SRC',p.relative_to(C))
for p in sorted(copied): print('RES',p.relative_to(C/'res'))
