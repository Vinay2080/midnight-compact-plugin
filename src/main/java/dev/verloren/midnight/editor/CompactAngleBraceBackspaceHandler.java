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
 * Handles backspacing angle brackets in Compact code.
 *
 * <p>When the caret is positioned between matching angle brackets ({@code <|>}) and the user
 * presses Backspace, this handler removes the closing {@code >} alongside the deleted {@code <}.</p>
 */
public class CompactAngleBraceBackspaceHandler extends BackspaceHandlerDelegate {

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
