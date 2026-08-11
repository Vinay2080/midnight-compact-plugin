package dev.verloren.midnight.parser;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.ParsingTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class EndToEndParserTest extends ParsingTestCase {
  public EndToEndParserTest() {
    super("endToEnd", "compact", new CompactParserDefinition());
  }

  @Override
  protected String getTestDataPath() {
    return "src/test/testData";
  }

  public void testReferenceTypeExampleParsesWithoutErrors() throws IOException {
    String text = Files.readString(Path.of("references", "type-example.compact"));
    PsiFile file = parseFile("TypeExample", text);
    String tree = DebugUtil.psiToString(file, true);
    PsiErrorElement error = PsiTreeUtil.findChildOfType(file, PsiErrorElement.class);

    assertEquals(text, file.getText());
    assertNull(error == null ? tree : error.getErrorDescription() + " at " + error.getTextRange() + " near '" + error.getText() + "'\n" + tree, error);
    assertTrue(tree.contains("CIRCUIT_DEFINITION"));
    assertTrue(tree.contains("STRUCT_DECLARATION"));
    assertTrue(tree.contains("TYPE_ALIAS_DECLARATION"));
  }

  public void testBadDeclarationRecoveryKeepsFollowingDeclaration() {
    String text = """
        ledger missingType: ;
        export circuit ok(): Field { return 1; }
        """;
    PsiFile file = parseFile("BadDeclarationRecovery", text);
    String tree = DebugUtil.psiToString(file, true);

    assertEquals(text, file.getText());
    assertTrue(tree.contains("PsiErrorElement"));
    assertTrue(tree.contains("CIRCUIT_DEFINITION"));
  }
}