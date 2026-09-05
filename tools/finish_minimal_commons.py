#!/usr/bin/env python3
"""Run the previous Commons finisher with the binding-name bug corrected."""
from pathlib import Path
import subprocess

# The previous revision is kept in the parent commit.  Its binding regex
# already captures the class name without the trailing 'Binding', so slicing
# another seven characters was incorrect and prevented layouts such as
# activity_app_lock.xml from being copied.
old = subprocess.check_output(
    ["git", "show", "HEAD^:tools/finish_minimal_commons.py"],
    text=True,
)
old = old.replace(
    'def binding_to_layout(cls):\n    base = cls[:-7]\n',
    'def binding_to_layout(cls):\n    base = cls\n',
)
if 'base = cls[:-7]' in old:
    raise SystemExit('binding_to_layout patch was not applied')
exec(compile(old, 'tools/finish_minimal_commons.py', 'exec'), {'__name__': '__main__'})
