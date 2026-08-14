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

  @Test
  public void lexesStringEscapesAndUnclosedString() {
    assertTokens("\"hello \\\"world\\\"\"", CompactTokenTypes.STRING_LITERAL);
    assertTokens("\"line1\\nline2\"", CompactTokenTypes.STRING_LITERAL);
    assertTokens("\"unclosed", CompactTokenTypes.UNTERMINATED_STRING);
  }

  @Test
  public void lexesNumericBoundaries() {
    assertTokens("0x0", CompactTokenTypes.HEX_LITERAL);
    assertTokens("0b0", CompactTokenTypes.BINARY_LITERAL);
    assertTokens("0o0", CompactTokenTypes.OCTAL_LITERAL);
    assertTokens("12345678901234567890", CompactTokenTypes.DECIMAL_LITERAL);
  }

  @Test
  public void lexesOperatorDisambiguation() {
    assertTokens(". .. ...",
            CompactTokenTypes.DOT, CompactTokenTypes.WHITE_SPACE,
            CompactTokenTypes.RANGE, CompactTokenTypes.WHITE_SPACE,
            CompactTokenTypes.SPREAD);
    assertTokens("= == =>",
            CompactTokenTypes.ASSIGN, CompactTokenTypes.WHITE_SPACE,
            CompactTokenTypes.EQEQ, CompactTokenTypes.WHITE_SPACE,
            CompactTokenTypes.ARROW);
    assertTokens("< <= > >=",
            CompactTokenTypes.LT, CompactTokenTypes.WHITE_SPACE,
            CompactTokenTypes.LTE, CompactTokenTypes.WHITE_SPACE,
            CompactTokenTypes.GT, CompactTokenTypes.WHITE_SPACE,
            CompactTokenTypes.GTE);
  }

  @Test
  public void lexesKeywordsAndIdentifiers() {
    assertTokens("let const circuit struct enum contract witness",
            CompactTokenTypes.LET, CompactTokenTypes.WHITE_SPACE,
            CompactTokenTypes.CONST, CompactTokenTypes.WHITE_SPACE,
            CompactTokenTypes.CIRCUIT, CompactTokenTypes.WHITE_SPACE,
            CompactTokenTypes.STRUCT, CompactTokenTypes.WHITE_SPACE,
            CompactTokenTypes.ENUM, CompactTokenTypes.WHITE_SPACE,
            CompactTokenTypes.CONTRACT, CompactTokenTypes.WHITE_SPACE,
            CompactTokenTypes.WITNESS);
  }
}
