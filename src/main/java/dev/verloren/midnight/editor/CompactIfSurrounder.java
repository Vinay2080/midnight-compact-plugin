package dev.verloren.midnight.editor;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Surrounds selected Compact statements with an {@code if (true) { ... }} block.
 */
public class CompactIfSurrounder extends CompactSurrounderBase {

  @Override
  public @NlsSafe String getTemplateDescription() {
    return "if (expr) { ... }";
  }

  @Override
  protected @NotNull String generateReplacement(@NotNull String selectedText) {
    return "if (true) {\n" + selectedText + "\n}";
  }

  @Override
  protected @Nullable TextRange postProcess(@NotNull Editor editor,
                                            @NotNull Document document,
                                            int startOffset,
                                            @NotNull String replacement) {
    String text = document.getText();
    int ifIndex = text.indexOf("if (true)", Math.max(0, startOffset - 20));
    if (ifIndex == -1) {
      ifIndex = text.indexOf("if (true)");
    }

    if (ifIndex != -1) {
      int conditionStart = ifIndex + 4;
      int conditionEnd = conditionStart + 4;
      editor.getCaretModel().moveToOffset(conditionStart);
      editor.getSelectionModel().setSelection(conditionStart, conditionEnd);
      return new TextRange(conditionStart, conditionEnd);
    }

    return new TextRange(startOffset, startOffset + replacement.length());
  }
}
