package dev.verloren.midnight.type;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.psi.CompactExpression;
import dev.verloren.midnight.psi.CompactStructFieldImpl;

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
    assertEquals(expectedType, expr.getType().name());
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
    assertEquals("Uint<32>", expr.getType().name());
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
    assertEquals("Field", expr.getType().name());
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
    CompactStructFieldImpl field = com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(myFixture.getFile(), CompactStructFieldImpl.class)
            .stream()
            .filter(candidate -> "y".equals(candidate.getName()))
            .findFirst()
            .orElse(null);
    assertNotNull("Struct field 'y' should be parsed as a typed PSI field", field);
    assertEquals("Boolean", field.getType().name());
    CompactExpression expr = getExpressionAtCaret();
    assertEquals("Boolean", expr.getType().name());
  }

  public void testCastExpressionTypes() {
    assertExpressionType("10 as Uint<64>", "Uint<64>");
  }

  public void testStringLiteralTypes() {
    assertExpressionType("\"hello\"", "Bytes");
  }

  public void testNestedArithmeticBinaryExpressionTypes() {
    assertExpressionType("1 + 2 * 3", "Field");
  }

  public void testNestedLogicalBinaryExpressionTypes() {
    assertExpressionType("(true || false) && true", "Boolean");
  }

  public void testStructLiteralTypeInference() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct Point { x: Field, y: Boolean }
                    circuit test() {
                      const p = <caret>Point { x: 0, y: true };
                    }
                    """
    );
    CompactExpression expr = getExpressionAtCaret();
    assertEquals("Point", expr.getType().name());
  }

  public void testUnresolvedReferenceTypeFallback() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit test() {
                      const x = <caret>nonExistentVar;
                    }
                    """
    );
    CompactExpression expr = getExpressionAtCaret();
    assertEquals("Unknown", expr.getType().name());
  }

  public void testUintArithmeticTypeInference() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit test(x: Uint<8>) {
                      const val = <caret>x + 1;
                    }
                    """
    );
    assertEquals("Uint<8>", getExpressionAtCaret().getType().name());

    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit test(x: Uint<8>) {
                      const val = <caret>1 + x;
                    }
                    """
    );
    assertEquals("Uint<8>", getExpressionAtCaret().getType().name());
  }

  public void testUintComparisonTypeInference() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit test(x: Uint<8>) {
                      const val = <caret>x > 0;
                    }
                    """
    );
    assertEquals("Boolean", getExpressionAtCaret().getType().name());

    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit test(x: Uint<8>) {
                      const val = <caret>x <= 20;
                    }
                    """
    );
    assertEquals("Boolean", getExpressionAtCaret().getType().name());
  }

  public void testHexAndBinaryLiteralInference() {
    assertExpressionType("0xFF", "Field");
    assertExpressionType("0b1010", "Field");
    assertExpressionType("0o77", "Field");
  }
}
