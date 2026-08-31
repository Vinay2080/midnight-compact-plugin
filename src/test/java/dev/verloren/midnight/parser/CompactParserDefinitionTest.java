package dev.verloren.midnight.parser;

import com.intellij.psi.tree.TokenSet;
import dev.verloren.midnight.lexer.CompactTokenTypes;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CompactParserDefinitionTest {

  @Test
  public void testStringLiteralElementsContainsStringLiteral() {
    CompactParserDefinition definition = new CompactParserDefinition();
    TokenSet stringLiterals = definition.getStringLiteralElements();
    assertNotNull("String literal elements should not be null", stringLiterals);
    assertTrue("String literal elements should contain STRING_LITERAL token type",
        stringLiterals.contains(CompactTokenTypes.STRING_LITERAL));
  }

  @Test
  public void testCommentTokensContainsComments() {
    CompactParserDefinition definition = new CompactParserDefinition();
    TokenSet comments = definition.getCommentTokens();
    assertNotNull("Comment tokens should not be null", comments);
    assertTrue(comments.contains(CompactTokenTypes.LINE_COMMENT));
    assertTrue(comments.contains(CompactTokenTypes.BLOCK_COMMENT));
  }
}
