# Operational Runbook: Build, Verification, Signing & Release

This runbook specifies the step-by-step procedures for building, testing, verifying, signing, and publishing the **Midnight Compact Language Plugin**.

---

## 1. Local Development Sandbox

To test the plugin interactively inside an isolated IntelliJ IDEA sandbox:
```bash
# Launch development sandbox with Material Theme UI (default)
.\gradlew.bat runIde

# Launch development sandbox without Material Theme UI
.\gradlew.bat runIdeNoMtui
```

---

## 2. Plugin Build & Structural Verification

Before packaging or publishing, the plugin distribution must pass structural verification against the IntelliJ Platform specification:
```bash
# 1. Run full test suite
.\gradlew.bat test

# 2. Verify plugin structure, descriptor, and class dependencies
.\gradlew.bat verifyPluginStructure

# 3. Build distribution package (.zip)
.\gradlew.bat buildPlugin
```
The output artifact is generated at `build/distributions/midnight-compact-plugin-<version>.zip`.

---

## 3. Production Release & Marketplace Publication

Releases are automated via GitHub Actions workflow (`.github/workflows/release.yml`) triggered on git tags (`v*`).

### Pre-Release Checklist
1. All unit and integration tests pass (`.\gradlew.bat test`).
2. IntelliJ inspections are clean (0 errors, 0 warnings, 0 weak warnings).
3. `gradle.properties` has `version=X.Y.Z`.
4. `CHANGELOG.md` has user-facing release notes under `## [X.Y.Z] - YYYY-MM-DD` and no internal engineering notes.
5. Commit and tag:
   ```bash
   git commit -m "chore(release): prepare vX.Y.Z"
   git tag -a vX.Y.Z -m "Release vX.Y.Z: <clean summary>"
   git push origin master
   git push origin vX.Y.Z
   ```

### CI Publication Pipeline Steps
The CI runner performs:
1. Validates tag format against SemVer (`vX.Y.Z`).
2. Executes `./gradlew verifyPluginStructure`.
3. Executes `./gradlew signPlugin` using `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, and `PRIVATE_KEY_PASSWORD`.
4. Executes `./gradlew publishPlugin` using `PUBLISH_TOKEN` to JetBrains Marketplace.
