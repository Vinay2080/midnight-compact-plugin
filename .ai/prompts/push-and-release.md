# Executable Protocol: Code Inspection, Clean Changelog, Semantic Tagging & Release

This document defines the mandatory, deterministic execution protocol for inspecting code, cleaning the user-facing changelog, determining semantic release tags, packaging, and pushing releases in `midnight-compact-plugin`.

---

## 1. Preconditions

Before executing a release or push, the AI agent **MUST** verify:
1. `git status` on `master` is clean.
2. A pre-flight drift check via [`.ai/prompts/sync-context-and-decisions.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/prompts/sync-context-and-decisions.md) has verified that machine state, ADRs, and tests are synchronized.
3. Gradle wrapper (`.\gradlew.bat` / `./gradlew`) and JDK 25 are available.
4. IntelliJ MCP server is responsive for final inspection.

---

## 2. Sequential Steps

The agent must execute these 6 steps in exact chronological order:

```text
[Step 1: Full Inspection Loop] ── get_file_problems & lint_files across touched files
        │
        ▼
[Step 2: Full Suite Verification] scripts/verify-patch.ps1 -AllTests
        │
        ▼
[Step 3: User Changelog Cleanup]  Translate Unreleased into ## [X.Y.Z] - YYYY-MM-DD
        │
        ▼
[Step 4: Version Synchronization] Bump gradle.properties, plugin.xml, project-state.yaml
        │
        ▼
[Step 5: Packaging & Build Check] ./gradlew buildPlugin
        │
        ▼
[Step 6: Commit, Tag & Push] ──── Commit chore(release), create git tag, push to remote
```

### Step 1: Full-Scope Post-Edit Inspection Loop via MCP
- Run inspection on all modified and referenced files:
  ```text
  execute_tool get_file_problems --filePath "<absolute-path>"
  execute_tool lint_files --files [\"<absolute-path>\"]
  ```
- Strict Zero-Tolerance: 0 errors (`ERROR`), 0 warnings (`WARNING`), 0 weak warnings (`WEAK WARNING`), 0 grammar/spelling errors, and all modern Java 25 language-level suggestions applied.

### Step 2: Full Suite Automated Verification
- Run the full test suite through the multi-gate runner:
  ```powershell
  powershell -ExecutionPolicy Bypass -File .\scripts\verify-patch.ps1 -AllTests
  ```
- Confirm that:
  - Gate 1 (Compilation): SUCCESS.
  - Gate 2 (Plugin Structure): SUCCESS.
  - Gate 3 (Tests): All tests pass 100% with 0 failures.
  - [`build/verification-report.json`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/build/verification-report.json) has status `"PASSED"`.

### Step 3: Dual-Tier User Changelog Cleanup
- In [`CHANGELOG.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/CHANGELOG.md):
  - Convert `## [Unreleased]` into `## [X.Y.Z] - YYYY-MM-DD`.
  - Open a fresh empty `## [Unreleased]` at the top.
  - Consolidate entries into minimal, human-centric bullets answering *"What changed for me?"*.
  - Enforce the **Technical Blacklist**: Strip all internal Java class names, method names, AST node types, file paths, test counts, and ADR numbers.

### Step 4: Release Version Synchronization
- Update `version` in [`gradle.properties`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/gradle.properties) (`version=X.Y.Z`).
- Update `<version>` and `<change-notes>` in [`src/main/resources/META-INF/plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml).
- Update `metadata.version` in [`.ai/project-state.yaml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/project-state.yaml) and [`.ai/context/current-state.md`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/context/current-state.md).

### Step 5: Plugin Distribution Packaging
- Run distribution packaging:
  ```powershell
  .\gradlew.bat buildPlugin
  ```
- Confirm the plugin `.zip` distribution artifact is created in `build/distributions/`.

### Step 6: Git Commit, Tagging, & Push
- Stage and commit release changes:
  ```bash
  git add gradle.properties src/main/resources/META-INF/plugin.xml CHANGELOG.md .ai/
  git commit -m "chore(release): prepare vX.Y.Z"
  ```
- Create annotated Git tag:
  ```bash
  git tag -a vX.Y.Z -m "Release vX.Y.Z: <clean user-facing summary>"
  ```
- Push to remote:
  ```bash
  git push origin master
  git push origin vX.Y.Z
  ```

---

## 3. Mandatory Gates & Evidence Requirements

| Gate | Requirement | Mandatory Evidence in Agent Output |
|:---|:---|:---|
| **Gate 1: Static Inspection** | Zero issues | Tool outputs showing `errors: []` and `items: []` |
| **Gate 2: Full Verification** | All tests pass | `build/verification-report.json` with status `"PASSED"` and `test_target: "ALL"` |
| **Gate 3: Changelog Quality** | User perspective | Quoted release notes under `## [X.Y.Z] - YYYY-MM-DD` |
| **Gate 4: Version Alignment** | Exact match | Values of version in `gradle.properties`, `plugin.xml`, and `project-state.yaml` |
| **Gate 5: Build Package** | Zip generated | Confirmation of `build/distributions/midnight-plugin-*.zip` |
| **Gate 6: Annotated Tag** | Git tag created | Tag name `vX.Y.Z` and tag message |

---

## 4. Failure Conditions

The release **MUST HALT** if:
1. Any test in the full test suite fails.
2. `buildPlugin` fails or emits plugin descriptor errors.
3. `CHANGELOG.md` contains uncurated technical jargon or internal AI notes.
4. Version numbers diverge across `gradle.properties`, `plugin.xml`, and `project-state.yaml`.

---

## 5. Recovery Rules

- **If `buildPlugin` Fails**: Inspect plugin descriptor XML errors in `plugin.xml`, correct invalid syntax or tag names, and re-run.
- **If Version Already Exists in Git**: Check existing tags via `git tag -l`. Bump to the next appropriate SemVer increment.

---

## 6. Completion Conditions

The release is complete **ONLY** when:
1. All verification gates have passed.
2. Plugin distribution zip is built and valid.
3. Release commit is created on `master`.
4. Annotated Git tag `vX.Y.Z` is created.
5. Commits and tag are pushed to remote.
