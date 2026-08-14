package dev.verloren.midnight.parser;

import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.ParsingTestCase;

public class StatementParserTest extends ParsingTestCase {
  public StatementParserTest() {
    super("statements", "compact", new CompactParserDefinition());
  }

  @Override
  protected String getTestDataPath() {
    return "src/test/testData";
  }

  public void testStatementsAndBlocks() {
    String text = """
            export circuit test(flag: Boolean): Field {
              const x = 1, y: Field = pad(32, "a");
              if (flag) { return x; } else return y;
              for (const i of 0..10) { x; }
              for (const item of items) item;
              return;
            }
            """;

    PsiFile file = parseFile("StatementsAndBlocks", text);
    String tree = DebugUtil.psiToString(file, true);

    assertEquals(text, file.getText());
    assertFalse(tree, tree.contains("PsiErrorElement"));
    assertTrue(tree.contains("BLOCK"));
    assertTrue(tree.contains("CONST_STATEMENT"));
    assertTrue(tree.contains("CONST_BINDING"));
    assertTrue(tree.contains("IF_STATEMENT"));
    assertTrue(tree.contains("FOR_STATEMENT"));
    assertTrue(tree.contains("RETURN_STATEMENT"));
    assertTrue(tree.contains("EXPR_STATEMENT"));
    assertTrue(tree.contains("EXPRESSION_SEQUENCE"));
  }

  public void testIncompleteBlockRecovery() {
    String text = """
            export circuit bad(flag: Boolean): Field {
              const x = ;
              if (flag) { return x; }
            """;

    PsiFile file = parseFile("IncompleteBlockRecovery", text);
    String tree = DebugUtil.psiToString(file, true);

    assertEquals(text, file.getText());
    assertTrue(tree.contains("PsiErrorElement"));
    assertTrue(tree.contains("BLOCK"));
    assertTrue(tree.contains("IF_STATEMENT"));
  }

  public void testDiscloseAndPatternDestructuring() {
    String text = """
            export circuit PrivacyOps(secret: Field): Boolean {
              const disclosedVal = disclose(secret);
              const [a, b] = [1, 2];
              return disclosedVal == a;
            }
            """;

    PsiFile file = parseFile("DiscloseAndPatternDestructuring", text);
    String tree = DebugUtil.psiToString(file, true);
    System.out.println("STATEMENT TREE:\n" + tree);

    assertEquals(text, file.getText());
    assertFalse(tree, tree.contains("PsiErrorElement"));
    assertTrue(tree.contains("DISCLOSE_EXPR"));
    assertTrue(tree.contains("PATTERN"));
    assertTrue(tree.contains("TUPLE_EXPR"));
  }
}