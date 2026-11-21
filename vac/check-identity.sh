#!/usr/bin/env bash
# Source-level identity + icon check for the VAC fork (runs before every release build).
set -euo pipefail
cd "$(dirname "$0")/.."
fail=0
grep -q 'applicationId "us.vacinc.ntfy"' app/build.gradle || { echo "applicationId is not us.vacinc.ntfy" >&2; fail=1; }
grep -q '<string name="app_name" translatable="false">VAC ntfy</string>' app/src/main/res/values/values.xml \
    || { echo "app_name is not VAC ntfy" >&2; fail=1; }
for d in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
    for f in ic_launcher ic_launcher_background ic_launcher_foreground; do
        [[ -s "app/src/main/res/mipmap-$d/$f.png" ]] || { echo "missing icon mipmap-$d/$f.png" >&2; fail=1; }
    done
done
for f in ic_launcher ic_launcher_round; do
    [[ -s "app/src/main/res/mipmap-anydpi-v26/$f.xml" ]] || { echo "missing adaptive icon $f.xml" >&2; fail=1; }
done
exit $fail
