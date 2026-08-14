package dev.verloren.midnight.parser;

import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.ParsingTestCase;

public class ErrorRecoveryParserTest extends ParsingTestCase {
  public ErrorRecoveryParserTest() {
    super("recovery", "compact", new CompactParserDefinition());
  }

  @Override
  protected String getTestDataPath() {
    return "src/test/testData";
  }

  public void testDeclarationErrorRecovery() {
    String text = """
            struct Point { x: ; y: Field; }
            circuit draw(): Boolean { return true; }
            """;

    PsiFile file = parseFile("DeclarationErrorRecovery", text);
    String tree = DebugUtil.psiToString(file, true);

    assertEquals(text, file.getText());
    assertTrue(tree.contains("PsiErrorElement"));
    assertTrue(tree.contains("STRUCT_DECLARATION"));
    assertTrue(tree.contains("CIRCUIT_DEFINITION"));
  }

  public void testStatementErrorRecovery() {
    String text = """
            circuit test(): Field {
              const x = +;
              const y = 42;
              return y;
            }
            """;

    PsiFile file = parseFile("StatementErrorRecovery", text);
    String tree = DebugUtil.psiToString(file, true);

    assertEquals(text, file.getText());
    assertTrue(tree.contains("PsiErrorElement"));
    assertTrue(tree.contains("CIRCUIT_DEFINITION"));
    assertTrue(tree.contains("CONST_STATEMENT"));
    assertTrue(tree.contains("RETURN_STATEMENT"));
  }

  public void testTopLevelInvalidTokenRecovery() {
    String text = """
            pragma language_version 1.0.0;
            @invalid_token;
            struct Point { x: Field; y: Field; }
            """;

    PsiFile file = parseFile("TopLevelInvalidTokenRecovery", text);
    String tree = DebugUtil.psiToString(file, true);

    assertEquals(text, file.getText());
    assertTrue(tree.contains("PsiErrorElement"));
    assertTrue(tree.contains("PRAGMA_FORM"));
    assertTrue(tree.contains("STRUCT_DECLARATION"));
  }

  public void testIsolatedExportKeywordRecovery() {
    String text = """
            pragma language_version 1.0.0;
            export ;
            struct Point { x: Field; }
            """;

    PsiFile file = parseFile("IsolatedExportKeywordRecovery", text);
    String tree = DebugUtil.psiToString(file, true);

    assertEquals(text, file.getText());
    assertTrue(tree.contains("PsiErrorElement"));
    assertTrue(tree.contains("PRAGMA_FORM"));
    assertTrue(tree.contains("STRUCT_DECLARATION"));
  }

  public void testExportAtEofRecovery() {
    String text = """
            pragma language_version 1.0.0;
            struct Point { x: Field; }
            export""";

    PsiFile file = parseFile("ExportAtEofRecovery", text);
    String tree = DebugUtil.psiToString(file, true);

    assertEquals(text, file.getText());
    assertTrue(tree.contains("PsiErrorElement"));
    assertTrue(tree.contains("PRAGMA_FORM"));
    assertTrue(tree.contains("STRUCT_DECLARATION"));
  }

  public void testExportModifierInvalidTokenRecovery() {
    String text = """
            export invalid_decl;
            struct Point { x: Field; }
            """;

    PsiFile file = parseFile("ExportModifierInvalidTokenRecovery", text);
    String tree = DebugUtil.psiToString(file, true);

    assertEquals(text, file.getText());
    assertTrue(tree.contains("PsiErrorElement"));
    assertTrue(tree.contains("STRUCT_DECLARATION"));
  }
}
