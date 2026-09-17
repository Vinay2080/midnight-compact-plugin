package dev.verloren.midnight.editor;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.lexer.CompactTokenTypes;

/**
 * Multi-tier unit and integration tests verifying automatic quotation mark pairing ({@code "} and {@code '}),
 * cursor positioning between paired quotes, closing quote step-over, and boundary resilience in Compact code.
 */
public class CompactQuoteTypingTest extends BasePlatformTestCase {

  // =========================================================================
  // Tier 1: Basic Quote Pairing Tests
  // =========================================================================

  public void testPairDoubleQuoteInVariableBinding() {
    myFixture.configureByText("test.compact", "let s = <caret>;");
    myFixture.type('"');
    myFixture.checkResult("let s = \"<caret>\";");
  }

  public void testPairDoubleQuoteAtEndOfFile() {
    myFixture.configureByText("test.compact", "let s = <caret>");
    myFixture.type('"');
    myFixture.checkResult("let s = \"<caret>\"");
  }

  public void testPairDoubleQuoteInEmptyLine() {
    myFixture.configureByText("test.compact", "<caret>");
    myFixture.type('"');
    myFixture.checkResult("\"<caret>\"");
  }

  public void testPairDoubleQuoteInAssertStatement() {
    myFixture.configureByText("test.compact", "assert(x > 0, <caret>);");
    myFixture.type('"');
    myFixture.checkResult("assert(x > 0, \"<caret>\");");
  }

  public void testPairDoubleQuoteInTypeArgument() {
    myFixture.configureByText("test.compact", "type StringTag = Opaque<<caret>>;");
    myFixture.type('"');
    myFixture.checkResult("type StringTag = Opaque<\"<caret>\">;");
  }

  public void testPairDoubleQuoteInIncludeStatement() {
    myFixture.configureByText("test.compact", "include <caret>;");
    myFixture.type('"');
    myFixture.checkResult("include \"<caret>\";");
  }

  public void testPairDoubleQuoteInImportStatement() {
    myFixture.configureByText("test.compact", "import { helper } from <caret>;");
    myFixture.type('"');
    myFixture.checkResult("import { helper } from \"<caret>\";");
  }

  public void testPairSingleQuoteInVariableBinding() {
    myFixture.configureByText("test.compact", "let s = <caret>;");
    myFixture.type('\'');
    myFixture.checkResult("let s = '<caret>';");
  }

  public void testPairSingleQuoteInEmptyLine() {
    myFixture.configureByText("test.compact", "<caret>");
    myFixture.type('\'');
    myFixture.checkResult("'<caret>'");
  }

  // =========================================================================
  // Tier 2: Step-Over / Overtyping Closing Quotes
  // =========================================================================

  public void testStepOverClosingDoubleQuoteInEmptyString() {
    myFixture.configureByText("test.compact", "let s = \"<caret>\";");
    myFixture.type('"');
    myFixture.checkResult("let s = \"\"<caret>;");
  }

  public void testStepOverClosingDoubleQuoteWithContent() {
    myFixture.configureByText("test.compact", "let s = \"hello<caret>\";");
    myFixture.type('"');
    myFixture.checkResult("let s = \"hello\"<caret>;");
  }

  public void testStepOverClosingSingleQuoteInEmptyString() {
    myFixture.configureByText("test.compact", "let s = '<caret>';");
    myFixture.type('\'');
    myFixture.checkResult("let s = ''<caret>;");
  }

  public void testStepOverClosingSingleQuoteWithContent() {
    myFixture.configureByText("test.compact", "let s = 'hello<caret>';");
    myFixture.type('\'');
    myFixture.checkResult("let s = 'hello'<caret>;");
  }

  public void testTypeAndStepOverSequentially() {
    myFixture.configureByText("test.compact", "let s = <caret>;");
    myFixture.type('"');
    myFixture.checkResult("let s = \"<caret>\";");
    myFixture.type("msg");
    myFixture.checkResult("let s = \"msg<caret>\";");
    myFixture.type('"');
    myFixture.checkResult("let s = \"msg\"<caret>;");
  }

  // =========================================================================
  // Tier 3: Contextual Boundary & Suppression Tests
  // =========================================================================

  public void testDoNotPairInsideLineComment() {
    myFixture.configureByText("test.compact", "// this is a comment <caret>");
    myFixture.type('"');
    myFixture.checkResult("// this is a comment \"<caret>");
  }

  public void testDoNotPairInsideBlockComment() {
    myFixture.configureByText("test.compact", "/* comment <caret> */");
    myFixture.type('"');
    myFixture.checkResult("/* comment \"<caret> */");
  }

  public void testDoNotPairInsideExistingString() {
    myFixture.configureByText("test.compact", "let s = \"hello <caret> world\";");
    myFixture.type('"');
    myFixture.checkResult("let s = \"hello \"<caret> world\";");
  }

  public void testDoNotPairWhenQuoteSettingDisabled() {
    boolean original = CodeInsightSettings.getInstance().AUTOINSERT_PAIR_QUOTE;
    try {
      CodeInsightSettings.getInstance().AUTOINSERT_PAIR_QUOTE = false;
      myFixture.configureByText("test.compact", "let s = <caret>;");
      myFixture.type('"');
      myFixture.checkResult("let s = \"<caret>;");
    } finally {
      CodeInsightSettings.getInstance().AUTOINSERT_PAIR_QUOTE = original;
    }
  }

  // =========================================================================
  // Tier 4: Direct Handler Structural Verification & Resilience
  // =========================================================================

  public void testQuoteHandlerContract() {
    CompactQuoteHandler handler = new CompactQuoteHandler();
    myFixture.configureByText("test.compact", "let s = \"hello\"; // comment\nlet u = 'unterminated");
    EditorEx editor = (EditorEx) myFixture.getEditor();

    // Verify inside "hello"
    HighlighterIterator iter = editor.getHighlighter().createIterator(8);
    assertEquals(CompactTokenTypes.STRING_LITERAL, iter.getTokenType());
    assertTrue(handler.isOpeningQuote(iter, 8));
    assertFalse(handler.isClosingQuote(iter, 8));
    assertTrue(handler.isInsideLiteral(iter));

    // Verify closing quote at offset 14 (position of closing ")
    assertTrue(handler.isClosingQuote(iter, 14));
    assertFalse(handler.isOpeningQuote(iter, 14));

    // Verify inside comment
    HighlighterIterator commentIter = editor.getHighlighter().createIterator(18);
    assertEquals(CompactTokenTypes.LINE_COMMENT, commentIter.getTokenType());
    assertFalse(handler.isOpeningQuote(commentIter, 18));
    assertFalse(handler.isClosingQuote(commentIter, 18));
    assertFalse(handler.isInsideLiteral(commentIter));
    assertFalse(handler.hasNonClosedLiteral(editor, commentIter, 18));

    // Verify unterminated string on line 2
    int unterminatedOffset = myFixture.getFile().getText().indexOf("'unterminated");
    HighlighterIterator unterminatedIter = editor.getHighlighter().createIterator(unterminatedOffset);
    assertEquals(CompactTokenTypes.UNTERMINATED_STRING, unterminatedIter.getTokenType());
    assertTrue(handler.isOpeningQuote(unterminatedIter, unterminatedOffset));
    assertFalse(handler.isClosingQuote(unterminatedIter, unterminatedOffset));
    assertTrue(handler.isInsideLiteral(unterminatedIter));
    assertTrue(handler.hasNonClosedLiteral(editor, unterminatedIter, unterminatedOffset));
  }
}
