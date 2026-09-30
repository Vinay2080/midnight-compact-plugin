package dev.verloren.midnight.highlighter;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.lexer.CompactTokenTypes;

/**
 * Unit tests for CompactSyntaxHighlighter token attribute mapping.
 */
public class CompactSyntaxHighlighterTest extends BasePlatformTestCase {

  public void testSyntaxHighlighterLexicalTokens() {
    CompactSyntaxHighlighter highlighter = new CompactSyntaxHighlighter();

    // Operators
    assertEquals(CompactHighlighterColors.OPERATOR, highlighter.getTokenHighlights(CompactTokenTypes.ASSIGN)[0]);
    assertEquals(CompactHighlighterColors.OPERATOR, highlighter.getTokenHighlights(CompactTokenTypes.EQEQ)[0]);
    assertEquals(CompactHighlighterColors.OPERATOR, highlighter.getTokenHighlights(CompactTokenTypes.NOT)[0]);
    assertEquals(CompactHighlighterColors.OPERATOR, highlighter.getTokenHighlights(CompactTokenTypes.GTE)[0]);
    assertEquals(CompactHighlighterColors.OPERATOR, highlighter.getTokenHighlights(CompactTokenTypes.LT)[0]);
    assertEquals(CompactHighlighterColors.OPERATOR, highlighter.getTokenHighlights(CompactTokenTypes.GT)[0]);

    // Numerics
    assertEquals(CompactHighlighterColors.NUMBER, highlighter.getTokenHighlights(CompactTokenTypes.DECIMAL_LITERAL)[0]);
    assertEquals(CompactHighlighterColors.NUMBER, highlighter.getTokenHighlights(CompactTokenTypes.HEX_LITERAL)[0]);
    assertEquals(CompactHighlighterColors.NUMBER, highlighter.getTokenHighlights(CompactTokenTypes.BINARY_LITERAL)[0]);
    assertEquals(CompactHighlighterColors.NUMBER, highlighter.getTokenHighlights(CompactTokenTypes.OCTAL_LITERAL)[0]);

    // Modifiers
    assertEquals(CompactHighlighterColors.MODIFIER, highlighter.getTokenHighlights(CompactTokenTypes.EXPORT)[0]);
    assertEquals(CompactHighlighterColors.MODIFIER, highlighter.getTokenHighlights(CompactTokenTypes.SEALED)[0]);
    assertEquals(CompactHighlighterColors.MODIFIER, highlighter.getTokenHighlights(CompactTokenTypes.PURE)[0]);

    // Keywords & Pragma
    assertEquals(CompactHighlighterColors.KEYWORD, highlighter.getTokenHighlights(CompactTokenTypes.CIRCUIT)[0]);
    assertEquals(CompactHighlighterColors.KEYWORD, highlighter.getTokenHighlights(CompactTokenTypes.LEDGER)[0]);
    assertEquals(CompactHighlighterColors.KEYWORD, highlighter.getTokenHighlights(CompactTokenTypes.LET)[0]);
    assertEquals(CompactHighlighterColors.PRAGMA, highlighter.getTokenHighlights(CompactTokenTypes.PRAGMA)[0]);
  }
}
