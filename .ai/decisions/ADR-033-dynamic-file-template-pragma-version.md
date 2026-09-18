# ADR-033: Dynamic File Template Pragma Version Resolution and Properties Provider

## Status
Accepted (v1.3.0 / Complete)

## Date
2026-09-18

## Subsystem
IDE & File Templates

## Related ADRs
- [ADR-010](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-010-file-creation-and-template-sanitization.md): File Creation Action, Nested MkDirs, & Template Sanitization
- [ADR-011](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-011-toolchain-discovery-and-wsl-translation.md): Toolchain Discovery and Cross-Platform WSL Execution
- [ADR-012](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-012-isolated-multi-version-compiler-management.md): Isolated Multi-Version Compiler Management & SemVer Normalization
- [ADR-032](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/.ai/decisions/ADR-032-pragma-version-completion-and-documentation.md): Pragma Version Directives Code Completion and Quick Documentation

---

## 1. Context & Problem Statement
When developers create new Compact smart contracts, modules, or interfaces using the **New -> Compact File** action or standard IntelliJ file templates, the emitted files begin with a pragma directive specifying language compatibility:
```compact
pragma language_version >= <version>;
```
Previously, all internal file templates (`Compact Contract.compact.ft`, `Compact Module.compact.ft`, `Compact Interface.compact.ft`, `Compact File.compact.ft`) hardcoded `pragma language_version >= 0.20.0;`.

This posed several usability and architectural shortcomings:
1. **Outdated Baseline**: Modern Compact compiler releases (such as `0.34.0`, which targets Compact Language `0.26.0` on Midnight Ledger 9) were misrepresented by an obsolete `0.20.0` header.
2. **Lack of Dynamic Project Toolchain Integration**: If a project configured a specific compiler toolchain (e.g. `0.31.1` -> `0.23.0`, or `0.34.0` -> `0.26.0`) via project settings (`selectedCompilerVersion` or `customCompilerPath`), newly created files ignored the active environment and still generated `0.20.0`.
3. **No Platform Template Properties Provider**: Standard IntelliJ file template evaluation mechanisms (such as custom templates created by users or external plugin integrations) had no access to the active Compact language or compiler versions as template variables.

---

## 2. Decision Drivers & Invariants
- **Dynamic Context Resolution**:
  File templates must dynamically query `CompactToolchainUtil.getActiveCompilerVersion(project)` to determine the currently active or configured compiler toolchain.
- **Language vs Toolchain Version Mapping**:
  The pragma directive in source code requires the Compact language version (e.g. `0.26.0`), not the compiler toolchain release version (e.g. `0.34.0`). The system must map toolchain versions to language versions via `CompactVersionManager.getLanguageVersionForToolchain(toolchainVer)`.
- **Resilient Fallback**:
  When creating a file outside a project context, or in a project without a detected/configured compiler, templates must fall back to the latest supported Compact language version (`0.26.0` from toolchain `0.34.0`), rather than an obsolete version or an unbound template error.
- **IntelliJ Platform Extension Integration**:
  Implement `DefaultTemplatePropertiesProvider` registered under `<defaultTemplatePropertiesProvider>` so that all file template evaluations for a directory automatically receive `COMPACT_LANGUAGE_VERSION`, `LANGUAGE_VERSION`, `COMPACT_COMPILER_VERSION`, and `COMPILER_VERSION`.
- **Velocity Template Conditional Expressions**:
  File templates must evaluate `${COMPACT_LANGUAGE_VERSION}` or `${LANGUAGE_VERSION}` with Velocity `#if ... #elseif ... #else ... #end` directives to guarantee zero modal prompt popups and seamless rendering under all execution paths (including headless and manual `FileTemplate.getText(props)` calls).

---

## 3. Architecture & Implementation Plan

### 3.1 Properties Provider (`CompactDefaultTemplatePropertiesProvider`)
Create `CompactDefaultTemplatePropertiesProvider` implementing `com.intellij.ide.fileTemplates.DefaultTemplatePropertiesProvider`:
- Defines standard property keys:
  - `COMPACT_LANGUAGE_VERSION`: Active Compact language version (e.g. `0.26.0`).
  - `LANGUAGE_VERSION`: Alias for compatibility with general templates.
  - `COMPACT_COMPILER_VERSION`: Active compiler toolchain version (e.g. `0.34.0`).
  - `COMPILER_VERSION`: Alias for compiler toolchain version.
- Exposes helper methods:
  - `resolveLanguageVersion(@Nullable Project project)`: Returns active language version or fallback `0.26.0`.
  - `resolveCompilerVersion(@Nullable Project project)`: Returns active toolchain version or fallback `0.34.0`.
  - `fillVersionProperties(@Nullable Project project, @NotNull Properties props)`.

### 3.2 Action Property Injection (`CompactCreateFileAction`)
In `CompactCreateFileAction.createFile`:
- Resolve `langVer` and `compilerVer` for the target project.
- Inject them into both `extraProperties` (`customProperties`) and `additionalProperties` maps passed to `createFileFromTemplate`.

### 3.3 Internal Velocity File Templates (`.ft`)
Update all four `.ft` files:
- `Compact File.compact.ft`
- `Compact Contract.compact.ft`
- `Compact Module.compact.ft`
- `Compact Interface.compact.ft`

Using standard Velocity conditional syntax:
```velocity
#* @vtlvariable name="COMPACT_LANGUAGE_VERSION" type="java.lang.String" *#
#* @vtlvariable name="LANGUAGE_VERSION" type="java.lang.String" *#
#if (${COMPACT_LANGUAGE_VERSION} && ${COMPACT_LANGUAGE_VERSION} != "")
pragma language_version >= ${COMPACT_LANGUAGE_VERSION};
#elseif (${LANGUAGE_VERSION} && ${LANGUAGE_VERSION} != "")
pragma language_version >= ${LANGUAGE_VERSION};
#else
pragma language_version >= 0.26.0;
#end
```

---

## 4. Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. The pragma language version is resolved dynamically from `CompactToolchainUtil` -> `CompactVersionManager` mapping based on the user's active toolchain or project settings.
- **Is it resilient?**: Yes. If no compiler is configured or installed, it cleanly falls back to the latest officially supported release (`0.26.0`) defined in `CompactVersionManager.KNOWN_VERSIONS.getFirst()`.

---

## 5. Verification & Testing Strategy
- Unit tests in `CompactFileTemplateTest.java`:
  - Verify default template evaluation produces `pragma language_version >= 0.26.0;`.
  - Verify template evaluation with explicit `COMPACT_LANGUAGE_VERSION` property uses the specified version.
  - Verify `CompactCreateFileAction.createFile` creates files with the active project's language version.
  - Verify switching project compiler settings (e.g. to `0.30.0` / Language `0.22.0`) updates created template file pragma to `>= 0.22.0`.
  - Verify `CompactDefaultTemplatePropertiesProvider` populates all 4 properties (`COMPACT_LANGUAGE_VERSION`, `LANGUAGE_VERSION`, `COMPACT_COMPILER_VERSION`, `COMPILER_VERSION`).
