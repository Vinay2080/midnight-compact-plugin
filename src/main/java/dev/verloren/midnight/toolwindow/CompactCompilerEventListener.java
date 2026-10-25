package dev.verloren.midnight.toolwindow;

import com.intellij.util.messages.Topic;

/**
 * Project message bus topic notified when the active Compact compiler version,
 * installation state, or contract pragma compatibility status changes.
 *
 * <h3>Architectural Role &amp; Extension Point</h3>
 * <p>Serves as the central publish-subscribe communication bus within the Midnight plugin for
 * synchronizing UI elements when the compiler state transitions. Subscribers include:</p>
 * <ul>
 *   <li>{@link CompactCompilerPanel}: Updates the Remix-style tool window version cards and pragma compatibility status.</li>
 *   <li>{@link dev.verloren.midnight.statusbar.CompactStatusBarWidget}: Refreshes the active compiler version label in the bottom IDE status bar.</li>
 *   <li>Code annotators &amp; problem handlers: Re-trigger file analysis when toolchain capabilities or target language versions shift.</li>
 * </ul>
 *
 * <h3>Event Propagation Pipeline</h3>
 * <pre>
 *   [Compiler Switch / Download / Removal Action]
 *                         │
 *                         ▼
 *   [project.getMessageBus().syncPublisher(CompactCompilerEventListener.TOPIC)]
 *                         │
 *                         ├─► onCompilerStateChanged()
 *                         │
 *            ┌────────────┴────────────┐
 *            │                         │
 *            ▼                         ▼
 *   [CompactCompilerPanel]   [CompactStatusBarWidget]
 *   - Refresh version cards  - Refresh widget text &amp; icon
 *   - Update pragma badges   - Re-render status bar
 * </pre>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Broadcast Direction:</b> Broadcasted across the project level ({@link Topic.BroadcastDirection#NONE}).</li>
 *   <li><b>Thread Dispatch:</b> Handlers must account for invocation on either the EDT or background worker threads,
 *       dispatching UI updates to the EDT via {@code invokeLater}.</li>
 * </ul>
 *
 * @see CompactCompilerPanel
 * @see dev.verloren.midnight.statusbar.CompactStatusBarWidget
 */
@FunctionalInterface
public interface CompactCompilerEventListener {

  /**
   * Project-level topic for compiler state and version changes.
   */
  Topic<CompactCompilerEventListener> TOPIC =
      Topic.create("Compact Compiler Event", CompactCompilerEventListener.class, Topic.BroadcastDirection.NONE);

  /**
   * Callback invoked whenever a compiler version is selected, installed, uninstalled,
   * or when the active pragma constraint is updated.
   */
  void onCompilerStateChanged();
}
