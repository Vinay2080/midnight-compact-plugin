package dev.verloren.midnight.lexer;

import com.intellij.psi.tree.IElementType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LexerTest {

  @Test
  public void lexesNotEqualOperator() {
    assertTokens("!=", CompactTokenTypes.NEQ);
    assertTokens("a != b",
            CompactTokenTypes.IDENTIFIER,
            CompactTokenTypes.WHITE_SPACE,
            CompactTokenTypes.NEQ,
            CompactTokenTypes.WHITE_SPACE,
            CompactTokenTypes.IDENTIFIER);
  }

  private static void assertTokens(String text, IElementType... expectedTypes) {
    CompactLexer lexer = new CompactLexer();
    lexer.start(text, 0, text.length(), 0);

    for (IElementType expectedType : expectedTypes) {
      assertEquals("Mismatch in token stream for input: " + text, expectedType, lexer.getTokenType());
      lexer.advance();
    }
    assertNull(lexer.getTokenType());
  }

  @Test
  public void lexesRangeExpressions() {
    assertTokens("1..10",
            CompactTokenTypes.DECIMAL_LITERAL,
            CompactTokenTypes.RANGE,
            CompactTokenTypes.DECIMAL_LITERAL);
  }

  @Test
  public void lexesHashPunctuation() {
    assertTokens("#N", CompactTokenTypes.HASH, CompactTokenTypes.IDENTIFIER);
  }

  @Test
  public void lexesImplementsAndExternalKeywords() {
    assertTokens("implements", CompactTokenTypes.IMPLEMENTS);
    assertTokens("external", CompactTokenTypes.EXTERNAL);
  }

  @Test
  public void lexesSingleAndDoubleQuotedStrings() {
    assertTokens("'hello'", CompactTokenTypes.STRING_LITERAL);
    assertTokens("\"world\"", CompactTokenTypes.STRING_LITERAL);
  }

  @Test
  public void lexesNumericPrefixes() {
    assertTokens("0o755", CompactTokenTypes.OCTAL_LITERAL);
    assertTokens("0x1AF", CompactTokenTypes.HEX_LITERAL);
    assertTokens("0b1010", CompactTokenTypes.BINARY_LITERAL);
  }

  @Test
  public void lexesIdentifiersWithDollar() {
    assertTokens("$foo", CompactTokenTypes.IDENTIFIER);
  }

  @Test
  public void lexesSlashOperator() {
    assertTokens("/", CompactTokenTypes.SLASH);
  }
}
