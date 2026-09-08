package dev.verloren.midnight.toolwindow;

import com.intellij.util.messages.Topic;

/**
 * Project message bus topic notified when the Compact compiler version,
 * installation state, or pragma constraints change.
 */
@FunctionalInterface
public interface CompactCompilerEventListener {
  Topic<CompactCompilerEventListener> TOPIC =
      Topic.create("Compact Compiler Event", CompactCompilerEventListener.class, Topic.BroadcastDirection.NONE);

  void onCompilerStateChanged();
}
