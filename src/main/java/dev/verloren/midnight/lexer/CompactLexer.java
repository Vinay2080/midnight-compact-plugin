package dev.verloren.midnight.lexer;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class CompactLexer extends LexerBase {

  private CharSequence buffer = "";
  private int endOffset;
  private int position;
  private int tokenStart;
  private int tokenEnd;
  private IElementType tokenType;

  @Override
  public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
    this.buffer = buffer;
    this.endOffset = endOffset;
    this.position = startOffset;
    this.tokenStart = startOffset;
    this.tokenEnd = startOffset;
    this.tokenType = null;

    advance();
  }

  @Override
  public int getState() {
    return 0;
  }

  @Override
  public @Nullable IElementType getTokenType() {
    return tokenType;
  }

  @Override
  public int getTokenStart() {
    return tokenStart;
  }

  @Override
  public int getTokenEnd() {
    return tokenEnd;
  }

  @Override
  public @NotNull CharSequence getBufferSequence() {
    return buffer;
  }

  @Override
  public int getBufferEnd() {
    return endOffset;
  }

  @Override
  public void advance() {
    if (position >= endOffset) {
      tokenStart = endOffset;
      tokenEnd = endOffset;
      tokenType = null;
      return;
    }

    tokenStart = position;
    char ch = buffer.charAt(position);

    if (Character.isWhitespace(ch)) {
      lexWhitespace();
      return;
    }

    if (isIdentifierStart(ch)) {
      lexIdentifierOrKeyword();
      return;
    }

    if (Character.isDigit(ch)) {
      lexVersion();
      return;
    }

    if (ch == ';') {
      position++;
      finish(CompactTokenTypes.SEMICOLON);
      return;
    }

    position++;
    finish(TokenType.BAD_CHARACTER);
  }

  private void lexWhitespace() {
    while (position < endOffset && Character.isWhitespace(buffer.charAt(position))) {
      position++;
    }
    finish(TokenType.WHITE_SPACE);
  }

  private void lexIdentifierOrKeyword() {

    do {
      position++;
    } while (position < endOffset && isIdentifierPart(buffer.charAt(position)));

    CharSequence text = buffer.subSequence(tokenStart, position);
    finish("pragma".contentEquals(text) ? CompactTokenTypes.PRAGMA : CompactTokenTypes.IDENTIFIER);
  }

  private void lexVersion() {
    consumeDigits();

    while (position < endOffset && buffer.charAt(position) == '.') {
      position++;

      if (position >= endOffset || !Character.isDigit(buffer.charAt(position))) {
        consumeMalformedVersionTail();
        finish(CompactTokenTypes.INVALID_VERSION);
        return;
      }

      consumeDigits();
    }

    if (position < endOffset && isIdentifierStart(buffer.charAt(position))) {
      consumeMalformedVersionTail();
      finish(CompactTokenTypes.INVALID_VERSION);
      return;
    }

    finish(CompactTokenTypes.VERSION);
  }

  private void consumeDigits() {
    while (position < endOffset && Character.isDigit(buffer.charAt(position))) {
      position++;
    }
  }

  private void consumeMalformedVersionTail() {
    while (position < endOffset) {
      char ch = buffer.charAt(position);
      if (!isIdentifierPart(ch) && ch != '.') {
        return;
      }
      position++;
    }
  }

  private void finish(IElementType tokenType) {
    tokenEnd = position;
    this.tokenType = tokenType;
  }

  private static boolean isIdentifierStart(char ch) {
    return Character.isLetter(ch) || ch == '_';
  }

  private static boolean isIdentifierPart(char ch) {
    return Character.isLetterOrDigit(ch) || ch == '_';
  }

}
