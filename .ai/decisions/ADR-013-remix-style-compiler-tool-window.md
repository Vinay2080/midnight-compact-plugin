# ADR-013: Remix-Style Compiler Tool Window & Real-Time Dynamic File Tracking

## Status
Accepted (Phase 26 / Complete)

## Context & Problem Statement
Developers coming to Midnight from Web3 ecosystems (Ethereum/Solidity) expect an intuitive, dedicated compiler sidebar resembling Remix IDE:
1. Instant visibility of the currently open contract file.
2. Real-time extraction and verification of the contract's `pragma language_version` directive.
3. Rapid selection, downloading, and switching between compiler versions without navigating deep into Settings dialogs.
4. One-click compilation with direct links to compile artifacts and line-numbered compiler error diagnostics.
5. In classic IDE architectures, users must create a separate Run Configuration for every single file, adding heavy UI friction for fast iterative smart contract prototyping.

## Authoritative References
1. **Ethereum Remix IDE Architecture**:
   - Solidity Compiler panel workflow: Active Contract selector, Compiler version dropdown, Auto-compile checkbox, Compile button, Compilation Details / ABI / Bytecode view.
2. **Production JetBrains Implementations**:
   - `intellij-solidity`: Compilation tool window integrations.
   - `intellij-scala`: Compile Server tool window and status monitors.

## Decision
1. **Dedicated Tool Window Factory (`CompactCompilerToolWindowFactory`)**:
   - Registers a tool window on the right IDE stripe with `MidnightIcons.COMPACT_13`.
   - Marked `DumbAware` so developers can configure toolchains and inspect compiler versions even during project background indexing.
2. **Dynamic Real-Time File Tracking**:
   - Subscribes to `FileEditorManagerListener.FILE_EDITOR_MANAGER` to automatically detect when the user switches tabs.
   - Attaches a `DocumentListener` to track live changes to the `pragma language_version` line in the active document.
   - Displays a dynamic status banner (Green for compatible toolchain, Yellow/Red warning when the selected compiler does not satisfy the pragma range).
3. **Integrated Compiler Actions**:
   - Dropdown listing all known and installed Midnight compiler releases (`0.34.0`, `0.31.1`, `0.30.0`, etc.).
   - If a version is selected that is not yet installed locally, the panel displays an **Install** action that triggers background downloading and unpacks into `~/.compact/versions/<version>/`.
   - One-click **Compile <filename>** button automatically constructs a transient `CompactRunConfiguration` or executes the compiler process with live console output in the IDE Run tool window.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. It queries the live PSI tree (`CompactPragmaForm`) using `ReadAction.compute()` and updates the UI via `ApplicationManager.getApplication().invokeLater()`.
- **Is it thread-safe?**: Yes. File tracking and PSI reads occur safely in read actions, and Swing UI components are manipulated strictly on the EDT.

## Feature Implementation Map
- Tool Window Factory: [`CompactCompilerToolWindowFactory.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/toolwindow/CompactCompilerToolWindowFactory.java)
- Panel Component: [`CompactCompilerPanel.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/toolwindow/CompactCompilerPanel.java)
- Problem Utilities: [`CompactProblemUtil.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/annotator/CompactProblemUtil.java)
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) (`<toolWindow id="Compact Compiler">`)
- Unit Tests: [`CompactCompilerPanelTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/toolwindow/CompactCompilerPanelTest.java)

## Consequences & Future Maintenance
- The tool window panel serves as the user-facing hub for contract compilation.
- Future phases (deployments, proof generation) can add additional tabbed views or deploy buttons to `CompactCompilerPanel` without altering the core compiler runner.
