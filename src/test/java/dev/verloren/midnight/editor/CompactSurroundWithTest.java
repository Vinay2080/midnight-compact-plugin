package dev.verloren.midnight.editor;

import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.psi.CompactCircuitDefinition;

public class CompactSurroundWithTest extends BasePlatformTestCase {

  public void testSurroundDescriptorAvailable() {
    CompactSurroundDescriptor descriptor = new CompactSurroundDescriptor();
    Surrounder[] surrounders = descriptor.getSurrounders();
    assertNotNull(surrounders);
    assertEquals(2, surrounders.length);
    assertEquals("if (expr) { ... }", surrounders[0].getTemplateDescription());
    assertEquals("{ ... }", surrounders[1].getTemplateDescription());
    assertFalse(descriptor.isExclusive());
  }

  public void testElementsToSurroundWithSelection() {
    String code = """
            pragma language_version >= 0.20.0;
            
            export circuit foo(): Void {
              assert true, "ok";
            }
            """;

    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    CompactSurroundDescriptor descriptor = new CompactSurroundDescriptor();

    CompactCircuitDefinition circuit = PsiTreeUtil.findChildOfType(file, CompactCircuitDefinition.class);
    assertNotNull(circuit);

    int offset = code.indexOf("assert");
    PsiElement[] elements = descriptor.getElementsToSurround(file, offset, offset + 18);
    assertTrue("Should find elements to surround with selection", elements.length > 0);
  }

  public void testElementsToSurroundWithoutSelection() {
    String code = """
            pragma language_version >= 0.20.0;
            
            export circuit foo(): Void {
              assert true, "ok";
            }
            """;

    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    CompactSurroundDescriptor descriptor = new CompactSurroundDescriptor();

    int offset = code.indexOf("assert") + 2; // Caret sitting inside "assert"
    PsiElement[] elements = descriptor.getElementsToSurround(file, offset, offset);
    assertTrue("Should find element to surround at cursor without selection", elements.length > 0);
  }

  public void testIfSurrounderExecution() {
    String code = """
            pragma language_version >= 0.20.0;
            
            export circuit foo(): Void {
              assert true, "ok";
            }
            """;

    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    CompactSurroundDescriptor descriptor = new CompactSurroundDescriptor();

    int offset = code.indexOf("assert");
    PsiElement[] elements = descriptor.getElementsToSurround(file, offset, offset + 18);
    assertTrue(elements.length > 0);

    CompactIfSurrounder ifSurrounder = new CompactIfSurrounder();
    assertTrue(ifSurrounder.isApplicable(elements));

    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      TextRange range = ifSurrounder.surroundElements(getProject(), myFixture.getEditor(), elements);
      assertNotNull(range);
    });

    String text = myFixture.getEditor().getDocument().getText();
    assertTrue("Should contain if (true)", text.contains("if (true)"));
    assertTrue("Should contain assert true, \"ok\";", text.contains("assert true, \"ok\";"));
  }

  public void testBlockSurrounderExecution() {
    String code = """
            pragma language_version >= 0.20.0;
            
            export circuit foo(): Void {
              assert true, "ok";
            }
            """;

    PsiFile file = myFixture.configureByText(CompactFileType.INSTANCE, code);
    CompactSurroundDescriptor descriptor = new CompactSurroundDescriptor();

    int offset = code.indexOf("assert");
    PsiElement[] elements = descriptor.getElementsToSurround(file, offset, offset + 18);
    assertTrue(elements.length > 0);

    CompactBlockSurrounder blockSurrounder = new CompactBlockSurrounder();
    assertTrue(blockSurrounder.isApplicable(elements));

    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      TextRange range = blockSurrounder.surroundElements(getProject(), myFixture.getEditor(), elements);
      assertNotNull(range);
    });

    String text = myFixture.getEditor().getDocument().getText();
    assertTrue("Should contain surrounding block", text.contains("{\n"));
    assertTrue("Should contain assert statement", text.contains("assert true, \"ok\";"));
  }
}
