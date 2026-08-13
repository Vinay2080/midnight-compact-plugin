package dev.verloren.midnight.parser;

import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.ParsingTestCase;

public class PragmaParserTest extends ParsingTestCase {

  public PragmaParserTest() {
    super("pragma", "compact", new CompactParserDefinition());
  }

  @Override
  protected String getTestDataPath() {
    return "src/test/testData";
  }

  public void testValidPragma() {
    String text = """
            pragma language_version 1;
            pragma language_version 1.0;
            pragma compiler_version 1.2.3;
            """;

    PsiFile file = parseFile("ValidPragma", text);

    assertEquals(text, file.getText());
    assertFalse(DebugUtil.psiToString(file, true).contains("PsiErrorElement"));
  }

  public void testInvalidPragma() {
    String text = """
            pragma 1;
            pragma version;
            pragma version 12.;
            """;

    PsiFile file = parseFile("InvalidPragma", text);
    String tree = DebugUtil.psiToString(file, true);

    assertEquals(text, file.getText());
    assertTrue(tree.contains("PsiErrorElement"));
    assertTrue(tree.contains("INVALID_VERSION"));
  }

  public void testHangTest() {
    String text = """
            pragma
            pragma version 1
            pragma compiler_version 1.2.3;
            """;

    PsiFile file = parseFile("HangTest", text);

    assertEquals(text, file.getText());
    assertTrue(DebugUtil.psiToString(file, true).contains("PsiErrorElement"));
  }
}
