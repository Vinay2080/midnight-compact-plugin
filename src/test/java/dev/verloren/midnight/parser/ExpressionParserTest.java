package dev.verloren.midnight.parser;

import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.ParsingTestCase;

public class ExpressionParserTest extends ParsingTestCase {
  public ExpressionParserTest() {
    super("expressions", "compact", new CompactParserDefinition());
  }

  @Override
  protected String getTestDataPath() {
    return "src/test/testData";
  }

  public void testExpressionFormsAndAmbiguity() {
    String text = """
        export circuit expressions(a: Field, b: Field, c: Field): Field {
          const precedence = a + b * c == default<Field> ? f<Field>(a).method(b)[0] : S<Field>{ value: b };
          const member = token.field;
          const relational = a < b > c;
          const tuple = [a, ...items];
          const bytes = Bytes[1, 2, 3];
          const sliced = slice<4>(bytes, 0);
          const padded = pad(32, "a");
          const mapped = map(items, (x: Field) => x + 1);
          x += assert(a > 0);
          emit(x);
          disclose(x as Field);
          return fold(items, 0, (acc: Field, item: Field) => acc + item);
        }
        """;

    PsiFile file = parseFile("ExpressionFormsAndAmbiguity", text);
    String tree = DebugUtil.psiToString(file, true);

    assertEquals(text, file.getText());
    assertFalse(tree, tree.contains("PsiErrorElement"));
    assertTrue(tree.contains("BINARY_EXPR"));
    assertTrue(tree.contains("CAST_EXPR"));
    assertTrue(tree.contains("TERNARY_EXPR"));
    assertTrue(tree.contains("ASSIGN_EXPR"));
    assertTrue(tree.contains("INDEX_EXPR"));
    assertTrue(tree.contains("MEMBER_EXPR"));
    assertTrue(tree.contains("CALL_EXPR"));
    assertTrue(tree.contains("MAP_EXPR"));
    assertTrue(tree.contains("FOLD_EXPR"));
    assertTrue(tree.contains("SLICE_EXPR"));
    assertTrue(tree.contains("TUPLE_EXPR"));
    assertTrue(tree.contains("BYTES_EXPR"));
    assertTrue(tree.contains("STRUCT_LITERAL_EXPR"));
    assertTrue(tree.contains("ASSERT_EXPR"));
    assertTrue(tree.contains("EMIT_EXPR"));
    assertTrue(tree.contains("DISCLOSE_EXPR"));
    assertTrue(tree.contains("REFERENCE_EXPR"));
    assertTrue(tree.contains("LITERAL_EXPR"));
    assertTrue(tree.contains("PAD_EXPR"));
    assertTrue(tree.contains("DEFAULT_EXPR"));
    assertTrue(tree.contains("LAMBDA_EXPR"));
  }
}