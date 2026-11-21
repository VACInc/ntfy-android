#!/usr/bin/env bash
# VAC fork release build: F-Droid flavor (no Firebase), signed with the one fixed fork key.
#   NTFY_KEYSTORE=/path/ntfy.p12 NTFY_KEYSTORE_PASSWORD=... vac/build-release.sh OUTDIR
# Fails when the app identity drifts (applicationId, app name, signing certificate) or a launcher
# icon is missing at any density.
set -euo pipefail
cd "$(dirname "$0")/.."
out=${1:?usage: build-release.sh OUTDIR}
want_id=us.vacinc.ntfy
want_name="VAC ntfy"
want_cert=a4ded958ba217a99ea47c1360f64fb7dea8c3c6bb1c871a721dd5858808bcb21

vac/check-identity.sh

[[ -n "${NTFY_KEYSTORE:-}" && -n "${NTFY_KEYSTORE_PASSWORD:-}" ]] || { echo "NTFY_KEYSTORE and NTFY_KEYSTORE_PASSWORD are required" >&2; exit 2; }
./gradlew --no-daemon -q assembleFdroidRelease
apk=app/build/outputs/apk/fdroid/release/app-fdroid-release.apk
[[ -f "$apk" ]] || { echo "no APK at $apk" >&2; exit 1; }

sdk=${ANDROID_HOME:-$HOME/Android/Sdk}
bt=$(ls -d "$sdk"/build-tools/* | sort -V | tail -n 1)
"$bt/apksigner" verify --print-certs "$apk" > /tmp/ntfy-apk-certs.txt
cert=$(grep -m1 'certificate SHA-256 digest' /tmp/ntfy-apk-certs.txt | awk '{print $NF}')
[[ "$cert" == "$want_cert" ]] || { echo "signing certificate $cert is not the fixed fork key" >&2; exit 1; }
badging=$("$bt/aapt2" dump badging "$apk")
grep -q "package: name='$want_id'" <<<"$badging" || { echo "applicationId drifted" >&2; exit 1; }
grep -q "application-label:'$want_name'" <<<"$badging" || { echo "app label drifted" >&2; exit 1; }
grep -q "application-icon-640:" <<<"$badging" || { echo "APK has no launcher icon" >&2; exit 1; }
ver=$(sed -n "s/.*versionName='\([^']*\)'.*/\1/p" <<<"$badging" | head -n 1)
mkdir -p "$out"
cp "$apk" "$out/vacntfy-$ver.apk"
echo "built $out/vacntfy-$ver.apk ($want_id, cert $cert)"
