package dev.verloren.midnight.completion;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Classifier helper detecting export statements and modifiers preceding the completion point.
 */
public final class CompactCompletionExportClassifier {

  private CompactCompletionExportClassifier() {}

  /**
   * Determines whether the cursor is preceded by an export modifier on the current line or statement.
   *
   * @param position the current caret element
   * @return true if export precedes the cursor, false otherwise
   */
  public static boolean isExportPreceding(@NotNull PsiElement position) {
    if (CompactCompletionContext.isComment(position)) {
      return false;
    }
    for (PsiElement p = CompactCompletionContext.prevNonCommentLeaf(position); p != null; p = CompactCompletionContext.prevNonCommentLeaf(p)) {
      if (p.getNode() == null) break;
      IElementType tt = p.getNode().getElementType();
      if (tt == CompactTokenTypes.EXPORT) {
        return true;
      }
      if (tt == CompactTokenTypes.SEMICOLON || tt == CompactTokenTypes.LBRACE || tt == CompactTokenTypes.RBRACE
          || tt == CompactTokenTypes.COLON || tt == CompactTokenTypes.ASSIGN
          || tt == CompactTokenTypes.LPAREN || tt == CompactTokenTypes.RPAREN
          || tt == CompactTokenTypes.COMMA
          || tt == CompactTokenTypes.LEDGER || tt == CompactTokenTypes.CIRCUIT
          || tt == CompactTokenTypes.WITNESS || tt == CompactTokenTypes.STRUCT
          || tt == CompactTokenTypes.ENUM || tt == CompactTokenTypes.TYPE
          || tt == CompactTokenTypes.MODULE || tt == CompactTokenTypes.CONTRACT) {
        break;
      }
    }

    try {
      com.intellij.openapi.editor.Document doc = position.getContainingFile().getViewProvider().getDocument();
      if (doc != null) {
        int offset = position.getTextRange().getStartOffset();
        int lineStart = doc.getLineStartOffset(doc.getLineNumber(offset));
        CharSequence chars = doc.getCharsSequence();
        return hasPrecedingExportOnLine(chars, lineStart, offset);
      }
    } catch (Exception _) {
    }

    return false;
  }

  /**
   * Fallback line scanner testing whether the text preceding offset on the current line
   * consists of an export statement prefix.
   *
   * @param chars     document character sequence
   * @param lineStart start offset of the current line
   * @param offset    caret offset within the document
   * @return true if the line prefix matches export patterns
   */
  public static boolean hasPrecedingExportOnLine(@NotNull CharSequence chars, int lineStart, int offset) {
    int stmtStart = lineStart;
    for (int i = offset - 1; i >= lineStart; i--) {
      char c = chars.charAt(i);
      if (c == ';' || c == '{' || c == '}') {
        stmtStart = i + 1;
        break;
      }
    }
    String stmtBefore = chars.subSequence(stmtStart, offset).toString().trim();
    return stmtBefore.matches("^export(\\s+(sealed|pure|new))?(\\s+\\w*)?$");
  }
}
