package dev.verloren.midnight.editor;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * Unit test suite verifying automatic delimiter and structural symbol skipping
 * (step-over) during editor typing in Compact code.
 */
public class CompactDelimiterTypingTest extends BasePlatformTestCase {

  // --- Parentheses ---

  public void testSkipClosingParenInWitnessDeclaration() {
    myFixture.configureByText("test.compact", "witness localSk(<caret>): Bytes<32>;");
    myFixture.type(')');
    myFixture.checkResult("witness localSk()<caret>: Bytes<32>;");
  }

  public void testSequentialParenAndColonSkipping() {
    myFixture.configureByText("test.compact", "witness localSk(<caret>): Bytes<32>;");
    myFixture.type(')');
    myFixture.type(':');
    myFixture.checkResult("witness localSk():<caret> Bytes<32>;");
  }

  public void testSkipClosingParenInCircuitDeclaration() {
    myFixture.configureByText("test.compact", "circuit compute(a: Field<caret>): Field { return a; }");
    myFixture.type(')');
    myFixture.checkResult("circuit compute(a: Field)<caret>: Field { return a; }");
  }

  public void testSkipNestedClosingParens() {
    myFixture.configureByText("test.compact", "let x = foo(bar(<caret>));");
    myFixture.type(')');
    myFixture.checkResult("let x = foo(bar()<caret>);");
    myFixture.type(')');
    myFixture.checkResult("let x = foo(bar())<caret>;");
  }

  // --- Colons ---

  public void testSkipColonAfterFunctionParens() {
    myFixture.configureByText("test.compact", "witness localSk()<caret>: Bytes<32>;");
    myFixture.type(':');
    myFixture.checkResult("witness localSk():<caret> Bytes<32>;");
  }

  public void testSkipColonInVariableBinding() {
    myFixture.configureByText("test.compact", "let x<caret>: Uint<32> = 1;");
    myFixture.type(':');
    myFixture.checkResult("let x:<caret> Uint<32> = 1;");
  }

  public void testSkipColonInStructLiteral() {
    myFixture.configureByText("test.compact", "let s = { field<caret>: 42 };");
    myFixture.type(':');
    myFixture.checkResult("let s = { field:<caret> 42 };");
  }

  // --- Semicolons ---

  public void testSkipSemicolonAtStatementEnd() {
    myFixture.configureByText("test.compact", "witness localSk(): Bytes<32><caret>;");
    myFixture.type(';');
    myFixture.checkResult("witness localSk(): Bytes<32>;<caret>");
  }

  public void testSkipSemicolonInLetBinding() {
    myFixture.configureByText("test.compact", "let x = 42<caret>;");
    myFixture.type(';');
    myFixture.checkResult("let x = 42;<caret>");
  }

  // --- Brackets ---

  public void testSkipClosingBracketInVectorLiteral() {
    myFixture.configureByText("test.compact", "let arr = [1, 2<caret>];");
    myFixture.type(']');
    myFixture.checkResult("let arr = [1, 2]<caret>;");
  }

  public void testSkipClosingBracketInEmptyVector() {
    myFixture.configureByText("test.compact", "let arr = [<caret>];");
    myFixture.type(']');
    myFixture.checkResult("let arr = []<caret>;");
  }

  public void testSkipNestedClosingBrackets() {
    myFixture.configureByText("test.compact", "let matrix = [[<caret>]];");
    myFixture.type(']');
    myFixture.checkResult("let matrix = [[]<caret>];");
    myFixture.type(']');
    myFixture.checkResult("let matrix = [[]]<caret>;");
  }

  // --- Braces ---

  public void testSkipClosingBraceInCircuitBody() {
    myFixture.configureByText("test.compact", "circuit foo() {<caret>}");
    myFixture.type('}');
    myFixture.checkResult("circuit foo() {}<caret>");
  }

  public void testSkipClosingBraceInStructDefinition() {
    myFixture.configureByText("test.compact", "struct MyData {<caret>}");
    myFixture.type('}');
    myFixture.checkResult("struct MyData {}<caret>");
  }

  public void testSkipClosingBraceWithWhitespaceInside() {
    myFixture.configureByText("test.compact", "circuit foo() { return; <caret>}");
    myFixture.type('}');
    myFixture.checkResult("circuit foo() { return; }<caret>");
  }

  // --- Commas ---

  public void testSkipCommaInArgumentList() {
    myFixture.configureByText("test.compact", "foo(a<caret>, b);");
    myFixture.type(',');
    myFixture.checkResult("foo(a,<caret> b);");
  }

  public void testSkipCommaInVectorElements() {
    myFixture.configureByText("test.compact", "let v = [1<caret>, 2];");
    myFixture.type(',');
    myFixture.checkResult("let v = [1,<caret> 2];");
  }

  // --- Angle Brackets ---

  public void testSkipClosingAngleBraceInGenericType() {
    myFixture.configureByText("test.compact", "let v: Vector<Field<caret>>;");
    myFixture.type('>');
    myFixture.checkResult("let v: Vector<Field><caret>;");
  }

  // --- Quotes ---

  public void testSkipClosingDoubleQuote() {
    myFixture.configureByText("test.compact", "let s = \"hello<caret>\";");
    myFixture.type('"');
    myFixture.checkResult("let s = \"hello\"<caret>;");
  }

  public void testSkipClosingDoubleQuoteInEmptyString() {
    myFixture.configureByText("test.compact", "let s = \"<caret>\";");
    myFixture.type('"');
    myFixture.checkResult("let s = \"\"<caret>;");
  }

  public void testSkipClosingSingleQuote() {
    myFixture.configureByText("test.compact", "let s = 'hello<caret>';");
    myFixture.type('\'');
    myFixture.checkResult("let s = 'hello'<caret>;");
  }

  public void testSkipClosingSingleQuoteInEmptyString() {
    myFixture.configureByText("test.compact", "let s = '<caret>';");
    myFixture.type('\'');
    myFixture.checkResult("let s = ''<caret>;");
  }

  // --- Negative Tests & Boundary Contexts ---

  public void testDoNotSkipInsideLineComment() {
    myFixture.configureByText("test.compact", "// this is a comment with (paren<caret>)");
    myFixture.type(')');
    myFixture.checkResult("// this is a comment with (paren)<caret>)");
  }

  public void testDoNotSkipColonInsideLineComment() {
    myFixture.configureByText("test.compact", "// note<caret>: something");
    myFixture.type(':');
    myFixture.checkResult("// note:<caret>: something");
  }

  public void testDoNotSkipColonInsideStringLiteral() {
    myFixture.configureByText("test.compact", "let s = \"http<caret>: //localhost\";");
    myFixture.type(':');
    myFixture.checkResult("let s = \"http:<caret>: //localhost\";");
  }

  public void testDoNotSkipParenInsideStringLiteral() {
    myFixture.configureByText("test.compact", "let s = \"foo(<caret>)\";");
    myFixture.type(')');
    myFixture.checkResult("let s = \"foo()<caret>)\";");
  }

  public void testDoNotSkipOpeningQuoteBeforeString() {
    myFixture.configureByText("test.compact", "let s = <caret>\"hello\";");
    myFixture.type('"');
    myFixture.checkResult("let s = \"<caret>\"hello\";");
  }

  public void testDoNotSkipWhenNextCharDiffers() {
    myFixture.configureByText("test.compact", "let x = <caret>42;");
    myFixture.type(')');
    myFixture.checkResult("let x = )<caret>42;");
  }

  public void testDoNotSkipBracketWhenSettingDisabled() {
    boolean original = CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET;
    try {
      CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET = false;
      myFixture.configureByText("test.compact", "witness localSk(<caret>): Bytes<32>;");
      myFixture.type(')');
      myFixture.checkResult("witness localSk()<caret>): Bytes<32>;");
    } finally {
      CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET = original;
    }
  }

  public void testDoNotSkipQuoteWhenSettingDisabled() {
    boolean original = CodeInsightSettings.getInstance().AUTOINSERT_PAIR_QUOTE;
    try {
      CodeInsightSettings.getInstance().AUTOINSERT_PAIR_QUOTE = false;
      myFixture.configureByText("test.compact", "let s = \"hello<caret>\";");
      myFixture.type('"');
      myFixture.checkResult("let s = \"hello\"<caret>\";");
    } finally {
      CodeInsightSettings.getInstance().AUTOINSERT_PAIR_QUOTE = original;
    }
  }
}
