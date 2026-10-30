#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Run selected original policy tests without private inputs, Android, or downloads."""
from pathlib import Path
import os
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SOURCES = [
    "src/bridge/org/local/aetherarms/AudioFocusPolicy.java",
    "src/bridge/org/local/aetherarms/PauseAudioController.java",
    "src/remoteplay/org/local/aetherarms/RemotePadController.java",
    "src/skins/org/local/aetherarms/SkinPolicy.java",
    "src/skins/org/local/aetherarms/SkinArchive.java",
    "src/skins/org/local/aetherarms/SkinLoadState.java",
    "src/controller/org/local/aetherarms/ControllerSettings.java",
]
TESTS = ["AudioFocusChecks", "RemotePadChecks", "SkinChecks", "SkinLoadChecks"]


def main():
    java, javac = shutil.which("java"), shutil.which("javac")
    if not java or not javac:
        raise SystemExit("A JDK with java and javac on PATH is required.")
    env = dict(os.environ)
    if env.get("PREFIX"):
        library = Path(env["PREFIX"]) / "lib"
        if library.is_dir():
            env["LD_LIBRARY_PATH"] = str(library) + (
                os.pathsep + env["LD_LIBRARY_PATH"] if env.get("LD_LIBRARY_PATH") else ""
            )
    with tempfile.TemporaryDirectory(prefix=".host-checks-", dir=ROOT) as folder:
        temporary = Path(folder)
        classes = temporary / "classes"
        classes.mkdir()
        inputs = [ROOT / item for item in SOURCES]
        inputs += [ROOT / "tests" / (name + ".java") for name in TESTS]
        subprocess.run(
            [javac, "-encoding", "UTF-8", "-d", str(classes), *map(str, inputs)],
            env=env, check=True,
        )
        for name in TESTS:
            args = [java, "-Djava.io.tmpdir=" + str(temporary), "-cp", str(classes), name]
            if name == "SkinChecks":
                absent = temporary / "intentionally-omitted-community-skin.zip"
                assert not absent.exists()
                args.append(str(absent))
            subprocess.run(args, env=env, check=True)
    print("PASS: 4 public host suites; community artwork, networking, and device tests excluded.")


if __name__ == "__main__":
    main()
