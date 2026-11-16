"""Compatibility entry point; implementation lives in scripts/publish_release.py."""
from pathlib import Path
import runpy

if __name__ == "__main__":
    runpy.run_path(str(Path(__file__).resolve().parents[1] / "scripts" / "publish_release.py"), run_name="__main__")
