package dev.verloren.midnight.editor;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class CompactDocCommentEnterTest extends BasePlatformTestCase {

  public void testDocCommentOpeningEnter() {
    myFixture.configureByText("test.compact", "/**<caret>");
    myFixture.type('\n');
    String text = myFixture.getEditor().getDocument().getText();
    assertEquals("/**\n* \n*/", text);
  }

  public void testDocCommentWithMultipleAsterisksOpeningEnter() {
    myFixture.configureByText("test.compact", "/****<caret>");
    myFixture.type('\n');
    String text = myFixture.getEditor().getDocument().getText();
    assertFalse("Should never have duplicate asterisks", text.contains("*  *"));
    assertFalse("Should never have duplicate closing comments", text.endsWith("*/\n*/"));
  }

  public void testBlockCommentOpeningEnter() {
    myFixture.configureByText("test.compact", "/*<caret>");
    myFixture.type('\n');
    String text = myFixture.getEditor().getDocument().getText();
    assertEquals("/*\n * \n */", text);
  }

  public void testDocCommentContinuation() {
    myFixture.configureByText("test.compact",
        "/**\n" +
        " * Hello world<caret>\n" +
        " */"
    );
    myFixture.type('\n');
    String text = myFixture.getEditor().getDocument().getText();
    assertFalse("Should never contain duplicate asterisks", text.contains("* *"));
    assertTrue("Should continue star comment line",
        text.contains(" * Hello world\n * "));
  }

  public void testDocCommentMultipleEntersNoDuplicateStars() {
    myFixture.configureByText("test.compact",
        "/**\n" +
        " * Line 1<caret>\n" +
        " */"
    );
    myFixture.type('\n');
    myFixture.type("Line 2");
    myFixture.type('\n');
    String text = myFixture.getEditor().getDocument().getText();
    assertFalse("Should not have duplicate asterisks on subsequent lines", text.contains("* *"));
    assertTrue("Should contain Line 2 with proper single asterisk prefix",
        text.contains(" * Line 1\n * Line 2\n * "));
  }

  public void testBlockCommentEnterDoesNotDuplicateExistingClosing() {
    myFixture.configureByText("test.compact",
        "/*\n" +
        " * Existing line<caret>\n" +
        " */"
    );
    myFixture.type('\n');
    String text = myFixture.getEditor().getDocument().getText();
    assertFalse("Should never duplicate closing comment", text.contains("*/\n */"));
  }

  public void testBlockCommentOpeningAfterClosingOnSameLine() {
    myFixture.configureByText("test.compact",
        "/* first */ /*<caret>"
    );
    myFixture.type('\n');
    String text = myFixture.getEditor().getDocument().getText();
    assertTrue("Should continue with star prefix on newline", text.contains("/* first */ /*\n * "));
    assertTrue("Should close the new block comment", text.contains("\n */"));
  }

  public void testBlockCommentOpeningAfterMultilineClosingOnSameLine() {
    myFixture.configureByText("test.compact",
        "/*\n" +
        " * but */ /*<caret>"
    );
    myFixture.type('\n');
    String text = myFixture.getEditor().getDocument().getText();
    assertTrue("Should continue with star prefix on newline", text.contains(" * but */ /*\n * "));
    assertTrue("Should close the new block comment", text.contains("\n */"));
  }

  public void testClosedCommentAloneDoesNotScaffold() {
    myFixture.configureByText("test.compact",
        "/* first */<caret>"
    );
    myFixture.type('\n');
    String text = myFixture.getEditor().getDocument().getText();
    assertEquals("/* first */\n", text);
  }
}
