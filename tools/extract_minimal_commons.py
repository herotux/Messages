# Minimal Commons extraction helper
from pathlib import Path
import re
import shutil
import xml.etree.ElementTree as ET

ROOT = Path.cwd()
C = Path('/tmp/commons/commons/src/main')
DEST = ROOT / 'app/src/main'

app = list((DEST / 'kotlin').rglob('*.kt')) + list((DEST / 'java').rglob('*.java'))
texts = [p.read_text(errors='ignore') for p in app]
manifest = (DEST / 'AndroidManifest.xml').read_text(errors='ignore') if (DEST / 'AndroidManifest.xml').exists() else ''
src = list((C / 'kotlin').rglob('*.kt')) + list((C / 'java').rglob('*.java'))

by_pkg = {}
decl = {}


def declared_names(t):
    return set(re.findall(
        r'^\s*(?:(?:public|private|internal|protected|data|sealed|open|abstract|enum|value|annotation|suspend|inline|infix|operator|tailrec)\s+)*'
        r'(?:class|interface|object|typealias|fun|val|var)\s+'
        r'(?:<[^\n>]+>\s+)?'
        r'(?:[\w.<>?,\[\]]+\.)?([A-Za-z_]\w*)',
        t,
        re.M,
    ))


for p in src:
    t = p.read_text(errors='ignore')
    m = re.search(r'^\s*package\s+([\w.]+)', t, re.M)
    if not m:
        continue
    pkg = m.group(1)
    by_pkg.setdefault(pkg, []).append(p)
    decl[p] = declared_names(t)

sel = set()
q = []


def add(p):
    if p not in sel:
        sel.add(p)
        q.append(p)


def imports(t):
    return re.findall(
        r'^\s*import\s+(org\.fossify\.commons\.[\w.]+)(?:\s+as\s+\w+)?',
        t,
        re.M,
    )


def resolve(imp, t):
    a = imp.split('.')
    pkg = '.'.join(a[:-1])
    name = a[-1]
    for p in by_pkg.get(pkg, []):
        if name in decl[p] or p.stem == name:
            add(p)


def resolve_same_package(p, t):
    m = re.search(r'^\s*package\s+([\w.]+)', t, re.M)
    if not m:
        return
    for candidate in by_pkg.get(m.group(1), []):
        if candidate == p:
            continue
        if any(re.search(r'\b' + re.escape(name) + r'\b', t) for name in decl[candidate]):
            add(candidate)


for t in texts + [manifest]:
    for imp in imports(t):
        resolve(imp, t)
    for pkg in re.findall(r'^\s*import\s+(org\.fossify\.commons\.[\w.]+)\.\*', t, re.M):
        for p in by_pkg.get(pkg, []):
            if any(re.search(r'\b' + re.escape(n) + r'\b', t) for n in decl[p]):
                add(p)

for root in ('BaseSimpleActivity.kt', 'EdgeToEdgeActivity.kt'):
    for p in src:
        if p.stem == Path(root).stem and p.parent.name == 'activities':
            add(p)
            break

while q:
    p = q.pop()
    t = p.read_text(errors='ignore')
    for imp in imports(t):
        resolve(imp, t)
    for pkg in re.findall(r'^\s*import\s+(org\.fossify\.commons\.[\w.]+)\.\*', t, re.M):
        for r in by_pkg.get(pkg, []):
            if any(re.search(r'\b' + re.escape(n) + r'\b', t) for n in decl[r]):
                add(r)
    resolve_same_package(p, t)

for p in sel:
    base = C / 'kotlin' if (C / 'kotlin') in p.parents else C / 'java'
    out = DEST / ('kotlin' if base.name == 'kotlin' else 'java') / p.relative_to(base)
    out.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(p, out)

res = [p for p in (C / 'res').rglob('*') if p.is_file()]
copied = set()
skipped = set()


def is_app_owned_bank_resource(p):
    # Bank logos are maintained as static source assets in Messages.
    # Never import another copy from Commons; this avoids XML/PNG collisions.
    return p.is_file() and p.stem.startswith('bank_')


def values_children(path):
    try:
        root = ET.parse(path).getroot()
        return root, list(root)
    except Exception:
        return None, []


def child_key(child):
    tag = child.tag.split('}')[-1]
    name = child.attrib.get('name')
    if tag == 'item' and child.attrib.get('type'):
        tag = child.attrib['type']
    return tag, name


def xml_refs(xml):
    # Includes normal refs and @+id refs. Attribute/resource namespace prefixes
    # are deliberately ignored because Android resource names are what matter.
    return re.findall(
        r'@\+?(string|color|dimen|drawable|mipmap|layout|xml|menu|style|font|array|plurals|integer|bool|fraction|id|raw|anim|animator)/([A-Za-z0-9_]+)',
        xml,
    )


def copyres(p):
    if is_app_owned_bank_resource(p):
        skipped.add(p)
        return False
    out = DEST / 'res' / p.relative_to(C / 'res')
    if out.exists():
        skipped.add(p)
        return False
    out.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(p, out)
    copied.add(p)
    return True


def merge_values_resource(p, wanted):
    src_root, children = values_children(p)
    if src_root is None:
        return False
    wanted_children = [c for c in children if child_key(c) in wanted]
    if not wanted_children:
        return False

    out = DEST / 'res' / p.relative_to(C / 'res')
    out.parent.mkdir(parents=True, exist_ok=True)

    if out.exists():
        dst_root, dst_children = values_children(out)
        if dst_root is None:
            return False
        existing = {child_key(c) for c in dst_children}
        changed = False
        for child in wanted_children:
            key = child_key(child)
            if key not in existing:
                dst_root.append(child)
                existing.add(key)
                changed = True
        if changed:
            ET.ElementTree(dst_root).write(out, encoding='utf-8', xml_declaration=True)
            copied.add(p)
        else:
            skipped.add(p)
        return changed

    root = ET.Element(src_root.tag, src_root.attrib)
    for child in wanted_children:
        root.append(child)
    ET.ElementTree(root).write(out, encoding='utf-8', xml_declaration=True)
    copied.add(p)
    return True


# Index resource definitions in Commons. This lets app code use normal
# org.fossify.messages.R references while the extractor still finds the
# corresponding Commons resource when the app copy is missing.
resource_defs = {}
for p in res:
    if is_app_owned_bank_resource(p):
        continue
    if p.parent.name.startswith('values'):
        _, children = values_children(p)
        for child in children:
            typ, name = child_key(child)
            if name:
                resource_defs.setdefault((typ, name), []).append(p)
    else:
        resource_defs.setdefault((p.parent.name.split('-')[0], p.stem), []).append(p)


# Find R references from BOTH the extracted Commons code and existing app code.
# This is important because some Commons resources are referenced by Messages
# classes after extraction and therefore never appear in selected Commons files.
need = set()
all_code_texts = texts + [p.read_text(errors='ignore') for p in sel]
for t in all_code_texts:
    need |= set(re.findall(r'(?:(?:org\.fossify\.commons\.)?R)\.(\w+)\.(\w+)', t))

# ViewBinding/DataBinding references may originate in app code while the layout
# itself lives in Commons. Only use a Commons layout if the app does not already
# have the corresponding default layout.
binding_layouts = set()
for t in all_code_texts:
    for cls in re.findall(r'\b([A-Z][A-Za-z0-9_]*)Binding\b', t):
        name = cls[:-7]
        snake = re.sub(r'(?<!^)([A-Z])', r'_\1', name).lower()
        binding_layouts.add(snake)

# Also catch imports of Commons R followed by bare R.<type>.<name> references.
# We intentionally scan all app code here: if a matching resource exists in
# Commons and is absent from app resources, it is a valid candidate to vendor.

queue = list(need)
seen = set()


def enqueue_xml(xml):
    for ref in xml_refs(xml):
        if ref not in seen:
            queue.append(ref)


def process_resource(key):
    typ, name = key
    if key in seen:
        return
    seen.add(key)

    # Existing app resource wins. Still inspect the Commons definition so any
    # dependencies needed by a generated binding/resource are discovered.
    candidates = resource_defs.get(key, [])
    for p in candidates:
        if p.parent.name.startswith('values'):
            src_root, children = values_children(p)
            if src_root is None:
                continue
            for child in children:
                if child_key(child) == key:
                    enqueue_xml(ET.tostring(child, encoding='unicode'))
            merge_values_resource(p, {key})
        elif p.stem == name:
            # If the destination already contains the resource, don't overwrite
            # it, but inspect the Commons XML for transitive dependencies.
            if p.suffix.lower() in ('.xml', '.svg'):
                try:
                    enqueue_xml(p.read_text(errors='ignore'))
                except Exception:
                    pass
            copyres(p)


while queue:
    process_resource(queue.pop())

# Copy missing layouts required by generated Binding classes. This catches
# ActivityAppLockBinding, DialogRenameItemsPatternBinding,
# TabRenameSimpleBinding, ItemContactWithNumberBinding, etc.
for name in sorted(binding_layouts):
    if (DEST / 'res' / 'layout' / f'{name}.xml').exists():
        continue
    candidates = [
        p for p in resource_defs.get(('layout', name), [])
        if p.parent.name.startswith('layout') and not is_app_owned_bank_resource(p)
    ]
    if not candidates:
        continue
    p = candidates[0]
    try:
        enqueue_xml(p.read_text(errors='ignore'))
    except Exception:
        pass
    copyres(p)

# Process dependencies discovered from binding layouts. We keep this separate
# from the first pass so a layout can introduce new R refs without rescanning
# every Commons resource repeatedly.
while queue:
    process_resource(queue.pop())

# Finally, resolve resource names referenced as bare R.* in app source against
# Commons definitions when the app currently has no matching resource. This is
# what fixes resources such as pinLockTitle/please_enter_pin that can be used by
# app code but originate in Commons.
app_res_root = DEST / 'res'
for t in texts:
    for typ, name in re.findall(r'\bR\.(\w+)\.(\w+)\b', t):
        if (typ, name) not in resource_defs:
            continue
        # If a same-name app resource already exists, leave it untouched.
        exists = False
        for p in app_res_root.rglob('*'):
            if not p.is_file() or is_app_owned_bank_resource(p):
                continue
            if p.parent.name.startswith('values'):
                _, children = values_children(p)
                if any(child_key(c) == (typ, name) for c in children):
                    exists = True
                    break
            elif p.stem == name and p.parent.name.split('-')[0] == typ:
                exists = True
                break
        if not exists:
            queue.append((typ, name))

while queue:
    process_resource(queue.pop())

print('SELECTED', len(sel), 'RESOURCES', len(copied), 'SKIPPED_EXISTING_RESOURCES', len(skipped))
for p in sorted(sel):
    print('SRC', p.relative_to(C))
for p in sorted(copied):
    print('RES', p.relative_to(C / 'res'))
for p in sorted(skipped):
    print('SKIP', p.relative_to(C / 'res'))
