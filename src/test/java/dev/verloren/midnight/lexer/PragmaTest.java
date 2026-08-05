package dev.verloren.midnight.lexer;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class PragmaTest {
  @Test
  public void lexesValidVersionLiterals() {
    assertSingleToken("1", CompactTokenTypes.VERSION_LITERAL);
    assertSingleToken("1.0", CompactTokenTypes.VERSION_LITERAL);
    assertSingleToken("1.2.3", CompactTokenTypes.VERSION_LITERAL);
  }

  @Test
  public void lexesMalformedVersionAsSingleToken() {
    assertSingleToken("12.", CompactTokenTypes.INVALID_VERSION);
    assertSingleToken("12..", CompactTokenTypes.INVALID_VERSION);
    assertSingleToken("12.a", CompactTokenTypes.INVALID_VERSION);
  }

  @Test
  public void reportsStableTokenOffsets() {
    CompactLexer lexer = new CompactLexer();
    lexer.start("pragma version 1;", 0, "pragma version 1;".length(), 0);

    assertToken(lexer, CompactTokenTypes.PRAGMA, 0, 6);
    lexer.advance();
    assertToken(lexer, TokenType.WHITE_SPACE, 6, 7);
    lexer.advance();
    assertToken(lexer, CompactTokenTypes.IDENTIFIER, 7, 14);
    lexer.advance();
    assertToken(lexer, TokenType.WHITE_SPACE, 14, 15);
    lexer.advance();
    assertToken(lexer, CompactTokenTypes.VERSION_LITERAL, 15, 16);
    lexer.advance();
    assertToken(lexer, CompactTokenTypes.SEMICOLON, 16, 17);
    lexer.advance();
    assertNull(lexer.getTokenType());
    assertEquals(17, lexer.getTokenStart());
    assertEquals(17, lexer.getTokenEnd());
  }

  private static void assertSingleToken(String text, IElementType expectedTokenType) {
    CompactLexer lexer = new CompactLexer();
    lexer.start(text, 0, text.length(), 0);

    assertToken(lexer, expectedTokenType, 0, text.length());
    lexer.advance();
    assertNull(lexer.getTokenType());
    assertEquals(text.length(), lexer.getTokenStart());
    assertEquals(text.length(), lexer.getTokenEnd());
  }

  private static void assertToken(CompactLexer lexer, IElementType expectedTokenType, int start, int end) {
    assertEquals(expectedTokenType, lexer.getTokenType());
    assertEquals(start, lexer.getTokenStart());
    assertEquals(end, lexer.getTokenEnd());
  }
}
