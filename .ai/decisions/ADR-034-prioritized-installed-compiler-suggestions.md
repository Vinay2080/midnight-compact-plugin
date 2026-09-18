# ADR-034: Prioritized Installed Compiler Suggestions for Pragma Constraints

## Status
Accepted (v1.3.0 / Complete)

## Date
2026-09-18

## Subsystem
Inspection & Intentions / Toolchain Management

## Related ADRs
- [ADR-011](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-011-toolchain-discovery-and-wsl-translation.md): Toolchain Discovery and Cross-Platform WSL Execution
- [ADR-012](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-012-isolated-multi-version-compiler-management.md): Isolated Multi-Version Compiler Management & SemVer Normalization
- [ADR-032](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-032-pragma-version-completion-and-documentation.md): Pragma Version Directives Code Completion and Quick Documentation
- [ADR-033](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-033-dynamic-file-template-pragma-version.md): Dynamic File Template Pragma Version Resolution and Properties Provider

---

## 1. Context & Problem Statement
When a Compact smart contract specifies a pragma language requirement such as:
```compact
pragma language_version >= 0.20;
```
and the active compiler toolchain configured in the project is lower (e.g. toolchain `0.26.0`, corresponding to language `0.18.0`), `CompactPragmaVersionInspection` flags a version mismatch.

Previously, the inspection and associated intention (`CompactSwitchCompilerVersionIntention`) only offered to download and switch to the exact required version specified in the pragma (e.g. download toolchain `0.28.0` for language `0.20`).
If the developer already had higher compatible compiler versions locally downloaded/installed in their toolchain directory (e.g. toolchain `0.34.0` targeting language `0.26.0`, or `0.31.1` targeting language `0.23.0`):
1. The IDE unnecessarily prompted an external network download rather than offering an instant, offline local switch.
2. If multiple installed versions satisfied the constraint, none were presented to the user.
3. If the exact version was already installed locally, a redundant "Download and switch..." fix could still be generated.

---

## 2. Decision Drivers & Invariants
- **Prioritize Offline & Installed Compilers**:
  Any locally installed compiler toolchain that satisfies the pragma constraint must be presented as an immediate offline quick-fix before prompting external downloads.
- **Strict Constraint Enforcement**:
  Open/minimum constraints (e.g. `>= 0.20`, `0.20`) permit higher compatible compilers (such as `0.26.0` / toolchain `0.34.0`).
  Locked/pinned constraints (such as `^0.20`, `~0.20`, `< 0.22`) must strictly respect SemVer upper bounds and never suggest incompatible higher versions.
- **Descending Order (Newest First)**:
  When multiple installed compilers satisfy the constraint, they must be presented in descending order (highest version first).
- **SemVer De-duplication**:
  Multiple installed patch releases targeting the same Compact language version are de-duplicated so the user is not flooded with redundant language options.
- **No Redundant Download Options**:
  If the exact required compiler toolchain is already installed, the IDE must not offer a duplicate "Download and switch..." option.
- **Intention Action Alignment**:
  The context intention (`CompactSwitchCompilerVersionIntention`) must follow the same resolution logic, dynamically offering an instant switch to the highest installed satisfying compiler if available.

---

## 3. Architecture & Implementation Plan

### 3.1 Discovered Toolchains in `CompactPragmaVersionInspection`
In `CompactPragmaVersionInspection.buildCompilerQuickFixes()`:
- Discovers installed compilers via `CompactVersionManager.getInstalledVersions()`.
- Sorts candidate toolchains descending using `CompactSemVerUtil.DESCENDING_COMPARATOR`.
- Evaluates each against `CompactSemVerUtil.satisfiesConstraint()`, mapping toolchain versions to language versions when evaluating `language_version` pragmas.
- Emits `CompactSwitchCompilerQuickFix` for each satisfying installed compiler.
- If the exact required version was not already installed and offered, appends the download quick-fix.

### 3.2 Dynamic Context Intention in `CompactSwitchCompilerVersionIntention`
Extracted `resolveTarget()` with record `TargetVersionResolution(toolchainVer, displayVer, isInstalled)`:
- Checks if exact version is installed.
- Scans installed compilers in descending SemVer order for satisfying alternatives.
- Updates action text and execution accordingly:
  - If installed: `"Switch project compiler to Compact <lang> (v<toolchain>)"`.
  - If not installed: `"Download and use Compact <lang> (v<toolchain>)"`.

### 3.3 QuickFix Toolchain Overrides in `CompactSwitchCompilerQuickFix`
Added support for explicit toolchain version overrides:
```java
public CompactSwitchCompilerQuickFix(@NotNull String targetVersion, boolean isCompilerPragma, @Nullable String explicitToolchainVersion)
```
Ensures that selecting a higher installed compiler explicitly configures that specific toolchain in `MidnightProjectSettings`.

### 3.4 Deterministic Test Support in `CompactVersionManager`
Added `@TestOnly public static void setInstalledVersionsForTesting(@Nullable SequencedMap<String, String> versions)`:
- Allows test suites to mock installed toolchains deterministically without requiring actual disk or WSL I/O.
- Resets cleanly in `tearDown()`.

---

## 4. Verification & Testing
- Unit tests added in `CompactPragmaVersionInspectionTest`:
  - `testInstalledHigherVersionSuggestedWhenOpenConstraint()`
  - `testInstalledHigherVersionNotSuggestedWhenLockedConstraint()`
  - `testMultipleInstalledSatisfyingVersionsSortedDescending()`
  - `testExactVersionInstalledNoDuplicateDownloadFix()`
  - `testInstalledCompilerPragmaConstraint()`
- Intention test added in `CompactPragmaIntentionTest`:
  - `testIntentionOffersInstalledHigherCompilerDirectly()`
- Intention preview side-effect test verified:
  - `CompactQuickFixPreviewSideEffectTest` passes 100%.
