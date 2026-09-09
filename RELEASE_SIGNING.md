# Release signing — development vs public release

## Current state (transparent)

- All APKs so far are **debug/test builds** signed with a pinned debug
  keystore committed to this repository (`debug.keystore`, password
  `android`).
- A committed debug key is a **convenience for development continuity**
  (updates install over each other), **not** a security measure. Anyone can
  build this repository and produce a byte-identical signature. Treat
  debug-signed APKs as unsigned from an authenticity standpoint.
- For a public release, an APK signed with a key only the maintainer controls is
  the authenticity guarantee users can rely on.

## Model going forward

| Build type | Key | Purpose |
|-----------|-----|---------|
| Debug/beta (CI) | committed `debug.keystore` | Development continuity, public beta testing. Updates install over previous debug builds. |
| Release (production) | **private keystore stored OUTSIDE git**, injected via GitHub Actions Secrets | Public releases. Authenticity = maintainer-controlled key. |

## Production signing setup (maintainer, one-time)

1. Generate a private keystore on a machine you control (NOT in this repo):

   ```bash
   keytool -genkeypair -v -keystore release.keystore -alias g102-release \
     -keyalg RSA -keysize 4096 -validity 10000
   ```

2. Base64-encode it and store as GitHub Actions Secrets in this repository:
   - `ANDROID_KEYSTORE_BASE64` — the encoded keystore
   - `ANDROID_KEYSTORE_PASSWORD`
   - `ANDROID_KEY_ALIAS`
   - `ANDROID_KEY_PASSWORD`

3. The workflow `release.yml` (this branch) already supports this: when the
   secrets are present it signs the release APK; when absent it builds and
   clearly labels an **UNSIGNED** release artifact. It never falls back to
   debug signing for a release build.

## Workflow secrets safety

- Secrets are never echoed into build logs (`set +x` before decoding; the
   decode step's output is masked).
- The keystore file is deleted after signing (`rm -f` post-sign step).
- Only maintainers can trigger the signed path (workflow is restricted to
   `workflow_dispatch` by the maintainer).

## What users should verify

For any public release APK, users should check that the signer certificate
matches the one announced in the release notes (SHA-256 fingerprint). Debug
builds are labeled as such and install over debug builds only.
