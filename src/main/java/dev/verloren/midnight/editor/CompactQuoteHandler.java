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
 * <p>Enables automatic insertion and pairing of quotation marks ({@code "} and {@code '}),
 * caret placement inside the paired quotes, and smart navigation / step-over during editing.</p>
 */
public class CompactQuoteHandler extends SimpleTokenSetQuoteHandler {

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
