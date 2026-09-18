---
title: "Compact Compiler Tool Window Version Selection Sync"
date: 2026-09-18
severity: High
subsystem: toolwindow
affected_versions: [v1.2.0, v1.3.0, v1.3.1, v1.3.2]
fixed_versions: [v1.3.3]
related_issues: []
tags: [toolwindow, version-manager, quick-fix, intention, ui-sync, message-bus]
---

# Compact Compiler Tool Window Version Selection Sync

- **Date:** 2026-09-18
- **Feature / Component:** toolwindow / version / quick-fix
- **Severity:** High
- **Status:** Resolved

## 1. Executive Summary

When a user accepted an in-editor quick-fix (Alt+Enter) to resolve a Compact pragma version mismatch (e.g. switching from Compact `v0.25.0` to `v0.26.0` to satisfy `pragma language_version >= 0.18.0;`), the underlying project settings and compiler paths updated correctly, but the "Compact Compiler" side panel tool window did not reflect the new selection. The previous version remained highlighted as active, and version card statuses were not updated. The fix establishes comprehensive bidirectional event synchronization across the IntelliJ message bus (`CompactCompilerEventListener.TOPIC`), immediately updating card active states on EDT and asynchronously refreshing version card data.

## 2. Problem Statement & Symptom

When working with Compact smart contracts in IntelliJ IDEA:
1. A contract declared a pragma constraint requiring a higher language version than currently selected (e.g., `pragma language_version >= 0.18.0;` while compiler `0.25.0` / language `0.17.0` was active).
2. The inspection (`CompactPragmaVersionInspection`) correctly flagged the mismatch and offered the quick-fix (`CompactSwitchCompilerQuickFix`): `Switch project compiler to Compact v0.26.0 (Language v0.18.0)`.
3. When the user pressed `Alt + Enter` and applied the quick-fix, `MidnightProjectSettings.selectedCompilerVersion` and `MidnightSettingsState.compilerPath` were successfully updated to `0.26.0`.
4. However, in the "Compact Compiler" tool window on the right stripe:
   - The card for `0.25.0` remained visually highlighted with the active border and checkmark.
   - The card for `0.26.0` remained inactive and unselected.
   - In contrast, selecting a version directly by clicking a version card in the tool window updated the UI immediately.

## 3. Upstream Ground Truth & Specification

In IntelliJ Platform UI architecture and tool window design:
- Tool window components must maintain strict synchronization with project-level state.
- When an external action (such as an editor quick-fix, intention action, status bar switcher, or project settings configurable) alters project state, any open tool windows must react via decoupled message bus events (`MessageBusConnection.subscribe(...)`).
- UI component visual states (such as active card highlights) must update immediately on the Event Dispatch Thread (EDT) to provide zero-latency tactile feedback, while heavy I/O operations (such as binary inspection on disk or network checks) must run asynchronously off EDT.

## 4. Root Cause Analysis

Tracing the execution flow revealed:
1. `CompactSwitchCompilerQuickFix.invoke()` and `CompactSwitchCompilerVersionIntention.invoke()` call `CompactVersionManager.ensureAndSwitchVersion(...)`.
2. `CompactVersionManager.switchAndApplyVersion(...)` updates `MidnightProjectSettings.getInstance(project).selectedCompilerVersion` and calls `CompactProblemUtil.clearProblemsAndRestart(project, vFile)`.
3. `CompactProblemUtil.clearProblemsAndRestart` fires the message bus topic:
   ```java
   project.getMessageBus().syncPublisher(CompactCompilerEventListener.TOPIC).onCompilerStateChanged();
   ```
4. In `CompactCompilerPanel`, the subscription was registered as:
   ```java
   project.getMessageBus().connect(this).subscribe(CompactCompilerEventListener.TOPIC, (CompactCompilerEventListener) () -> updateActiveFileInfo(null));
   ```
5. In `CompactCompilerPanel.updateActiveFileInfo(...)`:
   - It only checked whether the pragma in the open document changed (`pragmaChanged`).
   - Because the pragma in the editor document did not change, `pragmaChanged` was `false`.
   - `refreshCards()` was **only** invoked if `pragmaChanged == true`.
   - The method did not update `card.setActive(...)` on existing `CompactVersionCard` components.
   - As a result, `cardsPanel` remained completely untouched when receiving `onCompilerStateChanged()`.
6. Furthermore, inside `updateActiveFileInfo()`, `activeVer` was resolved purely via `CompactToolchainUtil.getActiveCompilerVersion(project)`, whereas `refreshCards()` and `getActiveCompilerDisplay()` prioritized `MidnightProjectSettings.selectedCompilerVersion`.

## 5. Minimal Reproduction Test

An automated regression test was written in `CompactCompilerPanelTest.java`:
- Configured a `.compact` contract with `pragma language_version >= 0.18.0;`.
- Set initial `MidnightProjectSettings.selectedCompilerVersion = "0.25.0"`.
- Verified that initial card `0.25.0` was active and `0.26.0` was inactive.
- Invoked `CompactSwitchCompilerQuickFix.invoke(...)`.
- Dispatched all events on the IDE event queue.
- Prior to the fix, the test failed with:
  ```text
  junit.framework.AssertionFailedError at CompactCompilerPanelTest.java:75
  Card for switched compiler version 0.26.0 should be active in the tool window panel
  ```

## 6. Key Changes & Fix Implementation

1. **`CompactCompilerPanel.java`**:
   - Added `updateCardSelection()` to immediately synchronize visual active state across all displayed cards on EDT:
     ```java
     public void updateCardSelection() {
       MidnightProjectSettings settings = MidnightProjectSettings.getInstance(project);
       String selectedVer = (settings != null && settings.selectedCompilerVersion != null)
           ? settings.selectedCompilerVersion.trim()
           : "";
       String activeCompilerVer = CompactToolchainUtil.getActiveCompilerVersion(project);
       String effectiveActive = !selectedVer.isEmpty()
           ? selectedVer
           : (activeCompilerVer != null ? activeCompilerVer : "");

       for (Component c : cardsPanel.getComponents()) {
         if (c instanceof CompactVersionCard card) {
           card.setActive(card.getVersion().equals(effectiveActive));
         }
       }
     }
     ```
   - Updated the `CompactCompilerEventListener.TOPIC` message bus subscription to invoke `updateCardSelection()`, `updateActiveFileInfo(null)`, and `refreshCards()`:
     ```java
     project.getMessageBus().connect(this).subscribe(CompactCompilerEventListener.TOPIC, (CompactCompilerEventListener) () -> {
       updateCardSelection();
       updateActiveFileInfo(null);
       refreshCards();
     });
     ```
   - Updated `updateActiveFileInfo()` to invoke `updateCardSelection()` and consistently resolve `activeVer` prioritizing `settings.selectedCompilerVersion`.
   - Refactored `selectVersionImmediately(...)` to utilize `updateCardSelection()`.

2. **`CompactCompilerPanelTest.java`**:
   - Added `testQuickFixCompilerSwitchReflectedInPanel()` to automate and guarantee regression-free compiler version switching via quick-fix.

## 7. Verification & Regression Analysis

- **Reproduction Test**: `CompactCompilerPanelTest > testQuickFixCompilerSwitchReflectedInPanel` passes reliably.
- **MCP Inspections**: `get_file_problems` and `lint_files` reported 0 errors, 0 warnings, and 0 weak warnings.
- **Full Test Suite**: Ran `./gradlew cleanTest test`. 100% test suite pass rate across all project tests with zero regressions.

## 8. Best Practices & Prevention Rules

- **Universal Event Handling in UI Panels**: When subscribing to domain state change topics (such as `CompactCompilerEventListener.TOPIC`), tool windows must trigger full model/view reconciliation rather than assuming that file contents or pragma text changed.
- **EDT Immediacy + Background Consistency**: Always update visual selection flags immediately on EDT so users experience instant feedback, while deferring disk or network queries to background worker threads.
- **Consistent State Resolution**: Ensure that status headers, version cards, and inspection badges evaluate `selectedCompilerVersion` and `effectiveActive` through identical priority rules.

## Related Files

- [`CompactCompilerPanel.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/toolwindow/CompactCompilerPanel.java)
- [`CompactCompilerPanelTest.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/test/java/dev/verloren/midnight/toolwindow/CompactCompilerPanelTest.java)
- [`CompactCompilerEventListener.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/toolwindow/CompactCompilerEventListener.java)
- [`CompactSwitchCompilerQuickFix.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/annotator/CompactSwitchCompilerQuickFix.java)
- [`CompactVersionManager.java`](file:///C:/Users/shaki/IdeaProjects/midnight-plugin/src/main/java/dev/verloren/midnight/version/CompactVersionManager.java)
