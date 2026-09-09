package dev.verloren.midnight.editor;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.editor.smartEnter.CompactSmartEnterProcessor;

public class CompactSmartEnterTest extends BasePlatformTestCase {

  public void testCircuitHeaderCompletionWithoutReturnType() {
    myFixture.configureByText("test.compact", "circuit transfer(to: Address)<caret>");
    new CompactSmartEnterProcessor().process(getProject(), myFixture.getEditor(), myFixture.getFile());
    String text = myFixture.getEditor().getDocument().getText();
    assertTrue("Should complete circuit with : Void and braces",
        text.contains("circuit transfer(to: Address): Void {\n  \n}"));
    int caretOffset = myFixture.getEditor().getCaretModel().getOffset();
    int expectedCaret = text.indexOf("Void {\n  ") + "Void {\n  ".length();
    assertEquals("Caret must be placed inside the circuit body", expectedCaret, caretOffset);
  }

  public void testCircuitHeaderCompletionTrailingColonDoesNotHardcodeVoid() {
    myFixture.configureByText("test.compact", "circuit transfer(to: Address):<caret>");
    new CompactSmartEnterProcessor().process(getProject(), myFixture.getEditor(), myFixture.getFile());
    String text = myFixture.getEditor().getDocument().getText();
    assertFalse("Trailing colon must NEVER hardcode Void", text.contains("Void"));
    assertEquals("circuit transfer(to: Address): ", text);
    assertEquals("Caret must be positioned after colon and space for typing return type",
        "circuit transfer(to: Address): ".length(), myFixture.getEditor().getCaretModel().getOffset());
  }

  public void testCircuitHeaderCompletionIncompleteByteType() {
    myFixture.configureByText("test.compact", "circuit transfer(to: Address): Byte<caret>");
    new CompactSmartEnterProcessor().process(getProject(), myFixture.getEditor(), myFixture.getFile());
    String text = myFixture.getEditor().getDocument().getText();
    assertFalse("Incomplete Byte type must not blindly append braces", text.contains("{"));
    assertEquals("circuit transfer(to: Address): Bytes<>", text);
    int expectedCaret = text.indexOf("Bytes<") + "Bytes<".length();
    assertEquals("Caret must be inside angle brackets to enter byte length", expectedCaret, myFixture.getEditor().getCaretModel().getOffset());
  }

  public void testCircuitHeaderCompletionIncompleteUintType() {
    myFixture.configureByText("test.compact", "circuit transfer(to: Address): Uint<caret>");
    new CompactSmartEnterProcessor().process(getProject(), myFixture.getEditor(), myFixture.getFile());
    String text = myFixture.getEditor().getDocument().getText();
    assertFalse("Incomplete Uint type must not blindly append braces", text.contains("{"));
    assertEquals("circuit transfer(to: Address): Uint<>", text);
    int expectedCaret = text.indexOf("Uint<") + "Uint<".length();
    assertEquals("Caret must be inside angle brackets to enter bit size", expectedCaret, myFixture.getEditor().getCaretModel().getOffset());
  }

  public void testCircuitHeaderCompletionUnclosedAngleBracket() {
    myFixture.configureByText("test.compact", "circuit transfer(to: Address): Bytes<32<caret>");
    new CompactSmartEnterProcessor().process(getProject(), myFixture.getEditor(), myFixture.getFile());
    String text = myFixture.getEditor().getDocument().getText();
    assertTrue("Should close angle bracket and append braces",
        text.contains("circuit transfer(to: Address): Bytes<32> {\n  \n}"));
  }

  public void testCircuitHeaderCompletionWithReturnType() {
    myFixture.configureByText("test.compact", "circuit query(): Uint<64><caret>");
    new CompactSmartEnterProcessor().process(getProject(), myFixture.getEditor(), myFixture.getFile());
    String text = myFixture.getEditor().getDocument().getText();
    assertTrue("Should complete circuit with braces",
        text.contains("circuit query(): Uint<64> {\n  \n}"));
    int caretOffset = myFixture.getEditor().getCaretModel().getOffset();
    int expectedCaret = text.indexOf("Uint<64> {\n  ") + "Uint<64> {\n  ".length();
    assertEquals("Caret must be placed inside the circuit body", expectedCaret, caretOffset);
  }

  public void testContractDeclarationCompletion() {
    myFixture.configureByText("test.compact", "export contract Token<caret>");
    new CompactSmartEnterProcessor().process(getProject(), myFixture.getEditor(), myFixture.getFile());
    String text = myFixture.getEditor().getDocument().getText();
    assertTrue("Should complete contract with braces",
        text.contains("export contract Token {\n  \n}"));
    int caretOffset = myFixture.getEditor().getCaretModel().getOffset();
    int expectedCaret = text.indexOf("Token {\n  ") + "Token {\n  ".length();
    assertEquals("Caret must be placed inside the contract body", expectedCaret, caretOffset);
  }

  public void testBareContractNoSemicolon() {
    myFixture.configureByText("test.compact", "contract<caret>");
    new CompactSmartEnterProcessor().process(getProject(), myFixture.getEditor(), myFixture.getFile());
    String text = myFixture.getEditor().getDocument().getText();
    assertFalse("Bare contract keyword must never append a semicolon", text.contains("contract;"));
    assertEquals("contract ", text);
    assertEquals("Caret should be positioned after space waiting for name", 9, myFixture.getEditor().getCaretModel().getOffset());
  }

  public void testConstructorDeclarationCompletion() {
    myFixture.configureByText("test.compact", "constructor(owner: Address)<caret>");
    new CompactSmartEnterProcessor().process(getProject(), myFixture.getEditor(), myFixture.getFile());
    String text = myFixture.getEditor().getDocument().getText();
    assertTrue("Should complete constructor with braces",
        text.contains("constructor(owner: Address) {\n  \n}"));
    int caretOffset = myFixture.getEditor().getCaretModel().getOffset();
    int expectedCaret = text.indexOf("constructor(owner: Address) {\n  ") + "constructor(owner: Address) {\n  ".length();
    assertEquals("Caret must be placed inside the constructor body", expectedCaret, caretOffset);
  }

  public void testStructDeclarationCompletion() {
    myFixture.configureByText("test.compact", "struct Point<caret>");
    new CompactSmartEnterProcessor().process(getProject(), myFixture.getEditor(), myFixture.getFile());
    String text = myFixture.getEditor().getDocument().getText();
    assertTrue("Should complete struct with braces",
        text.contains("struct Point {\n  \n}"));
    int caretOffset = myFixture.getEditor().getCaretModel().getOffset();
    int expectedCaret = text.indexOf("Point {\n  ") + "Point {\n  ".length();
    assertEquals("Caret must be placed inside the struct body", expectedCaret, caretOffset);
  }

  public void testIfStatementCompletionUnclosedParen() {
    myFixture.configureByText("test.compact",
        "circuit test(): Void {\n" +
        "  if (x == y<caret>\n" +
        "}"
    );
    new CompactSmartEnterProcessor().process(getProject(), myFixture.getEditor(), myFixture.getFile());
    String text = myFixture.getEditor().getDocument().getText();
    assertTrue("Should complete closing paren and braces",
        text.contains("if (x == y) {\n    \n  }"));
    int caretOffset = myFixture.getEditor().getCaretModel().getOffset();
    int expectedCaret = text.indexOf("if (x == y) {\n    ") + "if (x == y) {\n    ".length();
    assertEquals("Caret must be placed inside the if body", expectedCaret, caretOffset);
  }

  public void testConstStatementWithoutInitializerAppendsAssignment() {
    myFixture.configureByText("test.compact",
        "circuit test(): Void {\n" +
        "  const x<caret>\n" +
        "}"
    );
    new CompactSmartEnterProcessor().process(getProject(), myFixture.getEditor(), myFixture.getFile());
    String text = myFixture.getEditor().getDocument().getText();
    assertFalse("Const without initializer must never append semicolon", text.contains("const x;"));
    assertTrue("Const without initializer should insert assignment operator ' = '", text.contains("const x = "));
  }

  public void testConstStatementWithExplicitTypeWithoutInitializerAppendsAssignment() {
    myFixture.configureByText("test.compact",
        "circuit test(): Void {\n" +
        "  const x: Field<caret>\n" +
        "}"
    );
    new CompactSmartEnterProcessor().process(getProject(), myFixture.getEditor(), myFixture.getFile());
    String text = myFixture.getEditor().getDocument().getText();
    assertFalse("Const without initializer must never append semicolon", text.contains("const x: Field;"));
    assertTrue("Const without initializer should insert assignment operator ' = '", text.contains("const x: Field = "));
  }

  public void testConstStatementSemicolonCompletion() {
    myFixture.configureByText("test.compact",
        "circuit test(): Void {\n" +
        "  const x = 10<caret>\n" +
        "}"
    );
    new CompactSmartEnterProcessor().process(getProject(), myFixture.getEditor(), myFixture.getFile());
    assertTrue("Should append semicolon to const statement with value",
        myFixture.getEditor().getDocument().getText().contains("const x = 10;"));
  }

  public void testWitnessStatementSemicolonCompletion() {
    myFixture.configureByText("test.compact", "witness getSecret(): Bytes<32><caret>");
    new CompactSmartEnterProcessor().process(getProject(), myFixture.getEditor(), myFixture.getFile());
    assertTrue("Should append semicolon to witness declaration",
        myFixture.getEditor().getDocument().getText().contains("witness getSecret(): Bytes<32>;"));
  }
}
