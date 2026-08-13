package dev.verloren.midnight.parser;

import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.ParsingTestCase;

public class DeclarationParserTest extends ParsingTestCase {
  public DeclarationParserTest() {
    super("declarations", "compact", new CompactParserDefinition());
  }

  @Override
  protected String getTestDataPath() {
    return "src/test/testData";
  }

  public void testTopLevelDeclarations() {
    String text = declarationFixture();
    PsiFile file = parseFile("TopLevelDeclarations", text);
    String tree = DebugUtil.psiToString(file, true);

    assertEquals(text, file.getText());
    assertFalse(tree, tree.contains("PsiErrorElement"));
    assertTrue(tree.contains("INCLUDE_FORM"));
    assertTrue(tree.contains("IMPORT_FORM"));
    assertTrue(tree.contains("EXPORT_FORM"));
    assertTrue(tree.contains("MODULE_DEFINITION"));
    assertTrue(tree.contains("STRUCT_DECLARATION"));
    assertTrue(tree.contains("ENUM_DECLARATION"));
    assertTrue(tree.contains("CONTRACT_DECLARATION"));
    assertTrue(tree.contains("IMPLEMENTS_DECLARATION"));
    assertTrue(tree.contains("TYPE_ALIAS_DECLARATION"));
    assertTrue(tree.contains("LEDGER_DECLARATION"));
    assertTrue(tree.contains("WITNESS_DECLARATION"));
    assertTrue(tree.contains("CONSTRUCTOR_DEFINITION"));
    assertTrue(tree.contains("CIRCUIT_DEFINITION"));
  }

  public static String declarationFixture() {
    return """
        pragma language_version 1;
        include "std.compact";
        import { Foo, Bar as Baz } from "lib.compact" prefix Lib;
        export { Foo, Bar };
        export module Math { ledger inner: Field; }
        export struct Pair<T> { left: Field; right: Field; };
        enum Choice { A, B, }
        export contract Token { pure circuit balance(owner: Field): Field; circuit transfer(to: Field, amount: Uint<64>): Boolean, };
        contract implements Token;
        export new type Amount = Uint<64>;
        export sealed ledger totalSupply: Uint<64>;
        export witness secret(): Field;
        constructor() {}
        export pure circuit mint(to: Field, amount: Uint<64>): Boolean {}
        """;
  }
}