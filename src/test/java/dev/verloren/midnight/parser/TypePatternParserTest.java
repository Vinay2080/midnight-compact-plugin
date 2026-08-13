package dev.verloren.midnight.parser;

import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.ParsingTestCase;

public class TypePatternParserTest extends ParsingTestCase {
  public TypePatternParserTest() {
    super("types", "compact", new CompactParserDefinition());
  }

  @Override
  protected String getTestDataPath() {
    return "src/test/testData";
  }

  public void testTypesGenericsAndPatterns() {
    String text = """
            export struct Box<#N, T> {
              values: Vector<N, T>,
              range: Uint<0..N>,
              data: Bytes<N>,
              secret: Opaque<"tag">,
              pair: [Field, Boolean],
            };
            export witness choose<T>(left: T, right: Vector<2, T>): [T, Field];
            constructor([owner, balance]: [Field, Uint<64>], { key: value, flag }: Pair<Field>) {}
            """;

    PsiFile file = parseFile("TypesGenericsAndPatterns", text);
    String tree = DebugUtil.psiToString(file, true);

    assertEquals(text, file.getText());
    assertFalse(tree, tree.contains("PsiErrorElement"));
    assertTrue(tree.contains("GENERIC_PARAMETER_LIST"));
    assertTrue(tree.contains("GENERIC_PARAMETER"));
    assertTrue(tree.contains("GENERIC_ARGUMENT_LIST"));
    assertTrue(tree.contains("GENERIC_ARGUMENT"));
    assertTrue(tree.contains("BUILTIN_TYPE"));
    assertTrue(tree.contains("TUPLE_TYPE"));
    assertTrue(tree.contains("TYPE_SIZE"));
    assertTrue(tree.contains("PATTERN_PARAMETER_LIST"));
    assertTrue(tree.contains("TYPED_PATTERN"));
    assertTrue(tree.contains("PATTERN_STRUCT_ELEMENT"));
  }
}