package dev.verloren.midnight.parser;

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
    doTest(true);
  }

  public void testInvalidPragma() {
    doTest(true);
  }

  public void testHangTest() {
    doTest(true);
  }
}