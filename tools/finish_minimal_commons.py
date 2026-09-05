#!/usr/bin/env python3
"""Finish vendored Commons integration after extraction.

Fixes module-namespace differences, generated binding layouts, transitive
resources, and Kotlin 2.4 compatibility for the legacy Commons context receiver.
"""
from pathlib import Path
import re
import shutil
import xml.etree.ElementTree as ET

ROOT = Path.cwd()
APP = ROOT / "app/src/main"
COMMONS = Path("/tmp/commons/commons/src/main")
RES = COMMONS / "res"
APP_RES = APP / "res"


def read(p):
    return p.read_text(errors="ignore")


def is_bank(p):
    return p.is_file() and p.stem.startswith("bank_")


def camel_binding(layout_name):
    return "".join(part[:1].upper() + part[1:] for part in layout_name.split("_")) + "Binding"


def values_children(p):
    try:
        root = ET.parse(p).getroot()
        return root, list(root)
    except Exception:
        return None, []


def key(child):
    tag = child.tag.split("}")[-1]
    name = child.attrib.get("name")
    if tag == "item" and child.attrib.get("type"):
        tag = child.attrib["type"]
    if tag == "declare-styleable":
        return "styleable", name
    return tag, name


def refs(xml):
    return re.findall(
        r"@\+?(string|color|dimen|drawable|mipmap|layout|xml|menu|style|font|array|plurals|integer|bool|fraction|id|raw|anim|animator)/([A-Za-z0-9_]+)",
        xml,
    )


def merge_value(p, wanted):
    src_root, src_children = values_children(p)
    if src_root is None:
        return
    selected = [c for c in src_children if key(c) in wanted]
    if not selected:
        return
    out = APP_RES / p.relative_to(RES)
    out.parent.mkdir(parents=True, exist_ok=True)
    if out.exists():
        dst_root, dst_children = values_children(out)
        if dst_root is None:
            return
        existing = {key(c) for c in dst_children}
        changed = False
        for child in selected:
            k = key(child)
            if k not in existing:
                dst_root.append(child)
                existing.add(k)
                changed = True
            elif k[0] == "styleable":
                dst = next(c for c in dst_root if key(c) == k)
                have = {key(x) for x in dst}
                for nested in child:
                    nk = key(nested)
                    if nk not in have:
                        dst.append(nested)
                        have.add(nk)
                        changed = True
        if changed:
            ET.ElementTree(dst_root).write(out, encoding="utf-8", xml_declaration=True)
    else:
        root = ET.Element(src_root.tag, src_root.attrib)
        for child in selected:
            root.append(child)
        ET.ElementTree(root).write(out, encoding="utf-8", xml_declaration=True)


def copy_resource(p):
    if is_bank(p):
        return
    out = APP_RES / p.relative_to(RES)
    if out.exists():
        return
    out.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(p, out)


# The app namespace is org.fossify.messages, so generated bindings and R live
# there. Commons source imports must be rewritten accordingly.
for p in list(APP.rglob("*.kt")) + list(APP.rglob("*.java")):
    s = read(p)
    ns = s.replace("org.fossify.commons.databinding", "org.fossify.messages.databinding")
    ns = ns.replace("org.fossify.commons.R", "org.fossify.messages.R")
    if p.as_posix().startswith("app/src/main/kotlin/org/fossify/commons/") and re.search(r"\bR\.", ns):
        if "import org.fossify.messages.R" not in ns:
            ns = re.sub(r"(^\s*package\s+[^\n]+\n)", r"\1\nimport org.fossify.messages.R\n", ns, count=1, flags=re.M)
    if ns != s:
        p.write_text(ns)

# Migrate the one legacy Commons context receiver used by this Commons commit.
p = APP / "kotlin/org/fossify/commons/extensions/SharedPreferencesProducerExtensions.kt"
if p.exists():
    s = read(p)
    s = s.replace("context (SharedPreferences)", "context(sharedPreferences: SharedPreferences)")
    s = s.replace("registerOnSharedPreferenceChangeListener(sharedPreferencesListener)", "sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)")
    s = s.replace("unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)", "sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)")
    p.write_text(s)

# Resource index, including values resources and all layout variants.
resource_defs = {}
for p in [p for p in RES.rglob("*") if p.is_file() and not is_bank(p)]:
    if p.parent.name.startswith("values"):
        _, children = values_children(p)
        for c in children:
            k = key(c)
            if k[1]:
                resource_defs.setdefault(k, []).append(p)
    else:
        resource_defs.setdefault((p.parent.name.split("-")[0], p.stem), []).append(p)

# Find binding classes actually referenced by the app or vendored Commons.
texts = [read(p) for p in APP.rglob("*.kt") if p.is_file()]
binding_classes = set()
for s in texts:
    binding_classes.update(re.findall(r"\b([A-Z][A-Za-z0-9_]*)Binding\b", s))

# Generated binding name is deterministic from the XML filename. Copy every
# configuration variant of matching layouts, while preserving app-owned layouts.
for p in sorted(resource_defs.get(("layout", "__never__"), [])):
    pass
layout_bases = {}
for p in resource_defs.get(("layout", "__never__"), []):
    layout_bases.setdefault(p.stem, []).append(p)
for p in [p for p in RES.rglob("*") if p.is_file() and p.parent.name.startswith("layout") and not is_bank(p)]:
    layout_bases.setdefault(p.stem, []).append(p)
for stem, files in layout_bases.items():
    if camel_binding(stem) not in binding_classes:
        continue
    for p in files:
        out = APP_RES / p.relative_to(RES)
        if out.exists():
            continue
        copy_resource(p)
        try:
            for typ, name in refs(read(p)):
                resource_defs.get((typ, name), [])
        except Exception:
            pass

# Collect R references from all current app sources and resolve them against Commons.
need = set()
for p in APP.rglob("*.kt"):
    s = read(p)
    need.update(re.findall(r"\bR\.(\w+)\.(\w+)\b", s))
for p in APP.rglob("*.java"):
    s = read(p)
    need.update(re.findall(r"\bR\.(\w+)\.(\w+)\b", s))

queue = list(need)
seen = set()
while queue:
    typ, name = queue.pop()
    if (typ, name) in seen:
        continue
    seen.add((typ, name))
    for p in resource_defs.get((typ, name), []):
        if p.parent.name.startswith("values"):
            merge_value(p, {(typ, name)})
            _, children = values_children(p)
            for c in children:
                if key(c) == (typ, name):
                    queue.extend(refs(ET.tostring(c, encoding="unicode")))
        else:
            if p.suffix.lower() in (".xml", ".svg"):
                queue.extend(refs(read(p)))
            copy_resource(p)

# IDs are usually introduced with @+id inside layouts; ensure the defining
# layout is present when code references the id directly.
for typ, name in list(seen):
    if typ != "id":
        continue
    for p in RES.rglob("*"):
        if not p.is_file() or not p.parent.name.startswith("layout") or is_bank(p):
            continue
        if re.search(r"@\+id/" + re.escape(name) + r"\b", read(p)):
            copy_resource(p)

# Re-run dependency discovery after binding layouts were copied.
for p in APP_RES.rglob("*"):
    if p.is_file() and p.suffix.lower() == ".xml":
        for typ, name in refs(read(p)):
            if (typ, name) not in seen:
                queue.append((typ, name))
while queue:
    typ, name = queue.pop()
    if (typ, name) in seen:
        continue
    seen.add((typ, name))
    for p in resource_defs.get((typ, name), []):
        if p.parent.name.startswith("values"):
            merge_value(p, {(typ, name)})
        else:
            copy_resource(p)

print("FINISH_MINIMAL_COMMONS_OK")
print("BINDINGS", sorted(binding_classes))
print("RESOLVED_RESOURCES", len(seen))
