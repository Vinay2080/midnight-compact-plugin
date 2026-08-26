package dev.verloren.midnight.editor;

import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;

/**
 * Surrounds selected Compact statements with a {@code { ... }} block.
 */
public class CompactBlockSurrounder extends CompactSurrounderBase {

  @Override
  public @NlsSafe String getTemplateDescription() {
    return "{ ... }";
  }

  @Override
  protected @NotNull String generateReplacement(@NotNull String selectedText) {
    return "{\n" + selectedText + "\n}";
  }
}
