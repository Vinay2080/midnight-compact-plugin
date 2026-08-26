package dev.verloren.midnight.editor;

import com.intellij.lang.BracePair;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.lexer.CompactTokenTypes;

public class CompactEditorFeaturesTest extends BasePlatformTestCase {

  public void testCommenterPrefixes() {
    CompactCommenter commenter = new CompactCommenter();
    assertEquals("//", commenter.getLineCommentPrefix());
    assertEquals("/*", commenter.getBlockCommentPrefix());
    assertEquals("*/", commenter.getBlockCommentSuffix());
    assertNull(commenter.getCommentedBlockCommentPrefix());
    assertNull(commenter.getCommentedBlockCommentSuffix());
  }

  public void testPairedBraceMatcherPairs() {
    CompactPairedBraceMatcher matcher = new CompactPairedBraceMatcher();
    BracePair[] pairs = matcher.getPairs();
    assertNotNull(pairs);
    assertEquals(4, pairs.length);

    assertEquals(CompactTokenTypes.LBRACE, pairs[0].getLeftBraceType());
    assertEquals(CompactTokenTypes.RBRACE, pairs[0].getRightBraceType());
    assertTrue(pairs[0].isStructural());

    assertEquals(CompactTokenTypes.LPAREN, pairs[1].getLeftBraceType());
    assertEquals(CompactTokenTypes.RPAREN, pairs[1].getRightBraceType());
    assertFalse(pairs[1].isStructural());

    assertEquals(CompactTokenTypes.LBRACKET, pairs[2].getLeftBraceType());
    assertEquals(CompactTokenTypes.RBRACKET, pairs[2].getRightBraceType());
    assertFalse(pairs[2].isStructural());

    assertEquals(CompactTokenTypes.LT, pairs[3].getLeftBraceType());
    assertEquals(CompactTokenTypes.GT, pairs[3].getRightBraceType());
    assertFalse(pairs[3].isStructural());

    assertTrue(matcher.isPairedBracesAllowedBeforeType(CompactTokenTypes.LBRACE, CompactTokenTypes.IDENTIFIER));
    assertEquals(10, matcher.getCodeConstructStart(null, 10));
  }

  public void testQuoteHandler() {
    CompactQuoteHandler quoteHandler = new CompactQuoteHandler();
    assertNotNull(quoteHandler);
  }

  public void testSpellcheckingStrategy() {
    CompactSpellcheckingStrategy strategy = new CompactSpellcheckingStrategy();
    assertNotNull(strategy);
  }
}
