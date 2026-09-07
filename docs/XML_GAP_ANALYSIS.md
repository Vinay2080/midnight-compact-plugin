# XML Extension Point Gap Analysis & Platform Comparison

This document provides a systematic, line-by-line comparative audit of the extension point declarations in the **Midnight Compact Language Plugin** ([`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml)) against the four reference production language plugins bundled in this repository:
1. **IntelliJ Rust** ([`intellij-rust/src/main/resources/META-INF/rust-core.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-rust/src/main/resources/META-INF/rust-core.xml))
2. **IntelliJ Elixir** ([`intellij-elixir/resources/META-INF/plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-elixir/resources/META-INF/plugin.xml))
3. **IntelliJ Scala** ([`intellij-scala/scala/scala-impl/resources/META-INF/scala-plugin-common.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/intellij-scala/scala/scala-impl/resources/META-INF/scala-plugin-common.xml))
4. **JetBrains R Plugin** ([`Rplugin/resources/META-INF/rplugin-common.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/Rplugin/resources/META-INF/rplugin-common.xml))

---

## 1. Executive Comparison Matrix

The following table summarizes platform capability dimensions across all five plugin descriptors:

| Platform Capability Subsystem | Current Midnight (`plugin.xml`) | `intellij-rust` | `intellij-elixir` | `intellij-scala` | `Rplugin` | Status in Midnight |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **Parser & PSI Hierarchy** | Handwritten (`CompactParserDefinition`) | Generated & Custom | Handwritten (`ElixirParserDefinition`) | Custom Parser Definition | Generated (`RParserDefinition`) | **Complete** |
| **Syntax Highlighting & Colors** | Complete (42+ color keys) | Complete | Complete | Complete | Complete | **Complete** |
| **Code Completion** | `completion.contributor` (Basic/Type/Member) | Multi-layered (`RsKeywordCompletionContributor`, etc.) | Contextual | Multi-layered + AI Command Providers | Contextual + AutoPopup | **Good (Enhance with Command Completion)** |
| **Find Usages & Word Scanner** | `lang.findUsagesProvider` | `lang.findUsagesProvider`, `usageTypeProvider`, `fileStructureGroupRuleProvider` | `lang.findUsagesProvider` | `findUsages.*Searcher` | `usageTypeProvider` | **Partial (Missing Grouping & Usage Types)** |
| **Stubs & Stub Indexing** | **None** (AST loaded into memory) | `stubElementTypeHolder`, `stubIndex` (12+ indexes) | `stubElementTypeHolder`, `stubIndex` | `stubElementTypeHolder`, `stubIndex` | `stubElementTypeHolder`, `stubIndex` | **CRITICAL GAP** |
| **Modern Symbol API** | Legacy `psi.referenceContributor`, `gotoSymbol` | Transitioning | `psi.declarationProvider`, `psi.symbolReferenceProvider` | Modern Symbol Searchers | Legacy & Modern | **MODERATE GAP** |
| **Refactoring Engine** | In-place rename only | Rename processor, change signature, suggested refactoring | In-place rename, move | Move, copy, inline, change signature | Rename processor, inline assignment | **HIGH GAP** |
| **Project & DApp Wizards** | **None** | `moduleType`, `moduleBuilder`, `newProjectWizard.language`, `directoryProjectGenerator` | `moduleType`, `moduleBuilder` | Framework type, project templates | `moduleType`, `directoryProjectGenerator`, `projectTemplatesFactory` | **CRITICAL GAP** |
| **Toolchain & SDK Management** | Tool window + Settings (`MidnightProjectSettings`) | `cargo` project model, toolchain listener | `sdkType` | External build system | `RToolchain`, WSL distribution manager | **MODERATE GAP (Add Status Bar & SDK)** |
| **Status Bar Widgets** | **None** | Macro/Cargo status | `statusBarWidgetFactory` | `statusBarWidgetFactory` (`ScalaHighlightingMode`) | `statusBarWidgetFactory` (`rInterpreterWidget`) | **HIGH GAP** |
| **Code Vision & Metrics** | **None** | `codeInsight.daemonBoundCodeVisionProvider` | **None** | Inlay metrics | **None** | **MODERATE GAP** |
| **Testing Framework** | **None** (Only compiler run config) | `runLineMarkerContributor` (tests), `testSourcesFilter` | `testFinder`, `testRunConfiguration` | Test runners (ScalaTest, Specs2) | `RTestPackageAction` | **HIGH GAP** |
| **Interactive REPL / Console** | **None** | CLion debugger console | Elixir IEx console | Scala REPL, Worksheets (`scratch.rootType`) | `R_Console` tool window, chunk execution | **HIGH GAP** |
| **Interactive Blockchain / Visualizer Tool Windows** | Compiler Version Tool Window (`CompactCompilerPanel`) | Cargo tool window | Mix tool window | Sbt Shell tool window | `R_Tools`, `R_Jobs`, `RTableEditorProvider` | **FOUNDATIONAL (Expand to Remix IDE)** |
| **MCP (Model Context Protocol) Server** | **None** (IDE MCP tools only) | **None** | **None** | **None** | **None** | **UNIQUE COMPETITIVE ADVANTAGE** |

---

## 2. Detailed Subsystem XML Audit

### 2.1 Project Creation, Module Builders & Templates

#### Reference Implementations
- **`intellij-rust`**:
  ```xml
  <moduleType id="RUST_MODULE" implementationClass="org.rust.ide.module.RsModuleType"/>
  <moduleBuilder builderClass="org.rust.ide.module.RsModuleBuilder"/>
  <projectOpenProcessor id="Cargo" implementation="org.rust.cargo.project.CargoProjectOpenProcessor"/>
  <directoryProjectGenerator implementation="org.rust.ide.newProject.RsDirectoryProjectGenerator"/>
  <newProjectWizard.language implementation="org.rust.ide.newProject.RsNewProjectWizard"/>
  ```
- **`Rplugin`**:
  ```xml
  <moduleType id="R_MODULE" implementationClass="org.jetbrains.r.projectGenerator.builder.RModuleType"/>
  <projectTemplatesFactory implementation="org.jetbrains.r.projectGenerator.template.RTemplatesFactory" />
  <directoryProjectGenerator implementation="org.jetbrains.r.projectGenerator.template.REmptyProjectGenerator"/>
  <directoryProjectGenerator implementation="org.jetbrains.r.projectGenerator.template.RPackageProjectGenerator"/>
  ```

#### Current Midnight Plugin State
- Midnight currently provides file templates (`<internalFileTemplate name="Compact Contract"/>`, etc.) and a custom file creation action (`CompactCreateFileAction`).
- **Missing**:
  1. `directoryProjectGenerator`: No template generator when the user selects **File \u2192 New \u2192 Project**.
  2. `newProjectWizard.language`: No integration with IntelliJ's modern New Project Wizard sidebar (which groups projects by language: Java, Kotlin, Rust, Python, etc.).
  3. `moduleBuilder` & `moduleType`: Cannot create a dedicated Midnight Compact module within a multi-module enterprise repository.
  4. Project templates for standard Midnight contract topologies (e.g. Counter, Private Token/Zerocash, DAO/Governance, Oracle Consumer).

---

### 2.2 Stub Indexing & Large-Scale Codebase Performance

#### Reference Implementations
- **`intellij-rust`**:
  ```xml
  <stubElementTypeHolder class="org.rust.lang.core.psi.RsElementTypes" externalIdPrefix="rust."/>
  <stubIndex implementation="org.rust.lang.core.stubs.index.RsNamedElementIndex"/>
  <stubIndex implementation="org.rust.lang.core.stubs.index.RsImplIndex"/>
  <stubIndex implementation="org.rust.lang.core.stubs.index.RsTypeAliasIndex"/>
  <stubIndex implementation="org.rust.lang.core.stubs.index.RsMacroIndex"/>
  ```
- **`intellij-elixir`**:
  ```xml
  <stubElementTypeHolder class="org.elixir_lang.psi.stub.type.NamedStubElementTypes"/>
  <stubIndex implementation="org.elixir_lang.psi.stub.index.AllName"/>
  <stubIndex implementation="org.elixir_lang.psi.stub.index.Modular"/>
  ```

#### Current Midnight Plugin State
- Midnight currently uses AST-based parsing (`CompactResolveUtil`) traversing the in-memory AST for resolution and search.
- While fast for projects under 100 files, cross-file imports and Go To Symbol require parsing each file into a full PSI tree.
- **Missing**:
  1. `stubElementTypeHolder`: Compact top-level contracts, circuits, witnesses, structs, and enubs are not serialized into fast binary stub trees.
  2. `stubIndex`: No `CompactNamedElementIndex`, `CompactContractIndex`, or `CompactCircuitIndex` to provide millisecond lookup across thousands of files without disk parsing.

---

### 2.3 Status Bar Widgets & Real-Time Environment Feedback

#### Reference Implementations
- **`intellij-elixir`**:
  ```xml
  <statusBarWidgetFactory id="org.elixir_lang.status_bar.widget_factory"
                          implementation="org.elixir_lang.status_bar.WidgetFactory"/>
  ```
- **`intellij-scala`**:
  ```xml
  <statusBarWidgetFactory id="ScalaHighlightingMode"
                          implementation="org.jetbrains.plugins.scala.components.ScalaHighlightingModeWidgetFactory"
                          order="before Position"/>
  ```
- **`Rplugin`**:
  ```xml
  <statusBarWidgetFactory id="rInterpreterWidget"
                          implementation="org.jetbrains.r.configuration.RInterpreterBarWidgetFactory"
                          order="after CodeStyleStatusBarWidget, before pythonInterpreterWidget"/>
  ```

#### Current Midnight Plugin State
- The Midnight compiler version is currently displayed only when opening the right sidebar tool window or inspecting project settings.
- **Missing**:
  1. `statusBarWidgetFactory`: A dedicated status bar widget in the bottom-right corner displaying:
     - Active Compact Compiler Version (e.g. `Compact 0.34.0 (WSL)` or `Auto (0.34.0)`).
     - Clickable menu to instantly switch compiler version or open compiler settings.
     - Connected Network indicator: `Midnight Devnet (9944)` / `Midnight Testnet` / `Disconnected`.
     - Proof Server Status: `Proof Server OK (6300)` / `Offline`.

---

### 2.4 Refactoring Capabilities

#### Reference Implementations
- **`intellij-rust`**:
  ```xml
  <renamePsiElementProcessor implementation="org.rust.ide.refactoring.RsRenameProcessor" order="first" id="rsRenameProcessor"/>
  <renamePsiElementProcessor implementation="org.rust.ide.refactoring.RsDirectoryRenameProcessor" order="first, before rsRenameProcessor"/>
  <suggestedRefactoringSupport language="Rust" implementationClass="org.rust.ide.refactoring.suggested.RsSuggestedRefactoringSupport"/>
  <refactoring.changeSignatureUsageProcessor implementation="org.rust.ide.refactoring.changeSignature.RsChangeSignatureUsageProcessor" id="Rust"/>
  <inlineActionHandler implementation="org.rust.ide.refactoring.inline.RsInlineFunctionHandler"/>
  <inlineActionHandler implementation="org.rust.ide.refactoring.inline.RsInlineLocalVariableHandler"/>
  ```
- **`Rplugin`**:
  ```xml
  <renameInputValidator implementation="org.jetbrains.r.refactoring.rename.RRenameInputValidator"/>
  <nameSuggestionProvider implementation="org.jetbrains.r.refactoring.rename.RNameSuggestionProvider"/>
  <renamePsiElementProcessor implementation="org.jetbrains.r.refactoring.rename.RenameRPsiElementProcessor" id="RPsiElement" order="first"/>
  <inlineActionHandler implementation="org.jetbrains.r.refactoring.inline.RInlineAssignmentHandler"/>
  ```

#### Current Midnight Plugin State
- Midnight currently implements `CompactNamesValidator` and `CompactRefactoringSupportProvider` enabling basic inplace renaming of identifiers.
- **Missing**:
  1. `renamePsiElementProcessor`: Custom pre/post processing during rename (e.g. updating cross-file `include` / `import` references, renaming matching test fixtures or TypeScript bindings).
  2. `inlineActionHandler`: Inlining constant declarations (`const X = 42;`) into call sites.
  3. `refactoring.extractMethod`: Extracting statement sequences into a new `circuit` or private helper.
  4. `suggestedRefactoringSupport`: IntelliJ's modern intention popping up after editing a parameter signature.

---

### 2.5 Code Vision & Metrics

#### Reference Implementations
- **`intellij-rust`**:
  ```xml
  <vcs.codeVisionLanguageContext language="Rust" implementationClass="org.rust.ide.hints.codeVision.RsVcsCodeVisionContext"/>
  <codeInsight.daemonBoundCodeVisionProvider implementation="org.rust.ide.hints.codeVision.RsReferenceCodeVisionProvider"/>
  <codeInsight.daemonBoundCodeVisionProvider implementation="org.rust.ide.hints.codeVision.RsImplementationsCodeVisionProvider"/>
  ```

#### Current Midnight Plugin State
- Midnight has line markers (`CompactLineMarkerProvider`) for witness, circuit, disclose, and ledger declarations.
- **Missing**:
  1. `codeInsight.daemonBoundCodeVisionProvider`: Displaying usage counts and implementation counts inline directly above contract declarations, circuit definitions, and structs:
     - `3 usages | 1 test | 254 ZK constraints` rendered directly above `circuit transfer(...)`.

---

### 2.6 Testing Framework Integration

#### Reference Implementations
- **`intellij-rust`**:
  ```xml
  <runLineMarkerContributor language="Rust" implementationClass="org.rust.ide.lineMarkers.CargoTestRunLineMarkerContributor"/>
  <testSourcesFilter implementation="org.rust.cargo.project.model.RsTestSourcesFilter"/>
  ```
- **`intellij-elixir`**:
  ```xml
  <testFinder implementation="org.elixir_lang.test.TestFinder"/>
  <programRunner implementation="org.elixir_lang.mix.test.Runner"/>
  ```

#### Current Midnight Plugin State
- Midnight has run configurations for compiling contracts (`CompactRunConfiguration`), but no test runners.
- **Missing**:
  1. `runLineMarkerContributor` for tests: Detecting contract test functions or Jest/Vitest contract tests and showing a green play button.
  2. `testFinder`: Navigating between `MyContract.compact` and `MyContractTest.ts` or `test_my_contract.compact`.
  3. Test Run Configuration: Streaming test results into IntelliJ's native graphical test runner tool window.

---

### 2.7 Interactive Consoles & Visualizers

#### Reference Implementations
- **`Rplugin`**:
  ```xml
  <toolWindow id="R_Console" anchor="bottom" canCloseContents="true"
              icon="com.intellij.r.psi.icons.RIcons.ToolWindow.RConsole"
              factoryClass="org.jetbrains.r.console.RConsoleToolWindowFactory"/>
  <fileEditorProvider implementation="org.jetbrains.r.run.visualize.RTableEditorProvider"/>
  <scratch.rootType implementation="org.jetbrains.r.console.RConsoleRootType"/>
  ```
- **`intellij-scala`**:
  ```xml
  <scratch.rootType implementation="org.jetbrains.plugins.scala.worksheet.WorksheetRootType"/>
  <fileEditorProvider implementation="org.jetbrains.plugins.scala.worksheet.ui.WorksheetEditorProvider"/>
  ```

#### Current Midnight Plugin State
- Midnight has `CompactCompilerToolWindowFactory` with compiler version selection and Run Contract action.
- **Missing**:
  1. Interactive scratchpad / REPL: A Compact scratchpad executing circuits against in-memory mock ledger state.
  2. Visualizers: Table/Tree views for contract ledger storage, UTXO sets, and ZK constraint matrices.

---

## 3. Summary of High-Priority Extension Points to Add

The following XML extension points are targeted for implementation in the future plan:

```xml
<!-- 1. Modern Project Wizards & Templates -->
<directoryProjectGenerator implementation="dev.verloren.midnight.ide.newProject.CompactDirectoryProjectGenerator"/>
<newProjectWizard.language implementation="dev.verloren.midnight.ide.newProject.CompactNewProjectWizard"/>
<moduleType id="COMPACT_MODULE" implementationClass="dev.verloren.midnight.ide.module.CompactModuleType"/>
<moduleBuilder builderClass="dev.verloren.midnight.ide.module.CompactModuleBuilder"/>

<!-- 2. Status Bar Widget -->
<statusBarWidgetFactory id="CompactToolchainWidget"
                        implementation="dev.verloren.midnight.toolchain.ui.CompactStatusBarWidgetFactory"
                        order="before Position"/>

<!-- 3. Stub Indexing for Instant Project-Wide Lookup -->
<stubElementTypeHolder class="dev.verloren.midnight.psi.CompactElementTypes" externalIdPrefix="compact."/>
<stubIndex implementation="dev.verloren.midnight.stubs.index.CompactNamedElementIndex"/>
<stubIndex implementation="dev.verloren.midnight.stubs.index.CompactCircuitIndex"/>

<!-- 4. Code Vision Usage & Constraint Metrics -->
<codeInsight.daemonBoundCodeVisionProvider
    implementation="dev.verloren.midnight.codeVision.CompactUsagesCodeVisionProvider"/>
<codeInsight.daemonBoundCodeVisionProvider
    implementation="dev.verloren.midnight.codeVision.CompactConstraintMetricsCodeVisionProvider"/>

<!-- 5. Advanced Refactorings -->
<renamePsiElementProcessor implementation="dev.verloren.midnight.refactoring.CompactRenameProcessor" order="first"/>
<inlineActionHandler implementation="dev.verloren.midnight.refactoring.CompactInlineConstantHandler"/>

<!-- 6. Blockchain Remix-Style Explorer Tool Window -->
<toolWindow id="Midnight_Explorer" anchor="right" canCloseContents="false"
            icon="dev.verloren.midnight.MidnightIcons.SIDEBAR_TOOLWINDOW"
            factoryClass="dev.verloren.midnight.toolwindow.explorer.MidnightExplorerToolWindowFactory"/>

<!-- 7. Model Context Protocol (MCP) Server Integration -->
<postStartupActivity implementation="dev.verloren.midnight.mcp.CompactMcpStartupActivity"/>
```
