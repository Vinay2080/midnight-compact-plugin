package dev.verloren.midnight.type;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.psi.CompactExpression;

public class CompactTypeInferenceTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    com.intellij.lang.LanguageParserDefinitions.INSTANCE.addExplicitExtension(
            dev.verloren.midnight.CompactLanguage.INSTANCE,
            new dev.verloren.midnight.parser.CompactParserDefinition()
    );
  }

  public void testLiteralTypes() {
    assertExpressionType("true", "Boolean");
    assertExpressionType("false", "Boolean");
    assertExpressionType("42", "Field");
    assertExpressionType("0x123", "Field");
  }

  private void assertExpressionType(String code, String expectedType) {
    myFixture.configureByText(CompactFileType.INSTANCE, "circuit test() { const x = <caret>" + code + "; }");
    CompactExpression expr = getExpressionAtCaret();
    assertEquals(expectedType, expr.getType().getName());
  }

  private CompactExpression getExpressionAtCaret() {
    int offset = myFixture.getCaretOffset();
    com.intellij.psi.PsiElement element = myFixture.getFile().findElementAt(offset);
    CompactExpression expression = null;
    while (element != null && !(element instanceof CompactExpression)) {
      element = element.getParent();
    }
    while (element != null) {
      expression = (CompactExpression) element;
      com.intellij.psi.PsiElement parent = element.getParent();
      if (!(parent instanceof CompactExpression)) {
        break;
      }
      element = parent;
    }
    assertNotNull("Expression not found at caret", expression);
    return expression;
  }

  public void testBinaryExpressionTypes() {
    assertExpressionType("true && false", "Boolean");
    assertExpressionType("1 + 2", "Field");
    assertExpressionType("1 == 1", "Boolean");
    assertExpressionType("1 < 2", "Boolean");
  }

  public void testUnaryExpressionTypes() {
    assertExpressionType("!true", "Boolean");
    assertExpressionType("-1", "Field");
  }

  public void testReferenceTypes() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit test(amount: Uint<32>) {
                      const val = <caret>amount;
                    }
                    """
    );
    CompactExpression expr = getExpressionAtCaret();
    assertEquals("Uint<32>", expr.getType().getName());
  }

  public void testConstBindingTypeInference() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit test() {
                      const val = 42;
                      const x = <caret>val;
                    }
                    """
    );
    CompactExpression expr = getExpressionAtCaret();
    assertEquals("Field", expr.getType().getName());
  }

  public void testStructFieldTypeInference() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct Point { x: Field, y: Boolean }
                    circuit test(p: Point) {
                      const val = p.<caret>y;
                    }
                    """
    );
    CompactExpression expr = getExpressionAtCaret();
    assertEquals("Boolean", expr.getType().getName());
  }
}
