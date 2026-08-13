package dev.verloren.midnight.refactoring;


import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactLanguage;
import dev.verloren.midnight.parser.CompactParserDefinition;
import dev.verloren.midnight.CompactFileType;


public class CompactRenameTest extends BasePlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    com.intellij.lang.LanguageParserDefinitions.INSTANCE.addExplicitExtension(
      CompactLanguage.INSTANCE,
      new CompactParserDefinition()
    );
    com.intellij.lang.findUsages.LanguageFindUsages.INSTANCE.addExplicitExtension(
      CompactLanguage.INSTANCE,
      new dev.verloren.midnight.findUsages.CompactFindUsagesProvider()
    );
  }

  public void testRenameCircuitDeclaration() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit m<caret>int() {}
                    circuit caller() {
                      mint();
                    }
                    """
    );
    myFixture.renameElementAtCaret("mintCoins");
    myFixture.checkResult(
            """
                    circuit mintCoins() {}
                    circuit caller() {
                      mintCoins();
                    }
                    """
    );
  }

  public void testRenameParameter() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit mint(a<caret>mount: Uint<32>) {
                      const total = amount;
                    }
                    """
    );
    myFixture.renameElementAtCaret("quantity");
    myFixture.checkResult(
            """
                    circuit mint(quantity: Uint<32>) {
                      const total = quantity;
                    }
                    """
    );
  }

  public void testRenameLocalConst() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit test() {
                      const o<caret>ldVar = 10;
                      const copy = oldVar;
                    }
                    """
    );
    myFixture.renameElementAtCaret("newVar");
    myFixture.checkResult(
            """
                    circuit test() {
                      const newVar = 10;
                      const copy = newVar;
                    }
                    """
    );
  }

  public void testRenameStruct() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    struct P<caret>int { x: Field, y: Field }
                    circuit draw(p: Point) {
                      const p2 = Point { x: 0, y: 0 };
                    }
                    """
    );
    myFixture.renameElementAtCaret("Vector2D");
    myFixture.checkResult(
            """
                    struct Vector2D { x: Field, y: Field }
                    circuit draw(p: Vector2D) {
                      const p2 = Vector2D { x: 0, y: 0 };
                    }
                    """
    );
  }

  public void testRenameTypeAlias() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    type A<caret>mount = Uint<32>;
                    circuit transfer(a: Amount) {}
                    """
    );
    myFixture.renameElementAtCaret("Balance");
    myFixture.checkResult(
            """
                    type Balance = Uint<32>;
                    circuit transfer(a: Balance) {}
                    """
    );
  }

  public void testRenameImportAlias() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    module Utils {
                      export const MAX = 100;
                    }
                    import { MAX as L<caret>IMIT } from Utils;
                    circuit check() {
                      const cap = LIMIT;
                    }
                    """
    );
    myFixture.renameElementAtCaret("BOUND");
    myFixture.checkResult(
            """
                    module Utils {
                      export const MAX = 100;
                    }
                    import { MAX as BOUND } from Utils;
                    circuit check() {
                      const cap = BOUND;
                    }
                    """
    );
  }

  public void testNamesValidatorKeywordsAndIdentifiers() {
    CompactNamesValidator validator = new CompactNamesValidator();

    assertTrue("circuit should be keyword", validator.isKeyword("circuit", getProject()));
    assertTrue("const should be keyword", validator.isKeyword("const", getProject()));
    assertTrue("contract should be keyword", validator.isKeyword("contract", getProject()));
    assertTrue("if should be keyword", validator.isKeyword("if", getProject()));
    assertFalse("foo should not be keyword", validator.isKeyword("foo", getProject()));

    assertTrue("foo should be valid identifier", validator.isIdentifier("foo", getProject()));
    assertTrue("_myVar123 should be valid identifier", validator.isIdentifier("_myVar123", getProject()));
    assertFalse("123bad should not be valid identifier", validator.isIdentifier("123bad", getProject()));
    assertFalse("circuit should not be identifier", validator.isIdentifier("circuit", getProject()));
  }
}
