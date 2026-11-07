# ADR-035: Streamlined Multi-Axis Responsive Tool Window & Curated Status Bar Toolchain Manager

- **Status**: Accepted
- **Date**: 2026-11-06
- **Subsystem**: Tool Window, Status Bar & Toolchain Management
- **Preceding ADRs**: ADR-011 (Toolchain Discovery), ADR-012 (Isolated Multi-Version Compiler Storage), ADR-013 (Remix Tool Window), ADR-014 (Status Bar Toolchain Monitor), ADR-015 (Run Configurations)

---

## 1. Context & Problem Statement

The initial implementations of the **Compact Compiler Sidebar Tool Window** (ADR-013) and **Status Bar Toolchain Widget** (ADR-014) exhibited architectural redundancy, visual bloat, and rigid UI constraints:
1. **Redundant Responsibilities**: Both the sidebar tool window and the status bar popup attempted to manage version downloads and installations simultaneously. The tool window displayed a vertical wall of 12 full-width cards taking up over 600 vertical pixels.
2. **Horizontal & Vertical Non-Responsiveness**:
   - At narrow sidebar widths (< 380px), the "RUN CONTRACT" button collided horizontally with the "Skip ZK" checkbox.
   - At short vertical heights (< 280px) or when docked at the bottom of the IDE, the window lacked a scroll container (`JBScrollPane`), causing controls to be clipped off-screen into an unreachable void.
3. **Rigid Color Palette & Contrast Degradation**: The custom-painted green action button used hardcoded RGB values (`new Color(46, 160, 67)`). In Light and High-Contrast themes, mouse hover applied a semi-transparent white wash that degraded foreground text contrast to near-unreadable levels.
4. **Uninstalled Version Ambiguity & Download Lifecycle**: The dropdown list contained versions not present on disk. Selecting an uninstalled version had no confirmation guard, no cancellation fallback state machine, and no deletion capability.
5. **Code Bloat & Suppressions**: `CompactCompilerPanel.java` swelled to 740 lines plus 4 auxiliary card classes (~1,200 lines total), requiring baseline suppressions in `checkstyle-suppressions.xml`.

---

## 2. Decision Drivers & Architectural Invariants

1. **Strict Downward Dependency Hierarchy**: UI layers must delegate down to PSI, settings, and toolchain utilities without leaking state into domain models.
2. **Line & Method Limits ($\le 400$ Lines / $\le 40$ Lines)**: Zero Checkstyle suppressions permitted.
3. **Universal Viewport Fluidity (Two-Axis Responsiveness)**: The tool window must function with 100% usability whether docked Left, Right, Bottom, or Floating, and down to 150px width / 120px height.
4. **Theme Agnosticism**: Zero hardcoded RGB colors; all UI elements must derive styling from platform semantic tokens (`JBUI.CurrentTheme.*` and `JBColor.namedColor`).
5. **Deterministic State Machine**: When interacting with missing or uninstalled toolchains, the IDE must never crash, silently fall back to random binaries, or leave the run button in a broken state.

---

## 3. Evaluated Options

### Option 1: Monolithic All-In-One Sidebar (Rejected)
Keep version cards, downloads, uninstalls, and compile controls all crammed inside the sidebar tool window.
- *Downsides*: Giant vertical card list, high maintenance overhead, duplicates IntelliJ SDK management paradigms, high risk of layout clipping.

### Option 2: Pure Status Bar Management with Minimal Tool Window (Rejected)
Strip toolchains entirely from the tool window and only manage versions in the status bar.
- *Downsides*: The status bar tray is too small to display multi-dimensional metadata (Compiler + Language + Ledger) without heavy truncation.

### Option 3: Two-Tier Split Architecture (Selected)
- **Tool Window**: Serves as the **Active Contract Execution Dashboard**. Features a clean toolchain dropdown with status icons, collapsible advanced options (`--skip-zk`, output dir, custom flags), and a native platform Run button wrapped in a borderless `JBScrollPane`.
- **Status Bar**: Serves as the **Ambient Toolchain Monitor & Quick Switcher**. Shows active toolchain text. Clicking opens a curated speed-search popup with the **Top 5 Installed** (with delete buttons) + **Top 5 Available Releases** (with download buttons) + a direct link to Settings.

---

## 4. Decision Outcome & System Design

### 4.1 Tool Window Architecture (`CompactCompilerPanel`)
- **Layout Manager**: Root content wrapped in a borderless `JBScrollPane` (`VERTICAL_SCROLLBAR_AS_NEEDED`, `HORIZONTAL_SCROLLBAR_NEVER`). `CompactCompilerPanel` implements `javax.swing.Scrollable` with `getScrollableTracksViewportWidth() == true` to guarantee 100% horizontal stretch and fluid vertical scrolling.
- **Dropdown Renderer**: `ComboBox<ToolchainItem>` with left-aligned 16x16 icons:
  - Installed: `AllIcons.Actions.Checked` (or `MidnightIcons.FILE`)
  - Not Installed / Cloud: `AllIcons.Actions.Download`
- **Native Primary Run Button**: Standard IntelliJ action button using `putClientProperty("gotItButton", true)` or `JBUI.CurrentTheme.Button.focusBorderColor(true)`, eliminating theme contrast bugs.
- **Collapsible Advanced Options**: Dedicated `CompactAdvancedOptionsPanel` toggled via `▶ Advanced Options` link.

### 4.2 Status Bar Architecture (`CompactStatusBarWidget` & `CompactStatusBarPopup`)
- **Tray Label**: `Compact: v<ver> (<ledger>)`
- **Popup Structure**:
  1. Active version display.
  2. **Top 5 Installed Compilers** sorted descending by SemVer. Each unselected item features a trash action button (`AllIcons.Actions.GC`) invoking `CompactVersionManager.uninstallVersion()`. Active compiler is protected against deletion.
  3. **Top 5 Available Cloud Releases** with a download button (`AllIcons.Actions.Download`) invoking background installer.
  4. Shortcut: `⚙ Configure All Toolchains in Settings...`.

### 4.3 Download & Fallback State Machine

```
User selects uninstalled version V_target
                   │
                   ▼
Dialog: "Compact compiler V_target is not installed locally. Download now?"
                   │
         ┌─────────┴─────────┐
         ▼                   ▼
     [ YES ]              [ CANCEL ]
         │                   │
Spawns background task       ▼
Task.Backgroundable    Does machine have ANY other installed compiler?
         │                   │
         │           ┌───────┴───────┐
         │           ▼               ▼
         │        [ YES ]         [ NO ]
         │           │               │
         │     Revert dropdown to   Leave V_target selected.
         │     last working version. Transform Run button into:
         │     Run button enabled.   [ ⬇ Download V_target ]
         ▼
Download completes -> Select & Enable Run Button
```

---

## 5. Consequences & Architectural Benefits

- **File Length Limits**: Every class remains well under 300 lines ($\le 400$ ceiling).
- **Checkstyle Suppressions**: Zero suppressions required for tool window components.
- **High-DPI & Theme Resilient**: Seamless rendering across Dark, Light, High-Contrast, and third-party themes.
- **Zero Race Conditions**: Dropdown scrolling updates in-memory settings only; no synchronous I/O or AST re-parsing during rapid user interaction.
