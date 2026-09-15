package dev.verloren.midnight.editor;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * Unit test suite verifying angle bracket auto-closing ({@code <} mirroring),
 * step-over on {@code >}, and paired backspace deletion for Compact code.
 */
public class CompactAngleBraceTypingTest extends BasePlatformTestCase {

  public void testPairAngleBraceAfterVector() {
    myFixture.configureByText("test.compact", "export ledger state: Vector<caret>;");
    myFixture.type('<');
    myFixture.checkResult("export ledger state: Vector<<caret>>;");
  }

  public void testPairAngleBraceAfterUint() {
    myFixture.configureByText("test.compact", "let x: Uint<caret>;");
    myFixture.type('<');
    myFixture.checkResult("let x: Uint<<caret>>;");
  }

  public void testPairAngleBraceAfterBytes() {
    myFixture.configureByText("test.compact", "let x: Bytes<caret>;");
    myFixture.type('<');
    myFixture.checkResult("let x: Bytes<<caret>>;");
  }

  public void testPairAngleBraceAfterOpaque() {
    myFixture.configureByText("test.compact", "let x: Opaque<caret>;");
    myFixture.type('<');
    myFixture.checkResult("let x: Opaque<<caret>>;");
  }

  public void testPairAngleBraceAfterDefault() {
    myFixture.configureByText("test.compact", "let x = default<caret>;");
    myFixture.type('<');
    myFixture.checkResult("let x = default<<caret>>;");
  }

  public void testPairAngleBraceAfterSlice() {
    myFixture.configureByText("test.compact", "let x = slice<caret>(buf, 0);");
    myFixture.type('<');
    myFixture.checkResult("let x = slice<<caret>>(buf, 0);");
  }

  public void testPairAngleBraceAfterTypeIdentifier() {
    myFixture.configureByText("test.compact", "export ledger map: Map<caret>;");
    myFixture.type('<');
    myFixture.checkResult("export ledger map: Map<<caret>>;");
  }

  public void testPairAngleBraceAfterSingleLetterTypeParam() {
    myFixture.configureByText("test.compact", "let x: T<caret>;");
    myFixture.type('<');
    myFixture.checkResult("let x: T<<caret>>;");
  }

  public void testPairAngleBraceInCircuitGenericDeclaration() {
    myFixture.configureByText("test.compact", "circuit foo<caret>() {}");
    myFixture.type('<');
    myFixture.checkResult("circuit foo<<caret>>() {}");
  }

  public void testPairAngleBraceInWitnessGenericDeclaration() {
    myFixture.configureByText("test.compact", "witness bar<caret>();");
    myFixture.type('<');
    myFixture.checkResult("witness bar<<caret>>();");
  }

  public void testPairAngleBraceInStructGenericDeclaration() {
    myFixture.configureByText("test.compact", "struct Box<caret> { val: Field; }");
    myFixture.type('<');
    myFixture.checkResult("struct Box<<caret>> { val: Field; }");
  }

  public void testPairAngleBraceInTypeGenericDeclaration() {
    myFixture.configureByText("test.compact", "type Alias<caret> = Field;");
    myFixture.type('<');
    myFixture.checkResult("type Alias<<caret>> = Field;");
  }

  public void testPairAngleBraceInModuleGenericDeclaration() {
    myFixture.configureByText("test.compact", "module Mod<caret> {}");
    myFixture.type('<');
    myFixture.checkResult("module Mod<<caret>> {}");
  }

  public void testPairAngleBraceInContractGenericDeclaration() {
    myFixture.configureByText("test.compact", "contract Cont<caret> {}");
    myFixture.type('<');
    myFixture.checkResult("contract Cont<<caret>> {}");
  }

  public void testPairAngleBraceNested() {
    myFixture.configureByText("test.compact", "let v: Vector<Uint<caret>>;");
    myFixture.type('<');
    myFixture.checkResult("let v: Vector<Uint<<caret>>>;");
  }

  public void testTypeClosingAngleBraceStepsOver() {
    myFixture.configureByText("test.compact", "let v: Vector<<caret>>;");
    myFixture.type('>');
    myFixture.checkResult("let v: Vector<><caret>;");
  }

  public void testBackspaceDeletesMatchingClosingBrace() {
    myFixture.configureByText("test.compact", "let v: Vector<<caret>>;");
    myFixture.type('\b');
    myFixture.checkResult("let v: Vector<caret>;");
  }

  public void testDontPairAngleBraceAfterComparisonOperator() {
    myFixture.configureByText("test.compact", "if (a <caret>) {}");
    myFixture.type('<');
    myFixture.checkResult("if (a <<caret>) {}");
  }

  public void testDontPairAngleBraceInMiddleOfIdentifier() {
    myFixture.configureByText("test.compact", "struct Fo<caret>o {}");
    myFixture.type('<');
    myFixture.checkResult("struct Fo<<caret>o {}");
  }

  public void testDontPairAngleBraceAfterConstantIdentifier() {
    myFixture.configureByText("test.compact", "const CONSTANT<caret> = 1;");
    myFixture.type('<');
    myFixture.checkResult("const CONSTANT<<caret> = 1;");
  }

  public void testDontPairAngleBraceInLineComment() {
    myFixture.configureByText("test.compact", "// Vector<caret>\n");
    myFixture.type('<');
    myFixture.checkResult("// Vector<<caret>\n");
  }

  public void testDontPairAngleBraceInBlockComment() {
    myFixture.configureByText("test.compact", "/* Vector<caret> */");
    myFixture.type('<');
    myFixture.checkResult("/* Vector<<caret> */");
  }

  public void testDontPairAngleBraceInString() {
    myFixture.configureByText("test.compact", "let s = \"Vector<caret>\";");
    myFixture.type('<');
    myFixture.checkResult("let s = \"Vector<<caret>\";");
  }

  public void testDontPairAngleBraceWhenAutoinsertDisabled() {
    boolean original = CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET;
    try {
      CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET = false;
      myFixture.configureByText("test.compact", "let v: Vector<caret>;");
      myFixture.type('<');
      myFixture.checkResult("let v: Vector<<caret>;");
    } finally {
      CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET = original;
    }
  }
}
