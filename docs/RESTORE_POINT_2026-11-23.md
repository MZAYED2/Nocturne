# Nocturne Permanent Restore Point — 2026-11-23

- **Restore Point Name:** Nocturne Pre-Platform Sprints Baseline Checkpoint
- **Creation Date:** 2026-11-23
- **Branch:** `main`
- **Target Remote:** `https://github.com/MZAYED2/Nocturne.git` (`origin/main`)
- **Annotated Git Tag:** `backup/pre-platform-sprints-2026-11-23`
- **Application Version:** `1.0.8` (Version Code `1008`)
- **Previous Remote Commit:** `0566886` (`v1.0.7`)
- **Preceding Local Commit:** `542eac8`

---

## 1. Executive Summary & Purpose

This restore point establishes a permanent, immutable development snapshot of the complete **Nocturne** watch face and companion platform baseline prior to initiating subsequent development sprints. It captures:
- The full mobile companion application with theme customization, preset management, and update center.
- The Wear OS bridge service and installation sessions for Watch Face Push.
- High-fidelity Watch Face Format (WFF) definitions, assets, and build pipelines.
- Multi-variant catalog and distribution metadata.
- Automation toolchains, packaging scripts, and developer test fixtures.

This checkpoint is **not** a public application release; it is a secured development restore baseline.

---

## 2. Included Project Components

### Mobile Companion App (`:app`)
- **Main Interface (`MainActivity.kt`):** Dynamic theme selector, full-screen live preview for Active and Ambient (AOD) display modes, theme customization, and wireless push dispatcher over the Wear OS Data Layer.
- **Theme Preset Infrastructure (`ThemeLibrary.kt`):** Customization parameters (`DialSettings`, `ThemePreset`), JSON serialization/deserialization, import/export via Android Activity Result contracts, and preset persistence.
- **Studio Variants (`StudioVariant.kt`):** Studio theme configurations and real-time palette bindings.
- **In-App Update Center (`update/`):**
  - `UpdateManager.kt`: Background update check engine with SHA-256 integrity verification, cache management, and package installer integration.
  - `UpdateCenterDialog.kt`: Material 3 update dialog displaying release notes, download progress, and verification status.
  - `UpdateCheckWorker.kt`: Periodic WorkManager background check worker.
- **Debug Tooling (`app/src/debug/`):** Debug network security configuration enabling local update test fixtures (`http://127.0.0.1:8765`).

### Wear OS Bridge (`:wear-bridge`)
- **Bridge Service (`WatchFacePushListenerService.kt`):** Wear OS Data Layer receiver handling incoming WFF packages transmitted wirelessly from the companion phone.
- **Session Management (`BridgeInstallSession.kt`):** PackageInstaller session creation, streaming, and progress handling.
- **Integrity & Verification (`BridgeUpdateVerification.kt`):** Cryptographic package and signature verification before initiating installation sessions.
- **Package Installation Engine (`WatchFaceInstallation.kt`):** WFF installation orchestration on Wear OS 6 / Samsung Galaxy Watch 5.

### Watch Face Modules & WFF Assets
- **Sage Nocturne (`sage_nocturne`):** Ottoman heritage dial with authentic arabesque motif, lunar phase complication, and calligraphy.
- **Lunar Phase (`lunar_phase`):** Photorealistic edge-to-edge lunar disc derived from NASA LROC 16-bit topographical data across 29 discrete astronomical phases, with 3D applied faceted hour markers and Haute Horlogerie Dauphine hands.
- **Obsidian Atelier (`obsidian_atelier`):** Brushed Platinum, Soft Titanium, and Smoked Graphite mechanical dial with small seconds and power reserve.
- **Emerald Nocturne (`emerald_nocturne`):** Imperial Emerald and Gold edition with ornate pattern and golden accents.
- **Source Design Assets (`design-assets/artwork/`):** Original high-resolution launcher artwork, master eclipse layers, and ornate frame assets.

### Tooling & Automation (`tools/`)
- `build_all_wff.sh` / `build_all_wff.ps1`: Multi-variant WFF compilation, validation, and packaging scripts.
- `build_watchface.ps1`: Automated WFF XML generator and asset bundler.
- `GenerateLunarMaster.java` / `GenerateExperimentalLunar.java`: NASA LROC 16-bit texture processing and edge-to-edge astronomical phase generation.
- `local-update-server.mjs`: Local HTTP fixture server for end-to-end update flow verification.
- `release-manifest.mjs`: Cryptographic fingerprinting and catalog manifest synchronization.
- `validator-push-cli.jar`: Official Google Watch Face Push validation utility.

---

## 3. Excluded Secrets & Local Configuration

In strict accordance with security auditing standards, the following environment-specific and sensitive items are excluded from Git tracking via `.gitignore`:
- **Private Release Keystores:** `.signing/nocturne-release.jks` and any `*.jks`/`*.keystore` files.
- **Local Machine Settings:** `local.properties` (containing local Android SDK paths and local keystore passwords).
- **Historical Archives:** `legacy-projects/` (historical prototypes and pre-migration backups).
- **Intermediate Build Artifacts:** `build/`, `*.class`, `tools/experimental_lunar/`, `.gradle/`, `.cxx/`, `.externalNativeBuild/`.
- **IDE User State:** `.idea/workspace.xml`, `.idea/caches/`, `.idea/libraries/`.

---

## 4. Build & Test Verification Status

Prior to committing and tagging this checkpoint, the following verifications were executed and validated:
- `:app:assembleDebug` — **BUILD SUCCESSFUL**
- `:wear-bridge:assembleDebug` — **BUILD SUCCESSFUL**
- `:watchface:assembleDebug` — **BUILD SUCCESSFUL**
- `:app:testDebugUnitTest` — **TESTS PASSED (1 passed, 0 failed, 0 skipped)**
- Signing key safety audit — **NO SECRETS EXPOSED**
- Static code & asset audit — **ALL COMPONENTS ACCOUNTED FOR**

---

## 5. Safe Restoration Instructions

To restore and examine this exact baseline without modifying or endangering newer development work in the main working tree:

### Option A: Clone into a Dedicated Directory (Recommended)
```bash
# 1. Clone into an isolated restore directory
git clone https://github.com/MZAYED2/Nocturne.git Nocturne-Restore-2026-11-23

# 2. Enter the restored directory
cd Nocturne-Restore-2026-11-23

# 3. Check out the permanent restore tag into a detached state
git checkout tags/backup/pre-platform-sprints-2026-11-23

# 4. (Optional) Create a local exploration branch
git checkout -b restore/pre-platform-sprints tags/backup/pre-platform-sprints-2026-11-23
```

### Option B: Using Git Worktree in Existing Repository
```bash
# From the root of F:\Projects\Nocturne
git worktree add ../Nocturne-Baseline-2026-11-23 backup/pre-platform-sprints-2026-11-23
```

### Returning to the Latest Development Branch
```bash
# In your main working tree
git checkout main
git pull origin main
```

---

## 6. Known Limitations
- Release builds (`assembleRelease`) strictly mandate the configuration of release signing credentials via environment variables (`NOCTURNE_KEYSTORE_PASSWORD`, etc.) or `local.properties`. Debug builds do not require private credentials and compile out-of-the-box.
- Watch Face Push requires Bluetooth/Wi-Fi pairing between the companion Android device and a supported Wear OS 6 watch running `Nocturne-WearBridge`.
