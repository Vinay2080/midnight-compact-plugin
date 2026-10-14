# Bug Record: CompactVersionManager Decomposition, Windows File Lock & Zip Slip Protection

- **Date**: 2026-10-14
- **Feature / Subsystem**: `version` / `toolchain` / `security`
- **Affected Components**: `dev.verloren.midnight.version.CompactVersionManager`
- **Severity**: High
- **Status**: Resolved

---

## 1. Symptoms & Failure Behavior

1. **Oversized Grandfathered Class**: `CompactVersionManager.java` had grown to **508 lines**, violating the repo architectural invariant ($\le$ 400 lines per file) and requiring baseline suppressions in `config/checkstyle/checkstyle-suppressions.xml`. Methods `downloadOfficialRelease` (70 lines) and `findExecutableInVersionDir` (42 lines) violated the 40-line method limit.
2. **Windows File Locking on Download Cancellation**: If a user cancelled a compiler download in progress, `FileUtil.delete(zipFile)` was called while the `FileOutputStream` remained open within the `try-with-resources` block, causing file-locking exceptions on Windows and leaving corrupt partial archives.
3. **Zip Slip Vulnerability**: `extractZip()` extracted zip entry names directly without verifying canonical destination bounds, leaving archive extraction susceptible to directory traversal attacks.
4. **Triplicated Version Switch Statements**: Mapping between toolchain releases, language versions, and ledger descriptions was replicated across three disparate switch statements.

---

## 2. Context

During an architectural review guided by `.ai/prompts/review-modify-optimize.md`, `CompactVersionManager` was identified as an oversized coordinator carrying multiple disparate responsibilities: local toolchain discovery, official binary downloads, archive extraction, WSL storage management, version detection via CLI execution, and version switching coordination.

---

## 3. Root Cause Analysis

1. **Monolithic Accumulation**: Multiple features (WSL migration, yarn fallback, release downloading, process-based version detection) were continuously placed into `CompactVersionManager` without SRP delegation.
2. **Stream Lifecycle Ordering**: In `downloadOfficialRelease()`, `FileUtil.delete(zipFile)` was called inside the stream iteration loop before the `try-with-resources` closed the `FileOutputStream`, conflicting with Windows NTFS mandatory file locking semantics.
3. **Missing Canonical Path Normalization**: In `extractZip()`, `new File(destDir, entry.getName())` was used without validating `targetPath.startsWith(destDirPath)`.

---

## 4. Solution & Implementation

1. **Class Decomposition into SRP Delegates**:
   - Extracted [`CompactWslStorageHelper`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/version/CompactWslStorageHelper.java) (`136 lines`) for WSL distribution discovery, UNC path translation, legacy migration, and binary chmod operations.
   - Extracted [`CompactVersionInstaller`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/version/CompactVersionInstaller.java) (`245 lines`) for HTTP release downloading, yarn CLI fallback, zip extraction, and permission management.
   - Extracted [`CompactVersionDetector`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/version/CompactVersionDetector.java) (`125 lines`) for non-EDT CLI version detection and process caching.
   - Refactored [`CompactVersionManager`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/version/CompactVersionManager.java) (`352 lines`) to act as a lightweight coordinator, retaining 100% backward-compatible public facade methods.
2. **Windows Stream Closure Fix**: In `CompactVersionInstaller.downloadToFile()`, cancellation sets a boolean flag and exits the loop cleanly; `FileUtil.delete(zipFile)` executes strictly after `try-with-resources` closes both `InputStream` and `FileOutputStream`.
3. **Zip Slip Defenses**: `CompactVersionInstaller.extractZip()` validates `if (!targetPath.startsWith(destDirPath)) throw new IOException(...)`.
4. **Declarative Release Table**: Unified release mappings into an immutable `ReleaseMapping` table, eliminating duplicate switch statements.
5. **Checkstyle Suppression Cleanup**: Removed `CompactVersionManager` from `FileLength` and `MethodLength` suppressions in `config/checkstyle/checkstyle-suppressions.xml`.

---

## 5. Verification & Tests Added

1. **Zip Slip Prevention Test**: Added `testZipSlipPrevention()` in `CompactVersionManagerTest` asserting an `IOException` when attempting to extract an archive entry with `../escaped.txt`.
2. **Architecture Executable Discovery**: Added `testFindExecutableInSubdirectoriesArchitecture()` verifying binary resolution in target subdirectories (`x86_64-unknown-linux-musl`).
3. **Fast-Path Detection & Edge Cases**: Added `testDetectVersionFromExecutableFastPath()` and `testCleanVersionEdgeCases()`.
4. **Automated Verification Harness**:
   - `scripts/verify-patch.ps1 -TestPattern "dev.verloren.midnight.version.*"`: ALL GATES PASSED.
   - Checkstyle static analysis: 0 errors.
   - Architecture invariants: `CompactArchitectureTest` passed with 0 violations.

---

## 6. Prevention & Key Lessons Learned

1. **Windows File Locks**: Never attempt to delete, move, or rename a file inside a `try-with-resources` block holding that same file open. Flag cancellation, exit the block, and delete upon confirmed stream closure.
2. **Path Traversal Guards on Archives**: Always normalize paths (`toPath().normalize()`) and assert `startsWith(destDirPath)` during zip/tar archive unpacking.
3. **Surgical Decomposition with Facades**: Breaking down oversized classes while maintaining delegating wrappers preserves zero regressions across all external callers.
