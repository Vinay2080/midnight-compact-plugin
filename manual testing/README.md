# Comprehensive Manual Testing & Demonstration Guide

This guide covers interactive manual verification and demonstration of all **17 core features** across **Person 1, Person 2, Person 3, and Person 4** implemented for the Compact smart contract language plugin in IntelliJ IDEA:

* **👤 Person 1: All Hard Features (The Deep Architecture Owner - 5 Features)**
* **👤 Person 2: Navigation & Symbol Intelligence (4 Features)**
* **👤 Person 3: Editor Ergonomics & Live Typing (4 Features)**
* **👤 Person 4: Code Structure, Formatting & UI (4 Features)**

---

## 📋 Master Features & Shortcut Quick Reference

### 👤 Person 1: All Hard Features (Deep Architecture)
| # | Feature | Key Shortcut (Win/Linux) | Key Shortcut (macOS) | Subfolder |
|---|---|---|---|---|
| 1 | **Semantic Inspections & Intentions** | `Alt + Enter` | `Option + Enter` | `13_semantic_inspections_and_intentions/` |
| 2 | **Compiler Run Action** | `Ctrl + Shift + F10` | `Cmd + Shift + R` | `14_compiler_run_action/` |
| 3 | **Remix-Style Compiler Panel** | Click right sidebar tool window | Click right sidebar tool window | `15_remix_compiler_panel/` |
| 4 | **Contextual Code Completion** | `Ctrl + Space` | `Ctrl + Space` | `16_contextual_completion/` |
| 5 | **Go to Declaration** | `Ctrl + B` (or `Ctrl + Click`) | `Cmd + B` (or `Cmd + Click`) | `17_goto_declaration/` |

### 👤 Person 2: Navigation & Symbol Intelligence
| # | Feature | Key Shortcut (Win/Linux) | Key Shortcut (macOS) | Subfolder |
|---|---|---|---|---|
| 6 | **Find Usages** | `Alt + F7` (or `Ctrl + B`) | `Option + F7` (or `Cmd + B`) | `01_find_usages/` |
| 7 | **Go to Type Declaration** | `Ctrl + Shift + B` | `Cmd + Shift + B` | `02_goto_type_declaration/` |
| 8 | **Gutter Line Markers** | Click gutter icon | Click gutter icon | `03_gutter_line_markers/` |
| 9 | **Quick Documentation** | `Ctrl + Q` (or hover) | `F1` / `Cmd + J` (or hover) | `04_quick_documentation/` |

### 👤 Person 3: Editor Ergonomics & Live Typing
| # | Feature | Key Shortcut (Win/Linux) | Key Shortcut (macOS) | Subfolder |
|---|---|---|---|---|
| 10 | **Smart Complete Statement** | `Ctrl + Shift + Enter` | `Cmd + Shift + Enter` | `05_smart_complete_statement/` |
| 11 | **Parameter Info** | `Ctrl + P` | `Cmd + P` | `06_parameter_info/` |
| 12 | **Inlay Parameter Hints** | Inline non-editable badges | Inline non-editable badges | `07_inlay_parameter_hints/` |
| 13 | **Syntax Highlighting** | View in editor / Color Scheme | View in editor / Color Scheme | `08_syntax_highlighting/` |

### 👤 Person 4: Code Structure, Formatting & UI
| # | Feature | Key Shortcut (Win/Linux) | Key Shortcut (macOS) | Subfolder |
|---|---|---|---|---|
| 14 | **Code Formatting** | `Ctrl + Alt + L` | `Cmd + Option + L` | `09_code_formatting/` |
| 15 | **Inplace Rename Refactoring** | `Shift + F6` | `Shift + F6` | `10_inplace_rename/` |
| 16 | **Structure View Outline** | `Alt + 7` (or `Ctrl + F12`) | `Cmd + 7` (or `Cmd + F12`) | `11_structure_view/` |
| 17 | **Status Bar Compiler Monitor** | Bottom-right status bar / click | Bottom-right status bar / click | `12_status_bar_monitor/` |

---

## 📁 Complete Folder Structure

```
manual testing/
├── 01_find_usages/
│   ├── TokenDeclarations.compact      # Struct, enum, circuit, witness, param & constant declarations
│   ├── TokenOperations.compact        # Cross-file circuit calls, enum usages, struct instantiations
│   └── TokenAudit.compact             # Additional cross-file usages verifying project-wide indexing
├── 02_goto_type_declaration/
│   ├── TypesVault.compact             # Structs, nested structs, enums, type aliases, contract interface
│   └── TypeNavigationDemo.compact     # Local bindings, parameters, field chains, ledgers navigating to types
├── 03_gutter_line_markers/
│   ├── ITokenExchange.compact         # Interface contract & abstract circuit signatures (Down markers)
│   └── TokenExchangeService.compact   # Implementation contract (Up markers, witness, ledger, disclose)
├── 04_quick_documentation/
│   ├── DocumentedContracts.compact    # Rich /// and /** */ doc comments, JSDoc tags & markdown styles
│   └── DocumentationUsageDemo.compact # Call sites & field accesses displaying rich HTML hover tooltips
├── 05_smart_complete_statement/
│   └── SmartEnterScenarios.compact    # Live typing templates for headers, braces, semicolons & assignments
├── 06_parameter_info/
│   ├── ParameterInfoDeclarations.compact # Multi-param circuits, witnesses, constructors & structs
│   └── ParameterInfoDemo.compact      # Active parameter highlighting, nested calls & struct literal fields
├── 07_inlay_parameter_hints/
│   ├── InlayHintsDeclarations.compact # Circuits & witnesses with descriptive parameter names
│   └── InlayHintsDemo.compact         # Inline parameter badges, literal hints & name-matching suppression
├── 08_syntax_highlighting/
│   └── SyntaxShowcase.compact         # Comprehensive showcase of all lexer tokens, colors & semantic styles
├── 09_code_formatting/
│   └── UnformattedCodeShowcase.compact# Deliberately unformatted contract to test 2-space indentation & operator spacing
├── 10_inplace_rename/
│   ├── RenameTargetDeclarations.compact # Struct, enum, circuit, and type targets for safe renaming
│   └── RenameReferenceUsages.compact  # Cross-file rename verification, local renaming, keyword collision tests
├── 11_structure_view/
│   └── HierarchicalStructureShowcase.compact # Deep nested hierarchy showing modules, contracts, structs, enums & circuits
├── 12_status_bar_monitor/
│   └── CompilerVersionMonitor.compact # Status bar compiler toolchain widget & quick-switch popup demonstration
├── 13_semantic_inspections_and_intentions/
│   └── InspectionsAndIntentionsShowcase.compact # Unused vars, undisclosed witness quick-fixes & intentions
├── 14_compiler_run_action/
│   └── CompileTargetContract.compact  # Executable contract testing Run Configurations & console hyperlinks
├── 15_remix_compiler_panel/
│   └── CompilerToolWindowShowcase.compact # Remix-style sidebar tool window, release switcher & 1-click execution
├── 16_contextual_completion/
│   └── ContextualCompletionShowcase.compact # Struct dot-completion, type-position vs value-position completion
├── 17_goto_declaration/
│   ├── ExternalSymbolProvider.compact # Cross-file symbol provider for declaration resolution
│   └── GotoDeclarationDemo.compact    # Cross-file jumps, lexical scoping, and variable shadowing resolution
└── README.md                          # Master manual testing specification
```

---

## 👤 Person 1: All Hard Features (The Deep Architecture Owner)

### Feature 1: Semantic Inspections & Intentions (`Alt + Enter` / `Option + Enter`)
* **Files:** [`manual testing/13_semantic_inspections_and_intentions/InspectionsAndIntentionsShowcase.compact`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/manual%20testing/13_semantic_inspections_and_intentions/InspectionsAndIntentionsShowcase.compact)
* **How to Test:**
  1. Open `InspectionsAndIntentionsShowcase.compact`.
  2. **Unused Local Variable Quick-Fix:** Look at line 41 (`const unusedBuffer = ...`). Notice it is highlighted as an unused variable. Place caret on it, press `Alt + Enter`, and select **"Remove unused variable 'unusedBuffer'"** -> safely deletes the statement.
  3. **Specify Type Explicitly Intention:** Place caret on `factor` at line 45, press `Alt + Enter`, and select **"Specify type explicitly"** -> transforms into `const factor: Uint<64> = 2;`.
  4. **Invert 'if' Condition Intention:** Place caret on `if (netAmount > 100)` at line 51, press `Alt + Enter`, and select **"Invert 'if' condition"** -> cleanly inverts the condition (`netAmount <= 100`) and swaps the if and else branch blocks.
  5. **Undisclosed Witness Quick-Fix:** Notice that calling witness queries directly without `disclose(...)` triggers an inspection warning. Pressing `Alt + Enter` offers **"Surround with disclose(...)"**.
  6. **Toggle Pure Modifier Intention:** Place caret on `circuit calculateTax` at line 67, press `Alt + Enter`, and select **"Make circuit pure"** -> prepends `pure` modifier. Place caret on `pure` and press `Alt + Enter` -> select **"Make circuit non-pure"**.
  7. **Toggle Export Modifier Intention:** Place caret on `contract EscrowStorage` at line 73, press `Alt + Enter`, and select **"Add 'export' modifier"** -> adds `export`.

### Feature 2: Compiler Run Action (`Ctrl + Shift + F10` / `Cmd + Shift + R`)
* **Files:** [`manual testing/14_compiler_run_action/CompileTargetContract.compact`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/manual%20testing/14_compiler_run_action/CompileTargetContract.compact)
* **How to Test:**
  1. Open `CompileTargetContract.compact`.
  2. Right-click inside the editor or in the Project Explorer on the file and select **Run 'Compile CompileTargetContract.compact'** (or press `Ctrl + Shift + F10`).
  3. **Observed Result:**
     - IntelliJ synthesizes a `CompactRunConfiguration` on-the-fly via `CompactRunConfigurationProducer`.
     - Spawns the compiler toolchain process via `OSProcessHandler`.
     - Executes compilation in the Run console window.
     - Console error hyperlinks filter compiler outputs: clicking an error message line instantly repositions the editor caret directly on the error in the Compact source file!

### Feature 3: Remix-Style Compiler Tool Window Dashboard
* **Files:** [`manual testing/15_remix_compiler_panel/CompilerToolWindowShowcase.compact`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/manual%20testing/15_remix_compiler_panel/CompilerToolWindowShowcase.compact)
* **How to Test:**
  1. Open `CompilerToolWindowShowcase.compact` in the editor.
  2. Click the **"Compact Compiler"** tool window on the right IDE sidebar.
  3. **Active Contract Context Card:** Observe that the active contract is automatically recognized as `CompilerToolWindowShowcase.compact` with detected pragma `language_version >= 0.26.0`.
  4. **Toolchain Version Switcher:** Inspect the dropdown and release cards showing installed compiler versions sorted with SemVer logic, plus remote downloadable GitHub releases.
  5. **1-Click Compilation:** Click the **"Compile Contract"** button to trigger instant compilation.
  6. **Options:** Toggle the `--skip-zk` checkbox for fast iteration during early prototyping.

### Feature 4: Contextual Code Completion (`Ctrl + Space`)
* **Files:** [`manual testing/16_contextual_completion/ContextualCompletionShowcase.compact`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/manual%20testing/16_contextual_completion/ContextualCompletionShowcase.compact)
* **How to Test:**
  1. Open `ContextualCompletionShowcase.compact`.
  2. **Struct Member Dot-Completion:**
     - Place caret after `user.` at line 46 and press `Ctrl + Space`.
     - **Observed Result:** IntelliJ suggests ONLY declared struct fields (`accountId`, `reputation`, `isVerified`).
  3. **Type Position Completion:**
     - Place caret after `target:` at line 53 and press `Ctrl + Space`.
     - **Observed Result:** Suggests type names (`UserProfile`, `Field`, `Uint<64>`, `Bytes<32>`), excluding local expressions and variables.
  4. **Value / Expression Position Completion:**
     - Place caret after `const feeVal = ` at line 57 and press `Ctrl + Space`.
     - **Observed Result:** Suggests valid values in scope (`GLOBAL_BASE_FEE`, `target`, `default<...>`).

### Feature 5: Go to Declaration (`Ctrl + B` / `Cmd + B` / `Ctrl + Click`)
* **Files:** [`manual testing/17_goto_declaration/ExternalSymbolProvider.compact`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/manual%20testing/17_goto_declaration/ExternalSymbolProvider.compact), [`GotoDeclarationDemo.compact`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/manual%20testing/17_goto_declaration/GotoDeclarationDemo.compact)
* **How to Test:**
  1. Open `GotoDeclarationDemo.compact`.
  2. **Cross-File Circuit Resolution:** Place caret on `computeExternalBonus` at line 46 and press `Ctrl + B` -> jumps directly to the circuit declaration in `ExternalSymbolProvider.compact`.
  3. **Cross-File Struct Resolution:** Place caret on `ExternalConfig` at line 47 and press `Ctrl + B` -> jumps directly to `struct ExternalConfig` in `ExternalSymbolProvider.compact`.
  4. **Lexical Scoping & Variable Shadowing:**
     - Place caret on `shadowedVar` inside the `if` block at line 54 and press `Ctrl + B` -> jumps to `const shadowedVar = 999;` at line 53, confirming that lexical shadowing is strictly respected.
  5. **Function Parameter Resolution:** Place caret on `roundParam` at line 46 and press `Ctrl + B` -> jumps to the parameter declaration on line 43.
  6. **Standard Library Resolution:** Place caret on `CompactStandardLibrary` at line 4 and press `Ctrl + B` -> jumps straight to the virtual standard library definition managed by the plugin.

---

## 👤 Person 2: Navigation & Symbol Intelligence

### Feature 6: Find Usages (`Alt + F7`)
* **Files:** `01_find_usages/TokenDeclarations.compact`, `TokenOperations.compact`, `TokenAudit.compact`
* **How to Test:** Place caret on circuit `mintToken` in `TokenDeclarations.compact` and press `Alt + F7`. Finds all 3 usages across all files. Tests parameter local scope and struct field usages.

### Feature 7: Go to Type Declaration (`Ctrl + Shift + B`)
* **Files:** `02_goto_type_declaration/TypesVault.compact`, `TypeNavigationDemo.compact`
* **How to Test:** Place caret on variables, parameters, field chains, or ledger states in `TypeNavigationDemo.compact` and press `Ctrl + Shift + B` -> jumps straight past bindings to the underlying struct, enum, or contract interface.

### Feature 8: Gutter Line Markers
* **Files:** `03_gutter_line_markers/ITokenExchange.compact`, `TokenExchangeService.compact`
* **How to Test:** Click Down Arrow on `contract ITokenExchange` to navigate to implementations; click Up Arrow on `contract implements` to jump to interface. Displays icons for exported circuits (lightning), witnesses, ledgers, and ZK disclose boundaries.

### Feature 9: Quick Documentation (`Ctrl + Q` / Hover)
* **Files:** `04_quick_documentation/DocumentedContracts.compact`, `DocumentationUsageDemo.compact`
* **How to Test:** Hover over circuits, structs, and enums to view rich HTML doc popups with `@param`, `@return`, `@throws`, `@see`, `@since`, and markdown formatting. Supports Reader Mode (`Ctrl + Alt + Q`).

---

## 👤 Person 3: Editor Ergonomics & Live Typing

### Feature 10: Smart Complete Statement (`Ctrl + Shift + Enter`)
* **Files:** `05_smart_complete_statement/SmartEnterScenarios.compact`
* **How to Test:** Type partial circuit headers, struct declarations, or unclosed `if` statements and press `Ctrl + Shift + Enter` -> auto-completes braces, appends return types, inserts missing semicolons, and positions the caret.

### Feature 11: Parameter Info (`Ctrl + P`)
* **Files:** `06_parameter_info/ParameterInfoDeclarations.compact`, `ParameterInfoDemo.compact`
* **How to Test:** Press `Ctrl + P` inside circuit/witness calls or struct literals -> floating tooltip tracks active parameter index in bold and supports out-of-order struct fields.

### Feature 12: Inlay Parameter Hints
* **Files:** `07_inlay_parameter_hints/InlayHintsDeclarations.compact`, `InlayHintsDemo.compact`
* **How to Test:** View non-editable gray badges (`round:`, `secretKey:`) before call arguments. Suppresses redundant hints when variable name matches parameter name.

### Feature 13: Syntax Highlighting & Color Settings Page
* **Files:** `08_syntax_highlighting/SyntaxShowcase.compact`
* **How to Test:** View rich colorization for pragmas, keywords, types, numbers, string escapes, doc tags, and ledger mutations. Customizable under **Settings -> Editor -> Color Scheme -> Compact**.

---

## 👤 Person 4: Code Structure, Formatting & UI

### Feature 14: Code Formatting (`Ctrl + Alt + L`)
* **Files:** `09_code_formatting/UnformattedCodeShowcase.compact`
* **How to Test:** Open unformatted file and press `Ctrl + Alt + L` -> applies 2-space indentation, binary operator spacing, and clean block alignment.

### Feature 15: Inplace Rename Refactoring (`Shift + F6`)
* **Files:** `10_inplace_rename/RenameTargetDeclarations.compact`, `RenameReferenceUsages.compact`
* **How to Test:** Rename circuits, structs, or variables across files with `Shift + F6`. Prevents collisions with reserved language keywords.

### Feature 16: Structure View Outline (`Alt + 7` / `Ctrl + F12`)
* **Files:** `11_structure_view/HierarchicalStructureShowcase.compact`
* **How to Test:** Press `Alt + 7` to view full hierarchical symbol tree of contracts, circuits, structs, and enums.

### Feature 17: Status Bar Compiler Monitor
* **Files:** `12_status_bar_monitor/CompilerVersionMonitor.compact`
* **How to Test:** View active compiler version on bottom-right status bar and click to open the quick-switch toolchain popup.
