package dev.verloren.midnight.editor;

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.psi.tree.IElementType;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Registers string literal delimiter quote handling for Compact source files.
 *
 * <h3>Subsystem Role &amp; Extension Point</h3>
 * <p>Registered in {@code plugin.xml} via the {@code com.intellij.lang.quoteHandler} extension point
 * for {@link dev.verloren.midnight.CompactLanguage}. Extends {@link SimpleTokenSetQuoteHandler} to govern
 * quote pairing, auto-insertion, and navigation for single and double-quoted strings:
 * <ul>
 *   <li><b>Auto-Pairing:</b> Automatically inserts a closing quote when typing an opening quote ({@code "} or {@code '}),
 *       placing the caret between them.</li>
 *   <li><b>Step-Over / Over-Typing:</b> Detects when typing a quote inside an unclosed string or at the closing boundary
 *       ({@link #isClosingQuote}), advancing the caret forward instead of inserting a duplicate quote.</li>
 *   <li><b>Unclosed Literal Tracking:</b> Scans ahead within the current line ({@link #hasNonClosedLiteral}) to avoid
 *       inserting duplicate closing quotes if an unclosed literal already exists downstream.</li>
 * </ul>
 * </p>
 *
 * <h3>Calling Pipeline &amp; Lifecycle</h3>
 * <p>Invoked by IntelliJ's editor typing actions ({@code TypedAction}) whenever the user types {@code "} or {@code '}
 * in a Compact editor buffer.</p>
 *
 * <h3>Threading Contract</h3>
 * <ul>
 *   <li><b>Execution Thread:</b> Evaluated on the Event Dispatch Thread (EDT) within the active editor typing command.</li>
 *   <li><b>Locking:</b> Operates directly on the document character sequence and lexer tokens via {@link HighlighterIterator}
 *       without requiring full AST reads or PSI locks.</li>
 * </ul>
 *
 * @see dev.verloren.midnight.editor.CompactDelimiterTypedHandler
 * @see com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
 */
public class CompactQuoteHandler extends SimpleTokenSetQuoteHandler {

  /**
   * Initializes the quote handler with Compact string literal and unterminated string token types.
   */
  public CompactQuoteHandler() {
    super(CompactTokenTypes.STRING_LITERAL, CompactTokenTypes.UNTERMINATED_STRING);
  }

  @Override
  public boolean isOpeningQuote(@NotNull HighlighterIterator iterator, int offset) {
    return iterator.getTokenType() == CompactTokenTypes.UNTERMINATED_STRING
        && offset == iterator.getStart();
  }

  @Override
  public boolean isClosingQuote(@NotNull HighlighterIterator iterator, int offset) {
    IElementType tokenType = iterator.getTokenType();
    if (tokenType == CompactTokenTypes.STRING_LITERAL) {
      int start = iterator.getStart();
      int end = iterator.getEnd();
      return end - start >= 2 && offset == end - 1;
    }
    return false;
  }

  @Override
  public boolean hasNonClosedLiteral(@NotNull Editor editor, @NotNull HighlighterIterator iterator, int offset) {
    int start = iterator.getStart();
    try {
      Document doc = editor.getDocument();
      CharSequence chars = doc.getCharsSequence();
      int lineEnd = doc.getLineEndOffset(doc.getLineNumber(offset));

      while (!iterator.atEnd() && iterator.getStart() < lineEnd) {
        IElementType tokenType = iterator.getTokenType();
        if (tokenType == CompactTokenTypes.UNTERMINATED_STRING) {
          return true;
        }
        if (tokenType == CompactTokenTypes.STRING_LITERAL && isNonClosedLiteral(iterator, chars)) {
          return true;
        }
        iterator.advance();
      }
    } finally {
      while (iterator.atEnd() || iterator.getStart() != start) {
        iterator.retreat();
      }
    }
    return false;
  }

  @Override
  protected boolean isNonClosedLiteral(HighlighterIterator iterator, CharSequence chars) {
    if (iterator.getTokenType() == CompactTokenTypes.UNTERMINATED_STRING) {
      return true;
    }
    if (iterator.getStart() >= iterator.getEnd() - 1) {
      return true;
    }
    char lastChar = chars.charAt(iterator.getEnd() - 1);
    return lastChar != '"' && lastChar != '\'';
  }

  @Override
  public boolean isInsideLiteral(@NotNull HighlighterIterator iterator) {
    IElementType tokenType = iterator.getTokenType();
    return tokenType == CompactTokenTypes.STRING_LITERAL || tokenType == CompactTokenTypes.UNTERMINATED_STRING;
  }
}
