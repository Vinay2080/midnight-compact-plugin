# ADR-014: Status Bar Toolchain Monitor & Rapid Version Switching Popup

## Status
Accepted (Phase 27 / Complete)

## Context & Problem Statement
In multi-contract repositories, developers frequently shift between contracts targeting different language versions. Navigating to **Preferences -> Languages & Frameworks -> Midnight Compact** or opening side tool windows simply to check or switch the active compiler introduces excessive friction.
A native status bar indicator is the industry standard in IntelliJ plugins (like Rust's Cargo or Python's interpreter switcher), allowing developers to:
1. Glance at the bottom IDE status bar to see the active toolchain and language version.
2. Click to open a lightweight popup to switch versions in under one second.
3. Trigger version downloads directly from the popup.

## Authoritative References
1. **IntelliJ Platform Status Bar Architecture**:
   - `StatusBarWidgetFactory` and modern coroutine-backed `EditorBasedStatusBarPopup`.
2. **Production JetBrains Implementations**:
   - `intellij-rust`: Cargo toolchain status bar widget.
   - Python plugin: Python Interpreter switcher status bar popup.

## Decision
1. **Editor-Based Status Bar Widget (`CompactStatusBarWidget`)**:
   - Extends `EditorBasedStatusBarPopup`, tying widget visibility and updates to active editor tab transitions.
   - Only appears when editing `.compact` files or when a Compact project is detected.
   - Subscribes to the project message bus topic `CompactCompilerEventListener.TOPIC` to update instantly whenever the compiler version changes.
2. **Version Mapping Display**:
   - Computes concise status text showing both toolchain version and language version:
     - `Compact: v0.34.0 (0.26.0)`
     - `Compact: Not Configured` (with warning badge).
3. **Interactive Switching Popup (`CompactStatusBarPopup`)**:
   - Built using `JBPopupFactory.getInstance().createListPopup()`.
   - Lists all locally installed compiler versions with checkmarks on the active selection.
   - Lists available remote releases with one-click background installation.
   - Provides direct actions to open Settings or reveal the Compiler Tool Window.

## Scalability & Anti-Hardcoding Evaluation
- **Is it hardcoded?**: No. Dynamic discovery queries `CompactVersionManager.getInstalledVersions()` and `KNOWN_VERSIONS` at runtime.
- **Is it non-blocking?**: Yes. The widget uses asynchronous computation (`computeWidgetInfo()`) and IntelliJ coroutine scopes (`CoroutineScope`), guaranteeing zero UI freezes.

## Feature Implementation Map
- Widget Factory: [`CompactStatusBarWidgetFactory.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/statusbar/CompactStatusBarWidgetFactory.java)
- Widget Implementation: [`CompactStatusBarWidget.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/statusbar/CompactStatusBarWidget.java)
- Popup Menu: [`CompactStatusBarPopup.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/statusbar/CompactStatusBarPopup.java)
- Event Topic: [`CompactCompilerEventListener.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/toolwindow/CompactCompilerEventListener.java)
- Registration: [`plugin.xml`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/resources/META-INF/plugin.xml) (`<statusBarWidgetFactory>`)
- Unit Tests: [`CompactStatusBarWidgetTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/statusbar/CompactStatusBarWidgetTest.java)

## Consequences & Future Maintenance
- Any operation modifying the active toolchain (settings dialog, quick-fixes, tool window dropdown) must broadcast `CompactCompilerEventListener.TOPIC` to notify the status bar immediately.
