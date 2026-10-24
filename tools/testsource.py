"""Static discovery of TestNG test classes and methods from Java sources (no compilation needed).

A test class is a non-abstract class in a registered system package that declares @Test methods
(or inherits them from an abstract parent in the same source tree). File names are not assumed.
"""

import re

from systems import BASE, SYSTEMS, TEST_SOURCE_ROOT, system_for_class

_PACKAGE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.M)
_CLASS = re.compile(r"^\s*public\s+((?:abstract|final)\s+)*class\s+(\w+)(?:\s+extends\s+(\w+))?", re.M)
_METHOD = re.compile(r'\s*(?:@\w+(?:\((?:"(?:[^"\\]|\\.)*"|[^)"])*\))?\s*)*(?:public|protected)?\s*(?:static\s+)?void\s+(\w+)\s*\(', re.S)
_TEST_ID = re.compile(r"\b[A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+\b")


def _balanced(text, start):
    """Return the text inside parentheses starting at text[start] == '(' and the index after it."""
    depth = 0
    for index in range(start, len(text)):
        char = text[index]
        if char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return text[start + 1:index], index + 1
    return text[start + 1:], len(text)


def _strip_comments(source):
    source = re.sub(r"/\*.*?\*/", lambda m: "\n" * m.group(0).count("\n"), source, flags=re.S)
    return re.sub(r"(?<!:)//[^\n]*", "", source)


def _string_value(args, name):
    match = re.search(name + r'\s*=\s*"((?:[^"\\]|\\.)*)"', args)
    return match.group(1) if match else ""


def _parse_methods(source):
    methods = []
    for match in re.finditer(r"@Test\b", source):
        cursor = match.end()
        args = ""
        rest = source[cursor:]
        if rest.lstrip().startswith("("):
            open_at = cursor + (len(rest) - len(rest.lstrip()))
            args, cursor = _balanced(source, open_at)
        declaration = _METHOD.match(source, cursor)
        if not declaration:
            continue  # class-level @Test or unusual layout
        name = declaration.group(1)
        trailer = source[cursor:declaration.end()]
        description = _string_value(args, "description").strip()
        test_id = _TEST_ID.search(description)
        priority = re.search(r"priority\s*=\s*(-?\d+)", args)
        covers = re.search(r"@Covers\(\s*\{?([^)}]*)\}?\s*\)", trailer)
        severity = re.search(r"@Severity\(\s*SeverityLevel\.(\w+)", trailer)
        methods.append({
            "name": name,
            "id": test_id.group(0) if test_id else name,
            "description": re.sub(r"^[A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+\s*[—:-]\s*", "", description),
            "priority": int(priority.group(1)) if priority else 0,
            "covers": re.findall(r'"([^"]+)"', covers.group(1)) if covers else [],
            "severity": severity.group(1).lower() if severity else "",
            "enabled": not re.search(r"enabled\s*=\s*false", args),
            "issue": bool(re.search(r"@Issue\(", trailer)),
        })
    methods.sort(key=lambda m: m["priority"])
    return methods


def _read_sources():
    classes = {}
    for path in TEST_SOURCE_ROOT.rglob("*.java"):
        source = _strip_comments(path.read_text(encoding="utf-8"))
        package = _PACKAGE.search(source)
        declared = _CLASS.search(source)
        if not package or not declared:
            continue
        feature = re.search(r'@Feature\(\s*"([^"]+)"', source[:declared.start()])
        classes[declared.group(2)] = {
            "class": package.group(1) + "." + declared.group(2),
            "simple": declared.group(2),
            "abstract": "abstract" in (declared.group(1) or ""),
            "parent": declared.group(3),
            "feature": feature.group(1) if feature else "",
            "methods": _parse_methods(source),
            "path": path.relative_to(TEST_SOURCE_ROOT.parent.parent.parent).as_posix(),
        }
    return classes


def _inherited_methods(classes, entry, seen=None):
    seen = seen or set()
    methods = list(entry["methods"])
    parent = classes.get(entry["parent"]) if entry["parent"] else None
    if parent and parent["simple"] not in seen:
        seen.add(parent["simple"])
        known = {m["name"] for m in methods}
        methods += [m for m in _inherited_methods(classes, parent, seen) if m["name"] not in known]
    return methods


def discover():
    """{system_key: [class entries sorted by name]} for every registered system."""
    classes = _read_sources()
    found = {key: [] for key in SYSTEMS}
    for entry in classes.values():
        if entry["abstract"]:
            continue
        system = system_for_class(entry["class"])
        if system is None:
            continue
        methods = _inherited_methods(classes, entry)
        if not methods:
            continue
        found[system].append({**entry, "methods": methods, "module": module_of(system, entry["class"])})
    for entries in found.values():
        entries.sort(key=lambda e: e["class"])
    return found


def module_of(system, class_name):
    """Feature module: package below the system root, e.g. 'formbuilder'."""
    for package in SYSTEMS[system]["packages"]:
        prefix = BASE + package + "."
        if class_name.startswith(prefix):
            parts = class_name[len(prefix):].split(".")[:-1]
            return ".".join(parts) or "root"
    return "root"
