package dev.verloren.midnight.symbol;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.verloren.midnight.CompactFileType;
import dev.verloren.midnight.psi.CompactReferenceExprImpl;
import dev.verloren.midnight.psi.CompactStructDefinition;
import dev.verloren.midnight.resolve.CompactResolveUtil;
import dev.verloren.midnight.scope.CompactScope;
import dev.verloren.midnight.scope.CompactScopeKind;

import java.util.Collection;
import java.util.List;

public class CompactSymbolTest extends BasePlatformTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    com.intellij.lang.LanguageParserDefinitions.INSTANCE.addExplicitExtension(
            dev.verloren.midnight.CompactLanguage.INSTANCE,
            new dev.verloren.midnight.parser.CompactParserDefinition()
    );
  }

  public void testTypeDeclarationSymbolClassificationAndTypeDelegation() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    export struct Point { x: Field, y: Boolean }
                    circuit draw(p: Point) {}
                    """
    );

    CompactStructDefinition struct = PsiTreeUtil.findChildOfType(myFixture.getFile(), CompactStructDefinition.class);
    assertNotNull(struct);
    CompactSymbol symbol = CompactSymbols.from(struct);

    assertNotNull(symbol);
    assertInstanceOf(symbol, CompactTypeSymbol.class);
    assertEquals(CompactSymbolKind.STRUCT, symbol.getKind());
    assertEquals(CompactSymbolNamespace.TYPE, symbol.getNamespace());
    assertEquals(CompactVisibility.EXPORTED, symbol.getVisibility());
    assertEquals("Point", symbol.getType().name());
    assertTrue(symbol.canBeReferenced());
    assertTrue(symbol.canBeRenamed());
  }

  public void testScopeExposesVisibleValueTypeAndModuleSymbols() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    module Math {
                      export circuit inc(x: Field): Field { return x + 1; }
                    }
                    struct Amount { value: Field }
                    circuit main(input: Amount) {
                      const local = <caret>input;
                    }
                    """
    );

    PsiElement place = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertNotNull(place);
    CompactScope scope = CompactResolveUtil.scopeFor(place);

    assertNotNull(scope);
    assertEquals(CompactScopeKind.BLOCK, scope.getKind());
    assertContainsSymbol(scope.getSymbols(CompactSymbolNamespace.VALUE), "input", CompactSymbolKind.PARAMETER);
    assertContainsSymbol(scope.getSymbols(CompactSymbolNamespace.TYPE), "Amount", CompactSymbolKind.STRUCT);
    assertContainsSymbol(scope.getSymbols(CompactSymbolNamespace.TYPE), "Field", CompactSymbolKind.BUILTIN_TYPE);
    assertContainsSymbol(scope.getSymbols(CompactSymbolNamespace.MODULE), "Math", CompactSymbolKind.MODULE);
  }

  private static void assertContainsSymbol(Collection<CompactSymbol> symbols, String name, CompactSymbolKind kind) {
    for (CompactSymbol symbol : symbols) {
      if (name.equals(symbol.getName()) && symbol.getKind() == kind) {
        return;
      }
    }
    fail("Expected symbol " + name + " of kind " + kind);
  }

  public void testResolvedPsiCanBeProjectedToSymbol() {
    myFixture.configureByText(CompactFileType.INSTANCE,
            """
                    circuit main(amount: Field) {
                      const next = <caret>amount;
                    }
                    """
    );

    CompactReferenceExprImpl expr = PsiTreeUtil.getParentOfType(
            myFixture.getFile().findElementAt(myFixture.getCaretOffset()),
            CompactReferenceExprImpl.class
    );
    assertNotNull(expr);

    List<CompactSymbol> symbols = CompactResolveUtil.resolveValueSymbols(expr.getText(), expr);
    assertEquals(1, symbols.size());
    CompactSymbol symbol = symbols.getFirst();
    assertInstanceOf(symbol, CompactValueSymbol.class);
    assertEquals(CompactSymbolKind.PARAMETER, symbol.getKind());
    assertEquals("Field", symbol.getType().name());
  }
}
