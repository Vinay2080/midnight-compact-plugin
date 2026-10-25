package dev.verloren.midnight.editor;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import dev.verloren.midnight.psi.CompactFile;
import org.jetbrains.annotations.NotNull;

/**
 * Backspace handler for paired angle brackets in Compact source files.
 *
 * <h3>Subsystem Role &amp; Extension Point</h3>
 * <p>Registered in {@code plugin.xml} via the {@code com.intellij.backspaceHandlerDelegate} extension point.
 * Complements {@link CompactAngleBraceTypedHandler} by managing paired deletion of angle brackets:
 * <ul>
 *   <li><b>Symmetric Deletion:</b> When the caret is positioned immediately between matching angle brackets
 *       ({@code <|>}) and the user presses Backspace, this handler deletes the trailing {@code >} alongside the
 *       preceding {@code <}, leaving the editor buffer clean.</li>
 *   <li><b>Balance Verification:</b> Checks {@link CompactAngleBraceTypedHandler#calculateBalance} in
 *       {@link #charDeleted} to guarantee that the trailing {@code >} is only deleted if it represents an
 *       unbalanced paired bracket (preventing deletion of legitimate closing brackets in expressions).</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline &amp; Lifecycle</h3>
 * <p>Invoked by IntelliJ's backspace action pipeline:
 * <ol>
 *   <li>{@link #beforeCharDeleted}: Called before the character before the caret is deleted. Checks if
 *       {@code c == '<'} and whether the next token is {@link CompactTokenTypes#GT}. If so, sets
 *       {@link #BACKSPACE_ENABLED_KEY} in editor user data.</li>
 *   <li>The platform deletes the {@code <} character.</li>
 *   <li>{@link #charDeleted}: Consumes {@link #BACKSPACE_ENABLED_KEY}, verifies that bracket balance is
 *       negative (orphaned closing bracket), and deletes the trailing {@code >}.</li>
 * </ol>
 * </p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Always called on the Event Dispatch Thread (EDT) within an active
 *       editor write action command.</li>
 *   <li><b>Read Action:</b> Uses {@link HighlighterIterator} to inspect token types without acquiring
 *       explicit PSI read locks.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.editor.CompactAngleBraceTypedHandler
 * @see com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
 */
public class CompactAngleBraceBackspaceHandler extends BackspaceHandlerDelegate {

  /**
   * Transient editor user data key tracking whether backspacing a paired opening angle bracket was primed in beforeCharDeleted.
   */
  private static final Key<Boolean> BACKSPACE_ENABLED_KEY =
      Key.create("dev.verloren.midnight.editor.COMPACT_ANGLE_BRACE_BACKSPACE_ENABLED");

  @Override
  public void beforeCharDeleted(char c, @NotNull PsiFile file, @NotNull Editor editor) {
    if (!(file instanceof CompactFile)) {
      return;
    }

    if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) {
      return;
    }

    if (c == '<' && editor instanceof EditorEx editorEx) {
      int offset = editor.getCaretModel().getOffset();
      if (offset < editor.getDocument().getTextLength()) {
        HighlighterIterator iterator = editorEx.getHighlighter().createIterator(offset);
        if (!iterator.atEnd() && iterator.getTokenType() == CompactTokenTypes.GT) {
          editor.putUserData(BACKSPACE_ENABLED_KEY, Boolean.TRUE);
          return;
        }
      }
    }

    editor.putUserData(BACKSPACE_ENABLED_KEY, null);
  }

  @Override
  public boolean charDeleted(char c, @NotNull PsiFile file, @NotNull Editor editor) {
    if (!(file instanceof CompactFile)) {
      return false;
    }

    Boolean enabled = editor.getUserData(BACKSPACE_ENABLED_KEY);
    editor.putUserData(BACKSPACE_ENABLED_KEY, null);

    if (Boolean.TRUE.equals(enabled)) {
      int balance = CompactAngleBraceTypedHandler.calculateBalance(editor);
      if (balance < 0) {
        int offset = editor.getCaretModel().getOffset();
        if (offset < editor.getDocument().getTextLength()) {
          editor.getDocument().deleteString(offset, offset + 1);
          return true;
        }
      }
    }

    return false;
  }
}
