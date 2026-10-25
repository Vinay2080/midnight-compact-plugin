package dev.verloren.midnight.editor;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Statement surrounder that wraps selected Compact statements within a conditional branch
 * ({@code if (expr) { ... }}) and automatically selects the condition expression for immediate editing.
 *
 * <h3>Architectural Role</h3>
 * <p>Registered in {@link CompactSurroundDescriptor} as one of the standard surround actions. When invoked,
 * it wraps selected statements inside an {@code if (true) { ... }} control structure and pre-selects
 * the dummy {@code true} condition so the developer can immediately type the actual boolean expression.</p>
 *
 * <h3>Transformation Example</h3>
 * <pre>
 *   Before:
 *     doSomething();
 *
 *   After:
 *     if ([true]) {
 *       doSomething();
 *     }
 * </pre>
 * <p>where {@code [true]} indicates the active editor selection.</p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>EDT:</b> Invoked on EDT within a WriteCommandAction via {@link CompactSurrounderBase}.</li>
 * </ul>
 *
 * @see CompactSurroundDescriptor
 * @see CompactSurrounderBase
 * @see CompactBlockSurrounder
 */
public class CompactIfSurrounder extends CompactSurrounderBase {

  /**
   * Returns the user-facing label displayed in the "Surround With..." popup menu.
   *
   * @return {@code "if (expr) { ... }"}
   */
  @Override
  public @NlsSafe String getTemplateDescription() {
    return "if (expr) { ... }";
  }

  /**
   * Generates an {@code if (true) { ... }} control structure enclosing the selected statements.
   *
   * @param selectedText the statements to enclose
   * @return the transformed conditional block
   */
  @Override
  protected @NotNull String generateReplacement(@NotNull String selectedText) {
    return "if (true) {\n" + selectedText + "\n}";
  }

  /**
   * Locates the inserted {@code (true)} condition within the document, sets the editor selection
   * across {@code "true"}, and positions the caret for instant replacement.
   *
   * @param editor the active editor instance
   * @param document the modified editor document
   * @param startOffset starting offset of the replacement
   * @param replacement the replacement string inserted
   * @return the text range of the placeholder condition {@code "true"}
   */
  @Override
  protected @Nullable TextRange postProcess(@NotNull Editor editor,
                                            @NotNull Document document,
                                            int startOffset,
                                            @NotNull String replacement) {
    // 1. Search for 'if (true)' near the replacement start offset
    String text = document.getText();
    int ifIndex = text.indexOf("if (true)", Math.max(0, startOffset - 20));
    if (ifIndex == -1) {
      ifIndex = text.indexOf("if (true)");
    }

    // 2. If located, select the 'true' literal token (offset 4 chars into 'if (true)')
    if (ifIndex != -1) {
      int conditionStart = ifIndex + 4;
      int conditionEnd = conditionStart + 4;
      editor.getCaretModel().moveToOffset(conditionStart);
      editor.getSelectionModel().setSelection(conditionStart, conditionEnd);
      return new TextRange(conditionStart, conditionEnd);
    }

    // Fallback: return the entire replacement range
    return new TextRange(startOffset, startOffset + replacement.length());
  }
}
